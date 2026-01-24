package palecek.hillaire;

import org.joml.*;
import palecek.utils.RawTextureExporter;
import palecek.core.ComputeShaderManager;
import palecek.core.Utils;
import palecek.core.utils.ITexture;
import palecek.core.utils.Texture;
import palecek.core.utils.Texture3D;
import palecek.core.utils.TextureExporter;

import java.lang.Math;

import static org.lwjgl.opengl.ARBShadingLanguageInclude.GL_SHADER_INCLUDE_ARB;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.glNamedStringARB;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11C.glDeleteTextures;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;
import static org.lwjgl.opengl.GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42C.glBindImageTexture;
import static org.lwjgl.opengles.GLES31.GL_WRITE_ONLY;


public class HillariePrecompute {
    private final Texture transmittanceMap, multipleScatteringMap, skyViewMap;
    private final Vector2i transmittanceSize,scatteringSize, skyViewSize;
    private final int skyViewGroupsX, skyViewGroupsY;

    public HillariePrecompute(Vector2i transmittanceSize, Vector2i scatteringSize, Vector2i skyViewSize) {
        transmittanceMap = new Texture(transmittanceSize.x, transmittanceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        multipleScatteringMap = new Texture(scatteringSize.x, scatteringSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        skyViewMap = new Texture(skyViewSize.x, skyViewSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        this.transmittanceSize = transmittanceSize;
        this.scatteringSize = scatteringSize;
        this.skyViewSize = skyViewSize;
        this.skyViewGroupsX = (int) Math.ceil((double) skyViewSize.x / 8);
        this.skyViewGroupsY = (int) Math.ceil((double) skyViewSize.y / 8);
    }

    public ITexture[] precompute(ComputeShaderManager computeShaderManager, HillarieModel model) throws Exception {
        int groupsX = (int) Math.ceil((double) transmittanceSize.x / 8);
        int groupsY = (int) Math.ceil((double) transmittanceSize.y / 8);

        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                "/common.glsl",
                Utils.loadResource("/shaders/precompute/hillaire/common.glsl")
        );

        //----------------------- Transmittance -----------------------//

        computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/hillaire/transmittance/transmittance.glsl"));
        computeShaderManager.link();
        computeShaderManager.bind();

        computeShaderManager.createUniform("uTransmittanceTextureSize");
        model.createUniforms(computeShaderManager, "uAtmosphere");

        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        model.setUniforms(computeShaderManager, "uAtmosphere");


        glBindImageTexture(0, transmittanceMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        transmittanceMap.bind(
                computeShaderManager.getShaderProgram(),
                "u_TransmittanceLutTexture",
                0
        );
        computeShaderManager.dispatchCompute(groupsX, groupsY, 1);

        computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
        computeShaderManager.unbind();

        //----------------------- Save Transmittance -----------------------//
        RawTextureExporter.saveTexture2DFloat(
                transmittanceMap,
                transmittanceSize.x,
                transmittanceSize.y,
                "images/hillaire/transmittance.dat"
        );

        TextureExporter.saveHDRTextureToPNG(
                transmittanceMap, transmittanceSize.x, transmittanceSize.y, "images/hillaire/transmittance.png", 1f
        );

        //----------------------- Multiple Scattering -----------------------//

        computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/hillaire/multiple_scattering/multiple_scattering.glsl"));

        precomputeMultiScattering(computeShaderManager, model);

        //----------------------- Save Multiple Scattering -----------------------//
        RawTextureExporter.saveTexture2DFloat(
                multipleScatteringMap,
                scatteringSize.x,
                scatteringSize.y,
                "images/hillaire/multiple_scattering.dat"
        );

        TextureExporter.saveHDRTextureToPNG(
                multipleScatteringMap, scatteringSize.x, scatteringSize.y, "images/hillaire/multiple_scattering.png", 1f
        );

        //----------------------- Sky View -----------------------//

        computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/hillaire/sky_view/sky_view.glsl"));

        precomputeSkyView(computeShaderManager, model);

        //----------------------- Save Sky View -----------------------//
        RawTextureExporter.saveTexture2DFloat(
                skyViewMap,
                skyViewSize.x,
                skyViewSize.y,
                "images/hillaire/sky_view.dat"
        );

        TextureExporter.saveHDRTextureToPNG(
                skyViewMap, skyViewSize.x, skyViewSize.y, "images/hillaire/sky_view.png", 1f
        );

//        glDeleteTextures(
//                new int[]{
//                        irradianceMap.getId(),
//                        scatteringMap.getId(),
//                        scatteringDensityMap.getId()
//                }
//        );
        return new ITexture[]{transmittanceMap, multipleScatteringMap};
    }
    public void precomputeMultiScattering(ComputeShaderManager computeShaderManager, HillarieModel model) throws Exception {
        computeShaderManager.link();
        computeShaderManager.bind();

        computeShaderManager.createUniform("uScatteringTextureSize");
        computeShaderManager.createUniform("uTransmittanceTextureSize");
//        computeShaderManager.createUniform("gSkyInvViewProjMat");
//        computeShaderManager.createUniform("sunDirection");
        computeShaderManager.createUniform("RayMarchMinMaxSPP");
        computeShaderManager.createUniform("gSunIlluminance");
        model.createUniforms(computeShaderManager, "uAtmosphere");

        computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        computeShaderManager.setUniform("RayMarchMinMaxSPP", new Vector2f(4.0f, 14.0f));
        computeShaderManager.setUniform("gSunIlluminance", new Vector3f(10.0f, 10.0f, 10.0f));
        model.setUniforms(computeShaderManager, "uAtmosphere");

        glBindImageTexture(0, multipleScatteringMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        transmittanceMap.bind(
                computeShaderManager.getShaderProgram(),
                "u_TransmittanceLutTexture",
                1
        );

        computeShaderManager.dispatchCompute(scatteringSize.x, scatteringSize.y, 1);

        computeShaderManager.memoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        computeShaderManager.unbind();
    }

    public void precomputeSkyView(ComputeShaderManager computeShaderManager, HillarieModel model) throws Exception {
        computeShaderManager.link();
        computeShaderManager.bind();

        computeShaderManager.createUniform("uSkyViewTextureSize");
        computeShaderManager.createUniform("uScatteringTextureSize");
        computeShaderManager.createUniform("uTransmittanceTextureSize");
        computeShaderManager.createUniform("RayMarchMinMaxSPP");
        computeShaderManager.createUniform("gSunIlluminance");
        model.createUniforms(computeShaderManager, "uAtmosphere");

        computeShaderManager.setUniform("uSkyViewTextureSize", skyViewSize);
        computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        computeShaderManager.setUniform("RayMarchMinMaxSPP", new Vector2f(4.0f, 14.0f));
        computeShaderManager.setUniform("gSunIlluminance", new Vector3f(10.0f, 10.0f, 10.0f));
        model.setUniforms(computeShaderManager, "uAtmosphere");

        glBindImageTexture(0, skyViewMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        transmittanceMap.bind(
                computeShaderManager.getShaderProgram(),
                "u_TransmittanceLutTexture",
                1
        );
        transmittanceMap.bind(
                computeShaderManager.getShaderProgram(),
                "u_MultipleScatteringLutTexture",
                2
        );

        computeShaderManager.dispatchCompute(skyViewGroupsX, skyViewGroupsY, 1);

        computeShaderManager.memoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        computeShaderManager.unbind();
    }
}
