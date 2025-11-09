package palecek.preetham;

import org.joml.Matrix4f;
import palecek.Main;
import palecek.core.Camera;
import palecek.core.ShaderManager;
import palecek.core.entity.IShaderModule;
import palecek.core.utils.Counter;
import palecek.core.utils.Transformation;

public class PreethamSkyboxModule implements IShaderModule {
    private final Camera camera;
    private final PreethamModel model;

    public PreethamSkyboxModule(Camera camera, PreethamModel model) {
        this.camera = camera;
        this.model = model;
    }

    @Override
    public void createUniforms(ShaderManager shaderManager) throws Exception {
        shaderManager.createUniform("projectionMatrix");
        shaderManager.createUniform("viewMatrix");
        shaderManager.createUniform("sunDir");
        shaderManager.createUniform("A");
        shaderManager.createUniform("B");
        shaderManager.createUniform("C");
        shaderManager.createUniform("D");
        shaderManager.createUniform("E");
        shaderManager.createUniform("Z");
        shaderManager.createUniform("sunColor");
        shaderManager.createUniform("sunAngularRadius");
        shaderManager.createUniform("glowRadius");
    }

    @Override
    public void setUniforms(ShaderManager shaderManager) {
        shaderManager.setUniform("projectionMatrix", Main.getWindowManager().getProjectionMatrix());
        Matrix4f view = Transformation.getViewMatrix(this.camera);
        view.m30(0.0F).m31(0.0F).m32(0.0F);
        shaderManager.setUniform("viewMatrix", view);
        shaderManager.setUniform("sunDir", model.getSunDir().getDirection());
        shaderManager.setUniform("A", model.getA());
        shaderManager.setUniform("B", model.getB());
        shaderManager.setUniform("C", model.getC());
        shaderManager.setUniform("D", model.getD());
        shaderManager.setUniform("E", model.getE());
        shaderManager.setUniform("Z", model.getZ());
        shaderManager.setUniform("sunColor", model.getSunColor());
        shaderManager.setUniform("sunAngularRadius", model.getSunAngularRadius());
        shaderManager.setUniform("glowRadius", model.getGlowRadius());
    }

    @Override
    public void process(Object o) {

    }

    @Override
    public void prepare(ShaderManager shaderManager, Counter counter) {
        shaderManager.setUniform("projectionMatrix", counter.increment());
        shaderManager.setUniform("viewMatrix", counter.increment());
    }
}
