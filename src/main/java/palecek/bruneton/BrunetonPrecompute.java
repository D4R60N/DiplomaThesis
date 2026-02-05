package palecek.bruneton;

import org.joml.Vector2i;
import org.joml.Vector4i;
import palecek.core.ComputeShaderManager;
import palecek.core.Utils;
import palecek.core.utils.ITexture;
import palecek.core.utils.Texture;
import palecek.core.utils.Texture3D;
import palecek.core.utils.TextureExporter;
import palecek.utils.RawTextureExporter;

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

    private Texture3D scatteringMap, accumulatedScatteringMap, singleMieScatteringMap, scatteringDensityMap, scatteringDensityMapDisplay;
    private Texture transmittanceMap, irradianceMap, accumulatedIrradianceMap;
    private final Vector2i transmittanceSize, irradianceSize;
    private final Vector4i scatteringSize;
    private final int width;

    public BrunetonPrecompute(Vector2i transmittanceSize, Vector2i irradianceSize, Vector4i scatteringSize) {
        transmittanceMap = new Texture(transmittanceSize.x, transmittanceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        int width = scatteringSize.x * scatteringSize.w;
        scatteringMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        accumulatedScatteringMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        singleMieScatteringMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        irradianceMap = new Texture(irradianceSize.x, irradianceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        accumulatedIrradianceMap = new Texture(irradianceSize.x, irradianceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        scatteringDensityMap = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        scatteringDensityMapDisplay = new Texture3D(width, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, null);
        this.transmittanceSize = transmittanceSize;
        this.irradianceSize = irradianceSize;
        this.scatteringSize = scatteringSize;
        this.width = width;
    }

    public ITexture[] precompute(ComputeShaderManager computeShaderManager, BrunetonModel model) throws Exception {


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
                "/scattering_density/functions.glsl",
                Utils.loadResource("/shaders/precompute/bruneton/scattering_density/functions.glsl")
        );
        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                "/indirect_irradiance/functions.glsl",
                Utils.loadResource("/shaders/precompute/bruneton/indirect_irradiance/functions.glsl")
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
        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                "/functions.glsl",
                Utils.loadResource("/shaders/postProcess/bruneton/functions.glsl")
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

        //----------------------- Save Transmittance -----------------------//
        RawTextureExporter.saveTexture2DFloat(
                transmittanceMap,
                transmittanceSize.x,
                transmittanceSize.y,
                "images/bruneton/transmittance.dat"
        );

        TextureExporter.saveHDRTextureToPNG(
                transmittanceMap, transmittanceSize.x, transmittanceSize.y, "images/bruneton/transmittance.png", 1f
        );


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

        transmittanceMap.bind(
                computeShaderManager.getShaderProgram(),
                "transmittanceSampler",
                0
        );
        glBindImageTexture(1, accumulatedIrradianceMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);

        int irradianceX = (int) Math.ceil((double) irradianceSize.x / 8);
        int irradianceY = (int) Math.ceil((double) irradianceSize.y / 8);

        computeShaderManager.dispatchCompute(irradianceX, irradianceY, 1);

        computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
        computeShaderManager.unbind();

        //----------------------- Save Irradiance -----------------------//
        RawTextureExporter.saveTexture2DFloat(
                accumulatedIrradianceMap,
                irradianceSize.x,
                irradianceSize.y,
                "images/bruneton/irradiance.dat"
        );

        TextureExporter.saveHDRTextureToPNG(
                accumulatedIrradianceMap, irradianceSize.x, irradianceSize.y, "images/bruneton/irradiance.png", 1f
        );

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

        transmittanceMap.bind(
                computeShaderManager.getShaderProgram(),
                "transmittanceSampler",
                0
        );
        glBindImageTexture(1, accumulatedScatteringMap.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);
        glBindImageTexture(2, singleMieScatteringMap.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);

        groupsX = (int) Math.ceil((double) width / 8);
        groupsY = (int) Math.ceil((double) scatteringSize.y / 8);
        int groupsZ = (int) Math.ceil((double) scatteringSize.z / 8);

        computeShaderManager.dispatchCompute(groupsX, groupsY, groupsZ);

        computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
        computeShaderManager.unbind();

        //----------------------- Save Single Scattering -----------------------//
        RawTextureExporter.saveTexture3DFloat(
                accumulatedScatteringMap,
                width,
                scatteringSize.y,
                scatteringSize.z,
                "images/bruneton/single_scattering.dat"
        );
        RawTextureExporter.saveTexture3DFloat(
                singleMieScatteringMap,
                width,
                scatteringSize.y,
                scatteringSize.z,
                "images/bruneton/single_mie_scattering.dat"
        );

        TextureExporter.saveHDRTexture3DToPNG(
                accumulatedScatteringMap, width, scatteringSize.y, scatteringSize.z, "images/bruneton/single_scattering", 1f, TextureExporter.SliceDimension.Z, 1
        );
        TextureExporter.saveHDRTexture3DToPNG(
                singleMieScatteringMap, width, scatteringSize.y, scatteringSize.z, "images/bruneton/single_mie_scattering", 1f, TextureExporter.SliceDimension.Z, 1
        );

        for (int order = 2; order <= 4; order++) {
            //----------------------- Scattering Density -----------------------//

            computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/bruneton/scattering_density/scattering_density.glsl"));
            computeShaderManager.link();
            computeShaderManager.bind();

            computeShaderManager.createUniform("uScatteringTextureSize");
            computeShaderManager.createUniform("uTransmittanceTextureSize");
            computeShaderManager.createUniform("uIrradianceTextureSize");
            computeShaderManager.createUniform("order");
            model.createUniforms(computeShaderManager, "uAtmosphere");

            computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
            computeShaderManager.setUniform("uIrradianceTextureSize", irradianceSize);
            computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
            computeShaderManager.setUniform("order", order - 1);
            model.setUniforms(computeShaderManager, "uAtmosphere");

            transmittanceMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "transmittanceSampler",
                    0
            );
            accumulatedScatteringMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "singleRayleighScatteringSampler",
                    1
            );
            singleMieScatteringMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "singleMieScatteringSampler",
                    2
            );
            scatteringMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "multipleScatteringSampler",
                    3
            );
            if (order == 2) {
                accumulatedIrradianceMap.bind(
                        computeShaderManager.getShaderProgram(),
                        "irradianceSampler",
                        4
                );
            } else {
                irradianceMap.bind(
                        computeShaderManager.getShaderProgram(),
                        "irradianceSampler",
                        4
                );
            }
            glBindImageTexture(5, scatteringDensityMap.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);
            glBindImageTexture(6, scatteringDensityMapDisplay.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);

            computeShaderManager.dispatchCompute(groupsX, groupsY, groupsZ);

            computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
            computeShaderManager.unbind();

            //----------------------- Save Scattering Density -----------------------//
            RawTextureExporter.saveTexture3DFloat(
                    scatteringDensityMap,
                    width,
                    scatteringSize.y,
                    scatteringSize.z,
                    "images/bruneton/scattering_density_order_" + order + ".dat"
            );

            TextureExporter.saveHDRTexture3DToPNG(
                    scatteringDensityMapDisplay, width, scatteringSize.y, scatteringSize.z, "images/bruneton/scattering_density_order_" + order, 1f, TextureExporter.SliceDimension.Z, 1
            );

            //----------------------- Indirect Irradiance -----------------------//

            computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/bruneton/indirect_irradiance/indirect_irradiance.glsl"));
            computeShaderManager.link();
            computeShaderManager.bind();

            computeShaderManager.createUniform("uScatteringTextureSize");
            computeShaderManager.createUniform("uIrradianceTextureSize");
            computeShaderManager.createUniform("uTransmittanceTextureSize");
            computeShaderManager.createUniform("order");
            model.createUniforms(computeShaderManager, "uAtmosphere");

            computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
            computeShaderManager.setUniform("uIrradianceTextureSize", irradianceSize);
            computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
            computeShaderManager.setUniform("order", order - 1);
            model.setUniforms(computeShaderManager, "uAtmosphere");


            scatteringMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "singleRayleighScatteringSampler",
                    0
            );
            singleMieScatteringMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "singleMieScatteringSampler",
                    1
            );
            scatteringMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "multipleScatteringSampler",
                    2
            );
            glBindImageTexture(3, irradianceMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
            transmittanceMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "transmittanceSampler",
                    4
            );
            glBindImageTexture(5, accumulatedIrradianceMap.getId(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);

            computeShaderManager.dispatchCompute(irradianceX, irradianceY, 1);

            computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
            computeShaderManager.unbind();

            //----------------------- Save Indirect Irradiance -----------------------//
            RawTextureExporter.saveTexture2DFloat(
                    accumulatedIrradianceMap,
                    irradianceSize.x,
                    irradianceSize.y,
                    "images/bruneton/indirect_irradiance_order_" + order + ".dat"
            );

            TextureExporter.saveHDRTextureToPNG(
                    accumulatedIrradianceMap, irradianceSize.x, irradianceSize.y, "images/bruneton/acc_indirect_irradiance_order_" + order + ".png", 1f
            );

            TextureExporter.saveHDRTextureToPNG(
                    irradianceMap, irradianceSize.x, irradianceSize.y, "images/bruneton/indirect_irradiance_order_" + order + ".png", 1f
            );

            //----------------------- Multiple Scattering -----------------------//

            computeShaderManager.createComputeShader(Utils.loadResource("/shaders/precompute/bruneton/multiple_scattering/multiple_scattering.glsl"));
            computeShaderManager.link();
            computeShaderManager.bind();

            computeShaderManager.createUniform("uScatteringTextureSize");
            computeShaderManager.createUniform("uTransmittanceTextureSize");
            model.createUniforms(computeShaderManager, "uAtmosphere");

            computeShaderManager.setUniform("uScatteringTextureSize", scatteringSize);
            computeShaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
            model.setUniforms(computeShaderManager, "uAtmosphere");

            transmittanceMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "transmittanceSampler",
                    0
            );
            scatteringDensityMap.bind(
                    computeShaderManager.getShaderProgram(),
                    "scatteringDensitySampler",
                    1
            );
            glBindImageTexture(2, scatteringMap.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);
            glBindImageTexture(3, accumulatedScatteringMap.getId(), 0, true, 0, GL_WRITE_ONLY, GL_RGBA32F);

            computeShaderManager.dispatchCompute(groupsX, groupsY, groupsZ);

            computeShaderManager.memoryBarrier(GL_ALL_BARRIER_BITS);
            computeShaderManager.unbind();

            //----------------------- Save Multiple Scattering -----------------------//
            RawTextureExporter.saveTexture3DFloat(
                    accumulatedScatteringMap,
                    width,
                    scatteringSize.y,
                    scatteringSize.z,
                    "images/bruneton/multiple_scattering_order_" + order + ".dat"
            );

            TextureExporter.saveHDRTexture3DToPNG(
                    scatteringMap, width, scatteringSize.y, scatteringSize.z, "images/bruneton/multiple_scattering_order_" + order, 1f, TextureExporter.SliceDimension.Z, 1
            );
            TextureExporter.saveHDRTexture3DToPNG(
                    accumulatedScatteringMap, width, scatteringSize.y, scatteringSize.z, "images/bruneton/acc_multiple_scattering_order_" + order, 1f, TextureExporter.SliceDimension.Z, 1
            );
        }

        return new ITexture[]{transmittanceMap, accumulatedIrradianceMap, accumulatedScatteringMap, singleMieScatteringMap};
    }
}
