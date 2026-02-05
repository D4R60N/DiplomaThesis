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
import palecek.core.utils.ImageUtils;
import palecek.core.utils.RenderMode;
import palecek.core.utils.glfw.GLFWEnum;
import palecek.gui.IAddedWindow;
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

//        // Terrain
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
                A,
                .2f
        );
        hosekWilkieModel = hosekWilkie;
        skyboxRenderer = new SkyboxRenderer(skybox, "hosek-wilkie", List.of(new HosekWilkieSkyboxModule(camera, hosekWilkie)));

//         renderManager init
        renderManager.init(camera, skyboxRenderer, terrainRenderer);
        float[] valE = new float[]{hosekWilkieModel.getExposure()};
        float[] valT = new float[]{hosekWilkieModel.getTurbidity()};
        float[] valA = new float[]{hosekWilkieModel.getAlbedo()};
        float[] valAR = new float[]{hosekWilkieModel.getSunAngularRadius()};
        float[] valGR = new float[]{hosekWilkieModel.getGlowRadius()};
        float[] valC = new float[]{hosekWilkieModel.getSunColor().x, hosekWilkieModel.getSunColor().y, hosekWilkieModel.getSunColor().z};

        imGuiLayer = new PauseMenu(windowManager.getWindow(), logicManager, () -> showGui = false, (w, h) -> {


            if (IAddedWindow.centeredSlider("Exposure", valE, 0.0f, 2.0f, 200f, w,h)) {
                hosekWilkieModel.setExposure(valE[0]);
            }
            if (IAddedWindow.centeredSlider("Turbidity", valT, 1.0f, 10.0f, 200f, w,h)) {
                hosekWilkieModel.setTurbidity(valT[0]);
                hosekWilkieModel.recalculate();
            }
            if (IAddedWindow.centeredSlider("Albedo", valA, 0.0f, 1.0f, 200f, w,h)) {
                hosekWilkieModel.setAlbedo(Math.round(valA[0]));
                hosekWilkieModel.recalculate();
            }
            if (IAddedWindow.centeredSlider("Sun Angular Radius", valAR, 0.0f, 1.0f, 200f, w,h)) {
                hosekWilkieModel.setSunAngularRadius(valAR[0]);
            }
            if (IAddedWindow.centeredSlider("Glow Radius", valGR, 0.0f, 5.0f, 200f, w,h)) {
                hosekWilkieModel.setGlowRadius(valGR[0]);
            }
            if (IAddedWindow.centeredVector3("Sun Color", valC, 0.0f, 1.0f, 200f, w, h)) {
                hosekWilkieModel.setSunColor(new Vector3f(valC[0], valC[1], valC[2]));
            }
        });

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
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_P.val)) {
            ImageUtils.saveImage(1920, 1000);
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
//        imGuiLayer.dispose();
        renderManager.cleanup();
        objectLoader.cleanUp();
    }



}