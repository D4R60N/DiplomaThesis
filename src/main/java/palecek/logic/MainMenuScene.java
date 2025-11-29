package palecek.logic;

import palecek.Main;
import palecek.core.*;
import palecek.core.entity.SceneManager;
import palecek.core.gui.ImGuiLayer;
import palecek.core.rendering.RenderManager;
import palecek.core.utils.RenderMode;
import palecek.gui.MainMenu;
import palecek.gui.PauseMenu;


public class MainMenuScene implements ILogic {
    private WindowManager windowManager;
    private LogicManager logicManager;
    private ImGuiLayer imGuiLayer;
    private RenderManager renderManager;
    private SceneManager sceneManager;
    private Camera camera;

    @Override
    public void init(MouseInput mouseInput) throws Exception {
        renderManager = new RenderManager();
        camera = new Camera();
        sceneManager = new SceneManager(-90, camera);
        windowManager = Main.getWindowManager();
        logicManager = Main.getLogicManager();
        imGuiLayer = new MainMenu(windowManager.getWindow(), logicManager, mouseInput);
        renderManager.init(null, camera);
        mouseInput.redirectMouseToGui(imGuiLayer.getImGuiGlfw());
    }

    @Override
    public void input(MouseInput mouseInput) {

    }

    @Override
    public void update(float v, MouseInput mouseInput) {

    }

    @Override
    public void render() {
        renderManager.render(camera, sceneManager, RenderMode.GL_TRIANGLES.getMode());
        imGuiLayer.render();
    }

    @Override
    public void cleanup() {
        imGuiLayer.dispose();
    }
}
