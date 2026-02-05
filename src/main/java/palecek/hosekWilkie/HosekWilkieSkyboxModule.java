package palecek.hosekWilkie;

import org.joml.Matrix4f;
import palecek.Main;
import palecek.core.Camera;
import palecek.core.ShaderManager;
import palecek.core.entity.IShaderModule;
import palecek.core.utils.Counter;
import palecek.core.utils.RenderTarget;
import palecek.core.utils.Transformation;
import palecek.preetham.PreethamModel;

public class HosekWilkieSkyboxModule implements IShaderModule {
    private final Camera camera;
    private final HosekWilkieModel model;

    public HosekWilkieSkyboxModule(Camera camera, HosekWilkieModel model) {
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
        shaderManager.createUniform("F");
        shaderManager.createUniform("G");
        shaderManager.createUniform("H");
        shaderManager.createUniform("I");
        shaderManager.createUniform("Z");
        shaderManager.createUniform("sunColor");
        shaderManager.createUniform("sunAngularRadius");
        shaderManager.createUniform("glowRadius");
        shaderManager.createUniform("exposure");
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
        shaderManager.setUniform("F", model.getF());
        shaderManager.setUniform("G", model.getG());
        shaderManager.setUniform("H", model.getH());
        shaderManager.setUniform("I", model.getI());
        shaderManager.setUniform("Z", model.getZ());
        shaderManager.setUniform("sunColor", model.getSunColor());
        shaderManager.setUniform("sunAngularRadius", model.getSunAngularRadius());
        shaderManager.setUniform("glowRadius", model.getGlowRadius());
        shaderManager.setUniform("exposure", model.getExposure());
    }

    @Override
    public void setUniforms(ShaderManager shaderManager, RenderTarget renderTarget) {

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
