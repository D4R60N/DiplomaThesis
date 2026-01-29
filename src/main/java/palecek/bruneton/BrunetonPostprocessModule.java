package palecek.bruneton;

import org.joml.Vector2i;
import org.joml.Vector4i;
import palecek.core.ShaderManager;
import palecek.core.entity.IShaderModule;
import palecek.core.utils.*;

public class BrunetonPostprocessModule implements IShaderModule {
    private final BrunetonModel model;
    private Texture transmittanceTexture, irradianceTexture;
    private Texture3D scatteringTexture, singleMieScatteringTexture;
    private final Vector4i scatteringSize;
    private final Vector2i transmittanceSize, irradianceSize;

    public BrunetonPostprocessModule(BrunetonModel model, ITexture[] textures, Vector4i scatteringSize, Vector2i transmittanceSize, Vector2i irradianceSize) {
        this.model = model;
        this.transmittanceTexture = (Texture) textures[0];
        this.irradianceTexture = (Texture) textures[1];
        this.scatteringTexture = (Texture3D) textures[2];
        this.singleMieScatteringTexture = (Texture3D) textures[3];
        this.scatteringSize = scatteringSize;
        this.transmittanceSize = transmittanceSize;
        this.irradianceSize = irradianceSize;
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
        shaderManager.setUniform("exposure", model.getExposure());
        shaderManager.setUniform("white_point", model.getWhitePoint());
        shaderManager.setUniform("earth_center", model.getEarthCenter());
        shaderManager.setUniform("sun_direction", model.getSunDirection());
        shaderManager.setUniform("sun_size", model.getSunSize());
        transmittanceTexture.bind(shaderManager.getShaderProgram(), "transmittanceTexture", 2);
        irradianceTexture.bind(shaderManager.getShaderProgram(), "irradianceTexture", 3);
        scatteringTexture.bind(shaderManager.getShaderProgram(), "scatteringTexture", 4);
        singleMieScatteringTexture.bind(shaderManager.getShaderProgram(), "singleMieScatteringTexture", 5);

        shaderManager.setUniform("uScatteringTextureSize", scatteringSize);
        shaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        shaderManager.setUniform("uIrradianceTextureSize", irradianceSize);

    }

    @Override
    public void process(Object o) {

    }

    @Override
    public void prepare(ShaderManager shaderManager, Counter counter) {
        shaderManager.setUniform("uAtmosphere", counter.increment());
    }
}
