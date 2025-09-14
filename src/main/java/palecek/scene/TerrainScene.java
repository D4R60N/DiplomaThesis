package palecek.scene;

import org.joml.Vector2f;
import org.joml.Vector3f;
import palecek.Main;
import palecek.core.*;
import palecek.core.entity.Entity;
import palecek.core.entity.SceneManager;
import palecek.core.lighting.DirectionalLight;
import palecek.core.lighting.PointLight;
import palecek.core.planet.Planet;
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

import java.util.Arrays;
import java.util.List;

public class TerrainScene implements ILogic {
    private final RenderManager renderManager;
    private final ObjectLoader objectLoader;
    private final WindowManager windowManager;
    private final SceneManager sceneManager;
    private TerrainGenerator terrainGenerator;
    private TerrainRenderer terrainRenderer;
    private SkyboxRenderer skyboxRenderer;

    private Camera camera;
    Vector3f cameraInc;

    public TerrainScene() {
        renderManager = new RenderManager();
        windowManager = Main.getWindowManager();
        objectLoader = new ObjectLoader();
        camera = new Camera();
        cameraInc = new Vector3f(0, 0, 0);
        sceneManager = new SceneManager(-90, camera);
    }

    @Override
    public void init() throws Exception {
        // Terrain
        terrainRenderer = new TerrainRenderer();
        terrainGenerator = new TerrainGenerator(objectLoader, new ComputeShaderManager(), 100, 5, 8);
        terrainGenerator.createTerrain(0,0,0,sceneManager);

        // Skybox
        List<String> faces = Arrays.asList(
                "textures/skybox/px.png",   // +X
                "textures/skybox/nx.png",    // -X
                "textures/skybox/py.png",     // +Y
                "textures/skybox/ny.png",  // -Y
                "textures/skybox/pz.png",   // +Z
                "textures/skybox/nz.png"     // -Z
        );
        Skybox skybox = new Skybox(new SkyboxTexture(faces), objectLoader);
        skyboxRenderer = new SkyboxRenderer(skybox);

        // renderManager init
        renderManager.init(null, camera, terrainRenderer, skyboxRenderer);

        // Light
        float lightIntensity = 1.0f;
        Vector3f lightPosition = new Vector3f(1f, 0f, 1f);
        Vector3f lightColor = new Vector3f(1f, 1f, 1f);
        sceneManager.setDirectionalLight(new DirectionalLight(lightColor, lightPosition, lightIntensity));
    }

    @Override
    public void input() {
        cameraInc.set(0, 0, 0);
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_W.val)) {
            cameraInc.z = -0.02f;
        } else if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_S.val)) {
            cameraInc.z = 0.02f;
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_A.val)) {
            cameraInc.x = -0.02f;
        } else if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_D.val)) {
            cameraInc.x = 0.02f;
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_Q.val)) {
            cameraInc.y = -0.02f;
        } else if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_E.val)) {
            cameraInc.y = 0.02f;
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

        for (Planet planet : sceneManager.getPlanets()) {
            planet.incRotationAngle(0.1f);
        }

        for (Entity entity : sceneManager.getEntities()) {
            renderManager.processEntity(entity);
        }
        for (Terrain terrain : sceneManager.getTerrains()) {
            terrainRenderer.processTerrain(terrain);
        }
    }

    @Override
    public void render() {
        renderManager.render(camera, sceneManager, RenderMode.GL_TRIANGLES.getMode());
    }

    @Override
    public void cleanup() {
        renderManager.cleanup();
        objectLoader.cleanUp();
    }
}
