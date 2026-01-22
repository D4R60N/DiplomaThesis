package palecek.logic;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4i;
import palecek.Main;
import palecek.bruneton.BrunetonModel;
import palecek.bruneton.BrunetonPostprocessModule;
import palecek.bruneton.BrunetonPrecompute;
import palecek.core.*;
import palecek.core.entity.SceneManager;
import palecek.core.gui.ImGuiLayer;
import palecek.core.lighting.DirectionalLight;
import palecek.core.planet.PlanetGenerator;
import palecek.core.rendering.PlanetRenderer;
import palecek.core.rendering.RenderManager;
import palecek.core.rendering.TerrainRenderer;
import palecek.core.terrain.Terrain;
import palecek.core.terrain.TerrainGenerator;
import palecek.core.utils.Constants;
import palecek.core.utils.ITexture;
import palecek.core.utils.RenderMode;
import palecek.core.utils.glfw.GLFWEnum;
import palecek.gui.PauseMenu;
import palecek.hillaire.HillarieModel;
import palecek.hillaire.HillariePostprocessModule;
import palecek.hillaire.HillariePrecompute;

import java.util.List;


public class HillaireScene implements ILogic {
    private final RenderManager renderManager;
    private final ObjectLoader objectLoader;
    private WindowManager windowManager;
    private final SceneManager sceneManager;
    private PlanetGenerator planetGenerator;
    private PlanetRenderer planetRenderer;
    private HillarieModel hillarieModel;
    private ImGuiLayer imGuiLayer;
    private boolean showGui;
    private TerrainRenderer terrainRenderer;
    private TerrainGenerator terrainGenerator;
    private LogicManager logicManager;

    private float speed = 0.0f;

    private Camera camera;
    Vector3f cameraInc;

    public HillaireScene() {
        renderManager = new RenderManager();
        objectLoader = new ObjectLoader();
        camera = new Camera();
        cameraInc = new Vector3f(0, 0, 0);
        camera.setPosition(0, 10, 0);
        sceneManager = new SceneManager(-90, camera);
    }

    @Override
    public void init(MouseInput mouseInput) throws Exception {
        windowManager = Main.getWindowManager();
        logicManager = Main.getLogicManager();
        showGui = false;
        imGuiLayer = new PauseMenu(windowManager.getWindow(), logicManager, () -> showGui = false);

        // Planet
//        planetRenderer = new PlanetRenderer();
        hillarieModel = new HillarieModel();
        // renderManager init
        HillariePrecompute hillariePrecompute = new HillariePrecompute();
        Vector2i transmittanceSize = new Vector2i(256, 64);
        Vector2i irradianceSize = new Vector2i(64, 16);
        Vector4i scatteringSize = new Vector4i(32, 128, 32, 8);
        ITexture[] textures = hillariePrecompute.precompute(new ComputeShaderManager(), transmittanceSize, irradianceSize, scatteringSize, hillarieModel);

//        ITexture[] texturesArray = {
//                new Texture(transmittanceSize.x, transmittanceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, RawTextureExporter.loadRawTexture("images/bruneton/test/transmittance.dat", transmittanceSize.x, transmittanceSize.y, 1, 4)),
//                new Texture(irradianceSize.x, irradianceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, RawTextureExporter.loadRawTexture("images/bruneton/test/irradiance.dat", irradianceSize.x, irradianceSize.y, 1, 4)),
//                new Texture3D(scatteringSize.x, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, RawTextureExporter.loadRawTexture("images/bruneton/test/scattering.dat", scatteringSize.x, scatteringSize.y, scatteringSize.z, 4)),
//                textures[3]
//        };

        // Terrain
        terrainRenderer = new TerrainRenderer();
        int[] lods = {8, 16};
        short[] lodDistances = {8, 12};
        terrainGenerator = new TerrainGenerator(objectLoader, new ComputeShaderManager(), 500, 32, 36, 128, lods, lodDistances);


        HillariePostprocessModule hillariePostprocessModule = new HillariePostprocessModule(hillarieModel, textures, scatteringSize, transmittanceSize, irradianceSize);
        renderManager.init(List.of(hillariePostprocessModule), "hillaire", camera, terrainRenderer);

//        planetGenerator = new PlanetGenerator(objectLoader, new ComputeShaderManager());
//        planetGenerator.createPlanet(0, 0, -1000, 200f, 2f, 0f, new Vector2f(0, 0), 1f, sceneManager, Planet.PlanetType.TEMPERATE);


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
            hillarieModel.rotateSun(-.01f, 0);
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_F.val)) {
            hillarieModel.rotateSun(.01f, 0);
        }
        if (windowManager.isKeyTyped(GLFWEnum.GLFW_KEY_TAB.val)) {
            if (!showGui) {
                mouseInput.redirectMouseToGui(imGuiLayer.getImGuiGlfw());
            } else {
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

//        for (Planet planet : sceneManager.getPlanets()) {
//            planetRenderer.processPlanet(planet);
//        }
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