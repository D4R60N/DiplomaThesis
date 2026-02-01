package palecek.logic;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import palecek.Main;
import palecek.core.*;
import palecek.core.entity.SceneManager;
import palecek.core.gui.ImGuiLayer;
import palecek.core.lighting.DirectionalLight;
import palecek.core.planet.Planet;
import palecek.core.planet.PlanetGenerator;
import palecek.core.rendering.PlanetRenderer;
import palecek.core.rendering.RenderManager;
import palecek.core.rendering.TerrainRenderer;
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

import static org.lwjgl.opengl.GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;


public class HillaireSmallPlanetScene implements ILogic {
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
    private HillariePrecompute hillariePrecompute;
    private DirectionalLight directionalLight;

    private float speed = 0.0f;

    private Camera camera;
    Vector3f cameraInc;

    public HillaireSmallPlanetScene() {
        renderManager = new RenderManager();
        objectLoader = new ObjectLoader();
        camera = new Camera();
        cameraInc = new Vector3f(0, 0, 0);
        camera.setPosition(0, -6.36f, 10f);
        sceneManager = new SceneManager(0, camera);
        directionalLight = new DirectionalLight(new Vector3f(1f,1f,1f), new Vector3f(1f,0,0f), 1.0f);
    }

    @Override
    public void init(MouseInput mouseInput) throws Exception {
        windowManager = Main.getWindowManager();
        logicManager = Main.getLogicManager();
        showGui = false;
        imGuiLayer = new PauseMenu(windowManager.getWindow(), logicManager, () -> showGui = false);

        // Planet
        planetRenderer = new PlanetRenderer();
        windowManager.updateProjectionMatrix();
        hillarieModel = HillarieModel.getSmallPlanet();
        Vector2i transmittanceSize = new Vector2i(256, 64);
        Vector2i scatteringSize = new Vector2i(32, 32);
        Vector2i skyViewSize = new Vector2i(200, 100);
        Vector3i arialPerspectiveSize = new Vector3i(32);
        hillariePrecompute = new HillariePrecompute(transmittanceSize, scatteringSize, skyViewSize, arialPerspectiveSize);
        ITexture[] textures = hillariePrecompute.precompute(hillarieModel, camera);

//        ITexture[] texturesArray = {
//                new Texture(transmittanceSize.x, transmittanceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, RawTextureExporter.loadRawTexture("images/bruneton/test/transmittance.dat", transmittanceSize.x, transmittanceSize.y, 1, 4)),
//                new Texture(irradianceSize.x, irradianceSize.y, GL_RGBA32F, GL_RGBA, GL_FLOAT, RawTextureExporter.loadRawTexture("images/bruneton/test/irradiance.dat", irradianceSize.x, irradianceSize.y, 1, 4)),
//                new Texture3D(scatteringSize.x, scatteringSize.y, scatteringSize.z, GL_RGBA32F, GL_RGBA, GL_FLOAT, RawTextureExporter.loadRawTexture("images/bruneton/test/scattering.dat", scatteringSize.x, scatteringSize.y, scatteringSize.z, 4)),
//                textures[3]
//        };

        // Terrain
//        terrainRenderer = new TerrainRenderer();
//        int[] lods = {8, 16};
//        short[] lodDistances = {8, 12};
//        terrainGenerator = new TerrainGenerator(objectLoader, new ComputeShaderManager(), 500, 32, 36, 128, lods, lodDistances);


        HillariePostprocessModule hillariePostprocessModule = new HillariePostprocessModule(hillarieModel, textures, scatteringSize, transmittanceSize, skyViewSize, arialPerspectiveSize);
        renderManager.init(List.of(hillariePostprocessModule), "hillaire_small_planet", camera, planetRenderer);

        planetGenerator = new PlanetGenerator(objectLoader, new ComputeShaderManager());
        planetGenerator.createPlanet(0, -6.36f, 0, 6.36f, 0f, 0f, new Vector2f(0, 0), 0f, sceneManager, Planet.PlanetType.TEMPERATE);


        // Light
        float lightIntensity = 1.0f;
        Vector3f lightPosition = hillarieModel.getSunDirection();
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
            hillariePrecompute.updateMultiScattering(hillarieModel);
            directionalLight.setDirection(hillarieModel.getSunDirection());
            sceneManager.setDirectionalLight(directionalLight);
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_F.val)) {
            hillarieModel.rotateSun(.01f, 0);
            hillariePrecompute.updateMultiScattering(hillarieModel);
            directionalLight.setDirection(hillarieModel.getSunDirection());
            sceneManager.setDirectionalLight(directionalLight);
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
            speed = .0001f;
        } else {
            speed = .00001f;
        }
    }


    @Override
    public void update(float interval, MouseInput mouseInput) {
        camera.movePosition(cameraInc.x * Constants.CAMERA_MOVEMENT_SPEED,
                cameraInc.y * Constants.CAMERA_MOVEMENT_SPEED,
                cameraInc.z * Constants.CAMERA_MOVEMENT_SPEED);
        Vector3f pos = camera.getPosition();
//        terrainGenerator.updateChunksAround((int) pos.x, (int) pos.z, sceneManager);
        if (mouseInput.isRightButtonPressed()) {
            Vector2f rotVec = mouseInput.getDisplVec();
            camera.moveRotation(rotVec.x * Constants.MOUSE_SENSITIVITY, rotVec.y * Constants.MOUSE_SENSITIVITY, 0);
        }

        for (Planet planet : sceneManager.getPlanets()) {
            planetRenderer.processPlanet(planet);
        }
        hillariePrecompute.updateSkyAndFog(hillarieModel, camera);
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
//        for (Terrain terrain : sceneManager.getTerrains()) {
//            terrainRenderer.processTerrain(terrain);
//        }
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