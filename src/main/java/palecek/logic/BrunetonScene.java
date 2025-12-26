package palecek.logic;

import org.joml.*;
import palecek.Main;
import palecek.bruneton.BrunetonModel;
import palecek.core.*;
import palecek.core.entity.SceneManager;
import palecek.core.gui.ImGuiLayer;
import palecek.core.lighting.DirectionalLight;
import palecek.core.planet.Planet;
import palecek.core.planet.PlanetGenerator;
import palecek.core.rendering.PlanetRenderer;
import palecek.core.rendering.RenderManager;
import palecek.core.rendering.SkyboxRenderer;
import palecek.core.skybox.Skybox;
import palecek.core.skybox.SkyboxTexture;
import palecek.core.utils.Constants;
import palecek.core.utils.RenderMode;
import palecek.core.utils.glfw.GLFWEnum;
import palecek.gui.PauseMenu;

import palecek.bruneton.BrunetonPrecompute;

import java.util.Arrays;
import java.util.List;

public class BrunetonScene implements ILogic {
    private final RenderManager renderManager;
    private final ObjectLoader objectLoader;
    private WindowManager windowManager;
    private final SceneManager sceneManager;
    private PlanetGenerator planetGenerator;
    private PlanetRenderer planetRenderer;
    private SkyboxRenderer skyboxRenderer;
    private BrunetonModel brunetonModel;
    private ImGuiLayer imGuiLayer;
    private boolean showGui;
    private LogicManager logicManager;

    private float speed = 0.0f;

    private Camera camera;
    Vector3f cameraInc;

    public BrunetonScene() {
        renderManager = new RenderManager();
        objectLoader = new ObjectLoader();
        camera = new Camera();
        cameraInc = new Vector3f(0, 0, 0);
        sceneManager = new SceneManager(-90, camera);
    }

    @Override
    public void init(MouseInput mouseInput) throws Exception {
        windowManager = Main.getWindowManager();
        logicManager = Main.getLogicManager();
        showGui = false;
        imGuiLayer = new PauseMenu(windowManager.getWindow(), logicManager, () -> showGui = false);

        // Planet
        planetRenderer = new PlanetRenderer();
        int[] lods = {8, 16};
        short[] lodDistances = {8, 12};
        planetGenerator = new PlanetGenerator(objectLoader, new ComputeShaderManager());

//         Skybox
        List<String> faces = Arrays.asList(
                "textures/skybox/px.png",   // +X
                "textures/skybox/nx.png",    // -X
                "textures/skybox/py.png",     // +Y
                "textures/skybox/ny.png",  // -Y
                "textures/skybox/pz.png",   // +Z
                "textures/skybox/nz.png"     // -Z
        );
        Skybox skybox = new Skybox(new SkyboxTexture(faces), objectLoader, 8, 16);

        int T = 2;
        int A = 0;

        BrunetonModel bruneton = new BrunetonModel();
        brunetonModel = bruneton;
//        skyboxRenderer = new SkyboxRenderer(skybox, "nishita", List.of(new NishitaSkyboxModule(camera, nishita)));
//
//         renderManager init
        renderManager.init(null, camera, planetRenderer);

        // Light
        float lightIntensity = 1.0f;
        Vector3f lightPosition = new Vector3f(1f, 0f, 1f);
        Vector3f lightColor = new Vector3f(1f, 1f, 1f);
        sceneManager.setDirectionalLight(new DirectionalLight(lightColor, lightPosition, lightIntensity));

        BrunetonPrecompute brunetonPrecompute = new BrunetonPrecompute();
        System.out.println("BrunetonPrecompute: " + brunetonPrecompute);
        brunetonPrecompute.precompute(new ComputeShaderManager(), new Vector2i(256, 64), new Vector2i(64, 16), new Vector4i(32, 128, 32, 8), brunetonModel);
    }

    @Override
    public void input(MouseInput mouseInput) {
        cameraInc.set(0, 0, 0);
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_W.val)) {
            cameraInc.z = -speed;
        } else if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_S.val)) {
            cameraInc.z = speed;
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_A.val)) {
            cameraInc.x = -speed;
        } else if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_D.val)) {
            cameraInc.x = speed;
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_Q.val)) {
            cameraInc.y = -speed;
        } else if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_E.val)) {
            cameraInc.y = speed;
        }
//        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_R.val)) {
//            nishitaModel.rotateSun(-1);
//        }
//        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_F.val)) {
//            nishitaModel.rotateSun(1);
//        }
        if (windowManager.isKeyTyped(GLFWEnum.GLFW_KEY_TAB.val)) {
            if (!showGui) {
                mouseInput.redirectMouseToGui(imGuiLayer.getImGuiGlfw());
            }
            else {
                mouseInput.restoreMouseForApp();
            }
            showGui = !showGui;
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_LEFT_SHIFT.val)) {
            speed = 0.1f;
        } else {
            speed = 0.05f;
        }
    }

    @Override
    public void update(float interval, MouseInput mouseInput) {
        camera.movePosition(cameraInc.x * Constants.CAMERA_MOVEMENT_SPEED,
                cameraInc.y * Constants.CAMERA_MOVEMENT_SPEED,
                cameraInc.z * Constants.CAMERA_MOVEMENT_SPEED);


        if (mouseInput.isRightButtonPressed()) {
            Vector2f rotVec = mouseInput.getDisplVec();
            camera.moveRotation(rotVec.x * Constants.MOUSE_SENSITIVITY, rotVec.y * Constants.MOUSE_SENSITIVITY, 0);
        }

        for (Planet planet : sceneManager.getPlanets()) {
            planet.incRotationAngle(0.1f);
        }
    }

    @Override
    public void render() {
        renderManager.render(camera, sceneManager, RenderMode.GL_TRIANGLES.getMode());
        if (showGui)
            imGuiLayer.render();
    }

    @Override
    public void cleanup() {
        imGuiLayer.dispose();
        renderManager.cleanup();
        objectLoader.cleanUp();
    }



}