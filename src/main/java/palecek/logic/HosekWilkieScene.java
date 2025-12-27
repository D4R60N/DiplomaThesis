package palecek.logic;

import org.joml.Vector2f;
import org.joml.Vector3f;
import palecek.Main;
import palecek.core.*;
import palecek.core.entity.SceneManager;
import palecek.core.gui.ImGuiLayer;
import palecek.core.lighting.DirectionalLight;
import palecek.core.rendering.RenderManager;
import palecek.core.rendering.SkyboxRenderer;
import palecek.core.rendering.TerrainRenderer;
import palecek.core.skybox.Skybox;
import palecek.core.skybox.SkyboxTexture;
import palecek.core.terrain.Terrain;
import palecek.core.terrain.TerrainGenerator;
import palecek.core.utils.Constants;
import palecek.core.utils.RenderMode;
import palecek.core.utils.glfw.GLFWEnum;
import palecek.gui.PauseMenu;
import palecek.hosekWilkie.HosekWilkieModel;
import palecek.hosekWilkie.HosekWilkieSkyboxModule;
import palecek.utils.SunVector;

import java.util.Arrays;
import java.util.List;

public class HosekWilkieScene implements ILogic {
    private final RenderManager renderManager;
    private final ObjectLoader objectLoader;
    private WindowManager windowManager;
    private final SceneManager sceneManager;
    private TerrainGenerator terrainGenerator;
    private TerrainRenderer terrainRenderer;
    private SkyboxRenderer skyboxRenderer;
    private HosekWilkieModel hosekWilkieModel;
    private ImGuiLayer imGuiLayer;
    private boolean showGui;
    private LogicManager logicManager;

    private float speed = 0.0f;

    private Camera camera;
    Vector3f cameraInc;

    public HosekWilkieScene() {
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

        // Terrain
        terrainRenderer = new TerrainRenderer();
        int[] lods = {8, 16};
        short[] lodDistances = {8, 12};
        terrainGenerator = new TerrainGenerator(objectLoader, new ComputeShaderManager(), 500, 32, 36, 128, lods, lodDistances);

//         Skybox
        Skybox skybox = new Skybox(null, objectLoader, 8, 16);

        float T = 2.50f;
        int A = 0;

        HosekWilkieModel hosekWilkie = new HosekWilkieModel(
                new SunVector((float) Math.toRadians(0), (float) Math.toRadians(90)),
                new Vector3f(1.0f, 0.95f, 0.85f),
                (float)Math.toRadians(0.66),
                (float)Math.toRadians(2.0),
                T,
                A
        );
        hosekWilkieModel = hosekWilkie;
        skyboxRenderer = new SkyboxRenderer(skybox, "hosek-wilkie", List.of(new HosekWilkieSkyboxModule(camera, hosekWilkie)));

//         renderManager init
        renderManager.init(camera, terrainRenderer, skyboxRenderer);

        // Light
        float lightIntensity = 1.0f;
        Vector3f lightPosition = new Vector3f(1f, 0f, 1f);
        Vector3f lightColor = new Vector3f(1f, 1f, 1f);
        sceneManager.setDirectionalLight(new DirectionalLight(lightColor, lightPosition, lightIntensity));
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
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_R.val)) {
            hosekWilkieModel.rotateSun(-1);
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_F.val)) {
            hosekWilkieModel.rotateSun(1);
        }
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

        Vector3f pos = camera.getPosition();
        terrainGenerator.updateChunksAround((int) pos.x, (int) pos.z, sceneManager);


        if (mouseInput.isRightButtonPressed()) {
            Vector2f rotVec = mouseInput.getDisplVec();
            camera.moveRotation(rotVec.x * Constants.MOUSE_SENSITIVITY, rotVec.y * Constants.MOUSE_SENSITIVITY, 0);
        }

        for (Terrain terrain : sceneManager.getTerrains()) {
            terrainRenderer.processTerrain(terrain);
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