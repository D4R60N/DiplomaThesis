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
import palecek.preetham.PreethamModel;
import palecek.preetham.PreethamSkyboxModule;
import palecek.utils.SunVector;

import java.util.Arrays;
import java.util.List;

public class PreethamScene implements ILogic {
    private final RenderManager renderManager;
    private final ObjectLoader objectLoader;
    private WindowManager windowManager;
    private final SceneManager sceneManager;
    private TerrainGenerator terrainGenerator;
    private TerrainRenderer terrainRenderer;
    private SkyboxRenderer skyboxRenderer;
    private PreethamModel preethamModel;
    private ImGuiLayer imGuiLayer;
    private boolean showGui;
    private LogicManager logicManager;

    private float speed = 0.0f;

    private Camera camera;
    Vector3f cameraInc;

    public PreethamScene() {
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

        float T = 2.0f;
        Vector3f A = new Vector3f(0.1787f*T - 1.4630f,  -0.0193f*T - 0.2592f, -0.0167f*T - 0.2608f);
        Vector3f B = new Vector3f(-0.3554f*T + 0.4275f,  -0.0665f*T + 0.0008f,  -0.0950f*T + 0.0092f);
        Vector3f C = new Vector3f(-0.0227f*T + 5.3251f,  -0.0004f*T + 0.2125f, -0.0079f*T + 0.2102f);
        Vector3f D = new Vector3f(0.1206f*T - 2.5771f,  -0.0641f*T - 0.8989f, -0.0441f*T - 1.6537f);
        Vector3f E = new Vector3f(-0.0670f*T + 0.3703f,   -0.0033f*T + 0.0452f,  -0.0109f*T + 0.0529f);
        float sunAngle = (float) Math.toRadians(0);

        Vector3f Z = PreethamModel.computeZenith(T, sunAngle);
        PreethamModel preetham = new PreethamModel(
                new SunVector((float) Math.toRadians(0), (float) Math.toRadians(90)),
                A,
                B,
                C,
                D,
                E,
                Z,
                new Vector3f(1.0f, 0.95f, 0.85f),
                (float)Math.toRadians(0.266),
                (float)Math.toRadians(1.0),
                T,
                sunAngle
        );
        preethamModel = preetham;
        skyboxRenderer = new SkyboxRenderer(skybox, "preetham", List.of(new PreethamSkyboxModule(camera, preetham)));

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
            preethamModel.rotateSun(-1);
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_F.val)) {
            preethamModel.rotateSun(1);
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