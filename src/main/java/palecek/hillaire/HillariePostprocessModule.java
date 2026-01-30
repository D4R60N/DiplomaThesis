package palecek.hillaire;

import org.joml.*;
import palecek.Main;
import palecek.bruneton.BrunetonModel;
import palecek.core.ShaderManager;
import palecek.core.entity.IShaderModule;
import palecek.core.utils.*;

public class HillariePostprocessModule implements IShaderModule {
    private final HillarieModel model;
    private Texture transmittanceTexture, multiscatteringTexture, skyViewTexture;
    private Texture3D aerialPerspectiveTexture;
    private final Vector2i transmittanceSize, multiScatteringSize, skyViewSize;
    private final Vector3i arialPerspectiveSize;

    public HillariePostprocessModule(HillarieModel model, ITexture[] textures, Vector2i multiScatteringSize, Vector2i transmittanceSize, Vector2i skyViewSize, Vector3i arialPerspectiveSize) {
        this.model = model;
        this.transmittanceTexture = (Texture) textures[0];
        this.multiscatteringTexture = (Texture) textures[1];
        this.skyViewTexture = (Texture) textures[2];
        this.aerialPerspectiveTexture = (Texture3D) textures[3];
        this.multiScatteringSize = multiScatteringSize;
        this.transmittanceSize = transmittanceSize;
        this.skyViewSize = skyViewSize;
        this.arialPerspectiveSize = arialPerspectiveSize;
    }

    @Override
    public void createUniforms(ShaderManager shaderManager) throws Exception {
        model.createUniforms(shaderManager, "uAtmosphere");
        shaderManager.createUniform("exposure");
        shaderManager.createUniform("sunDirection");
        shaderManager.createUniform("sunIlluminance");

        shaderManager.createUniform("transmittanceTexture");
        shaderManager.createUniform("irradianceTexture");
        shaderManager.createUniform("scatteringTexture");
        shaderManager.createUniform("singleMieScatteringTexture");

        shaderManager.createUniform("uScatteringTextureSize");
        shaderManager.createUniform("uTransmittanceTextureSize");
        shaderManager.createUniform("uSkyViewTextureSize");
        shaderManager.createUniform("uAerialPerspectiveTextureSize");
    }

    @Override
    public void setUniforms(ShaderManager shaderManager) {

    }

    @Override
    public void setUniforms(ShaderManager shaderManager, RenderTarget renderTarget) {
        shaderManager.setUniform("exposure", model.getExposure());
        shaderManager.setUniform("sunDirection", model.getSunDirection());
        shaderManager.setUniform("sunIlluminance", model.getSunIlluminance());
        transmittanceTexture.bind(shaderManager.getShaderProgram(), "transmittanceTexture", 2);
        multiscatteringTexture.bind(shaderManager.getShaderProgram(), "multiScatteringTexture", 2);
        skyViewTexture.bind(shaderManager.getShaderProgram(), "skyViewTexture", 3);
        aerialPerspectiveTexture.bind(shaderManager.getShaderProgram(), "aerialPerspectiveTexture", 4);

        shaderManager.setUniform("uTransmittanceTextureSize", transmittanceSize);
        shaderManager.setUniform("uScatteringTextureSize", multiScatteringSize);
        shaderManager.setUniform("uSkyViewTextureSize", skyViewSize);
        shaderManager.setUniform("uAerialPerspectiveTextureSize", arialPerspectiveSize);
        model.setUniforms(shaderManager, "uAtmosphere");

    }

    @Override
    public void process(Object o) {

    }

    @Override
    public void prepare(ShaderManager shaderManager, Counter counter) {

    }
}
