package palecek.hillaire;

import org.joml.Vector2i;
import org.joml.Vector4i;
import palecek.bruneton.BrunetonModel;
import palecek.core.ShaderManager;
import palecek.core.entity.IShaderModule;
import palecek.core.utils.*;

public class HillariePostprocessModule implements IShaderModule {
    private final HillarieModel model;
    private Texture transmittanceTexture, irradianceTexture;
    private Texture3D scatteringTexture, singleMieScatteringTexture;
    private final Vector2i transmittanceSize, scatteringSize;

    public HillariePostprocessModule(HillarieModel model, ITexture[] textures, Vector2i scatteringSize, Vector2i transmittanceSize) {
        this.model = model;
        this.transmittanceTexture = (Texture) textures[0];
        this.irradianceTexture = (Texture) textures[1];
        this.scatteringTexture = (Texture3D) textures[2];
        this.singleMieScatteringTexture = (Texture3D) textures[3];
        this.scatteringSize = scatteringSize;
        this.transmittanceSize = transmittanceSize;
    }

    @Override
    public void createUniforms(ShaderManager shaderManager) throws Exception {

        model.createUniforms(shaderManager, "uAtmosphere");
        shaderManager.createUniform("transmittanceTexture");
        shaderManager.createUniform("irradianceTexture");
        shaderManager.createUniform("scatteringTexture");
        shaderManager.createUniform("singleMieScatteringTexture");

        shaderManager.createUniform("uScatteringTextureSize");
        shaderManager.createUniform("uTransmittanceTextureSize");
        shaderManager.createUniform("uIrradianceTextureSize");

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

        shaderManager.setUniform("uScatteringTextureSize", scatteringSize);
        shaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);

    }

    @Override
    public void process(Object o) {

    }

    @Override
    public void prepare(ShaderManager shaderManager, Counter counter) {
        shaderManager.setUniform("uAtmosphere", counter.increment());
    }
}
