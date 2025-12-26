package palecek.bruneton;

import org.joml.Vector2i;
import org.joml.Vector4i;
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
import static org.lwjgl.opengles.GLES31.*;


public class BrunetonPrecompute {

    public ITexture[] precompute(ComputeShaderManager computeShaderManager, Vector2i transmittanceSize, Vector2i irradianceSize, Vector4i scatteringSize, BrunetonModel model) throws Exception {
        Texture transmittanceMap = new Texture(transmittanceSize.x, transmittanceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        int width = scatteringSize.x*scatteringSize.w;
        Texture3D singleRayleighScatteringMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture3D singleMieScatteringMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture3D multipleScatteringMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture irradianceMap = new Texture(irradianceSize.x, irradianceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        Texture3D scatteringDensityMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);

        int groupsX = (int) Math.ceil((double) transmittanceSize.x / 8);
        int groupsY = (int) Math.ceil((double) transmittanceSize.y / 8);

        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                "/transmittance/functions.glsl",
                Utils.loadResource("/shaders/precompute/bruneton/transmittance/functions.glsl")
        );
        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                "/single_scattering/functions.glsl",
                Utils.loadResource("/shaders/precompute/bruneton/single_scattering/functions.glsl")
        );
        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                "/direct_irradiance/functions.glsl",
                Utils.loadResource("/shaders/precompute/bruneton/direct_irradiance/functions.glsl")
        );
        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                "/multiple_scattering/functions.glsl",
                Utils.loadResource("/shaders/precompute/bruneton/multiple_scattering/functions.glsl")
        );
        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                "/definitions.glsl",
                Utils.loadResource("/shaders/precompute/bruneton/definitions.glsl")
        );

        //----------------------- Transmittance -----------------------//

        computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/bruneton/transmittance/transmittance.glsl"));
        computeShaderManager.link();
        computeShaderManager.bind();

        computeShaderManager.createUniform("uTransmittanceTextureSize");
        model.createUniforms(computeShaderManager, "uAtmosphere");

        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        model.setUniforms(computeShaderManager, "uAtmosphere");


        glBindImageTexture(0, transmittanceMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        computeShaderManager.dispatchCompute(groupsX, groupsY, 1);

        computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
        computeShaderManager.unbind();

        //----------------------- Direct Irradiance -----------------------//

        computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/bruneton/direct_irradiance/direct_irradiance.glsl"));
        computeShaderManager.link();
        computeShaderManager.bind();

        computeShaderManager.createUniform("uIrradianceTextureSize");
        computeShaderManager.createUniform("uTransmittanceTextureSize");
        model.createUniforms(computeShaderManager, "uAtmosphere");

        computeShaderManager.setUniform("uIrradianceTextureSize", irradianceSize);
        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        model.setUniforms(computeShaderManager, "uAtmosphere");

        glBindImageTexture(0, transmittanceMap.getId(), 0, false, 0, GL_READ_ONLY, GL_RGBA32F);
        glBindImageTexture(1, irradianceMap.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);

        groupsX = (int) Math.ceil((double) irradianceSize.x / 8);
        groupsY = (int) Math.ceil((double) irradianceSize.y / 8);

        computeShaderManager.dispatchCompute(groupsX, groupsY, 1);

        computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
        computeShaderManager.unbind();

        //----------------------- Single Scattering -----------------------//

        computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/bruneton/single_scattering/single_scattering.glsl"));
        computeShaderManager.link();
        computeShaderManager.bind();

        computeShaderManager.createUniform("uScatteringTextureSize");
        computeShaderManager.createUniform("uTransmittanceTextureSize");
        model.createUniforms(computeShaderManager, "uAtmosphere");

        computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        model.setUniforms(computeShaderManager, "uAtmosphere");

        glBindImageTexture(0, transmittanceMap.getId(), 0, false, 0, GL_READ_ONLY, GL_RGBA32F);
        glBindImageTexture(1, singleRayleighScatteringMap.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);
        glBindImageTexture(2, singleMieScatteringMap.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);

        groupsX = (int) Math.ceil((double) width / 8);
        groupsY = (int) Math.ceil((double) scatteringSize.y / 8);
        int groupsZ = (int) Math.ceil((double) scatteringSize.z / 8);

        computeShaderManager.dispatchCompute(groupsX, groupsY, groupsZ);

        computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
        computeShaderManager.unbind();


        //----------------------- Multiple Scattering -----------------------//

//        computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/bruneton/multiple_scattering/multiple_scattering.glsl"));
//        computeShaderManager.link();
//        computeShaderManager.bind();
//
//        computeShaderManager.createUniform("uScatteringTextureSize");
//        computeShaderManager.createUniform("uTransmittanceTextureSize");
//        model.createUniforms(computeShaderManager, "uAtmosphere");
//
//        computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
//        computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
//        model.setUniforms(computeShaderManager, "uAtmosphere");
//
//        glBindImageTexture(0, transmittanceMap.getId(), 0, false, 0, GL_READ_ONLY, GL_RGBA32F);
//        glBindImageTexture(1, singleRayleighScatteringMap.getId(), 0, true, 0, GL_READ_ONLY, GL_RGBA32F);
//        glBindImageTexture(2, singleMieScatteringMap.getId(), 0, true, 0, GL_READ_ONLY, GL_RGBA32F);
//        glBindImageTexture(3, multipleScatteringMap.getId(), 0, true, 0, GL_READ_WRITE, GL_RGBA32F);
//        glBindImageTexture(4, irradianceMap.getId(), 0, true, 0, GL_READ_ONLY, GL_RGBA32F);
//        glBindImageTexture(5, scatteringDensityMap.getId(), 0, true, 0, GL_READ_ONLY, GL_RGBA32F);
//
//        computeShaderManager.dispatchCompute(groupsX, groupsY, groupsZ);
//
//        computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
//        computeShaderManager.unbind();

        TextureExporter.saveHDRTextureToPNG(transmittanceMap, transmittanceSize.x, transmittanceSize.y, "images/bruneton/transmittance_map.png", 1f);
        TextureExporter.saveHDRTextureToPNG(irradianceMap, irradianceSize.x, irradianceSize.y, "images/bruneton/irradiance_map.png", 1f);
        TextureExporter.saveHDRTexture3DToPNG(singleRayleighScatteringMap, width, scatteringSize.y, scatteringSize.z, "images/bruneton/scattering_rayleigh/scattering_map.png", 1f);
        TextureExporter.saveHDRTexture3DToPNG(singleMieScatteringMap, width, scatteringSize.y, scatteringSize.z, "images/bruneton/scattering_mie/scattering_map.png", 1f);
        glDeleteTextures(
                new int[]{
                        singleMieScatteringMap.getId(),
                        singleRayleighScatteringMap.getId()
                }
        );
        return new ITexture[]{transmittanceMap, multipleScatteringMap};
    }
}
