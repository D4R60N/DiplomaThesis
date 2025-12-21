package palecek.nishita;

import org.joml.Matrix4f;
import palecek.Main;
import palecek.core.Camera;
import palecek.core.ShaderManager;
import palecek.core.entity.IShaderModule;
import palecek.core.utils.Counter;
import palecek.core.utils.RenderTarget;
import palecek.core.utils.Transformation;

public class NishitaSkyboxModule implements IShaderModule {
    private final Camera camera;
    private final NishitaModel model;

    public NishitaSkyboxModule(Camera camera, NishitaModel model) {
        this.camera = camera;
        this.model = model;
    }

    @Override
    public void createUniforms(ShaderManager shaderManager) throws Exception {
        shaderManager.createUniform("projectionMatrix");
        shaderManager.createUniform("viewMatrix");
        shaderManager.createUniform("sunDir");
        shaderManager.createUniform("betaR");
        shaderManager.createUniform("betaM");
        shaderManager.createUniform("HR");
        shaderManager.createUniform("HM");
        shaderManager.createUniform("g");
    }

    @Override
    public void setUniforms(ShaderManager shaderManager) {
        shaderManager.setUniform("projectionMatrix", Main.getWindowManager().getProjectionMatrix());
        Matrix4f view = Transformation.getViewMatrix(this.camera);
        view.m30(0.0F).m31(0.0F).m32(0.0F);
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
