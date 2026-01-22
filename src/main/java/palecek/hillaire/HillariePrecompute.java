package palecek.hillaire;

import org.joml.Vector2i;
import org.joml.Vector4i;
import palecek.utils.RawTextureExporter;
import palecek.core.ComputeShaderManager;
import palecek.core.Utils;
import palecek.core.utils.ITexture;
import palecek.core.utils.Texture;
import palecek.core.utils.Texture3D;
import palecek.core.utils.TextureExporter;

import static org.lwjgl.opengl.ARBShadingLanguageInclude.GL_SHADER_INCLUDE_ARB;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.glNamedStringARB;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11C.glDeleteTextures;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;
import static org.lwjgl.opengl.GL42C.GL_ALL_BARRIER_BITS;
import static org.lwjgl.opengl.GL42C.glBindImageTexture;
import static org.lwjgl.opengles.GLES31.GL_WRITE_ONLY;


public class HillariePrecompute {

    public ITexture[] precompute(ComputeShaderManager computeShaderManager, Vector2i transmittanceSize, Vector2i irradianceSize, Vector4i scatteringSize, HillarieModel model) throws Exception {
        Texture transmittanceMap = new Texture(transmittanceSize.x, transmittanceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        int width = scatteringSize.x*scatteringSize.w;
        Texture3D scatteringMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture3D accumulatedScatteringMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture3D singleMieScatteringMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture irradianceMap = new Texture(irradianceSize.x, irradianceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture accumulatedIrradianceMap = new Texture(irradianceSize.x, irradianceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture3D scatteringDensityMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture3D scatteringDensityMapDisplay = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);

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

        glDeleteTextures(
                new int[]{
                        irradianceMap.getId(),
                        scatteringMap.getId(),
                        scatteringDensityMap.getId()
                }
        );
        return new ITexture[]{transmittanceMap, accumulatedIrradianceMap, accumulatedScatteringMap, singleMieScatteringMap};
    }
}
