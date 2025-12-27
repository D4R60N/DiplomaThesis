package palecek.bruneton;

import palecek.core.ShaderManager;
import palecek.core.Utils;
import palecek.core.entity.IShaderModule;
import palecek.core.utils.*;

import static org.lwjgl.opengl.ARBShadingLanguageInclude.GL_SHADER_INCLUDE_ARB;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.glNamedStringARB;

public class BrunetonPostprocessModule implements IShaderModule {
    private final BrunetonModel model;
    private Texture transmittanceTexture, irradianceTexture;
    private Texture3D scatteringTexture, singleMieScatteringTexture;

    public BrunetonPostprocessModule(BrunetonModel model, ITexture[] textures) {
        this.model = model;
        this.transmittanceTexture = (Texture) textures[0];
        this.irradianceTexture = (Texture) textures[1];
        this.scatteringTexture = (Texture3D) textures[2];
        this.singleMieScatteringTexture = (Texture3D) textures[3];
    }

    @Override
    public void createUniforms(ShaderManager shaderManager) throws Exception {
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
        model.createUniforms(shaderManager, "uAtmosphere");
        shaderManager.createUniform("transmittanceTexture");
        shaderManager.createUniform("irradianceTexture");
        shaderManager.createUniform("scatteringTexture");
        shaderManager.createUniform("singleMieScatteringTexture");
    }

    @Override
    public void setUniforms(ShaderManager shaderManager) {

    }

    @Override
    public void setUniforms(ShaderManager shaderManager, RenderTarget renderTarget) {
        model.setUniforms(shaderManager, "uAtmosphere");
        transmittanceTexture.bind(shaderManager.getShaderProgram(), "transmittanceTexture", 2);
        irradianceTexture.bind(shaderManager.getShaderProgram(), "irradianceTexture", 3);
        scatteringTexture.bind(shaderManager.getShaderProgram(), "scatteringTexture", 4);
        singleMieScatteringTexture.bind(shaderManager.getShaderProgram(), "singleMieScatteringTexture", 5);
    }

    @Override
    public void process(Object o) {

    }

    @Override
    public void prepare(ShaderManager shaderManager, Counter counter) {
        shaderManager.setUniform("uAtmosphere", counter.increment());
    }
}
