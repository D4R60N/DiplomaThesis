package palecek.logic;

import imgui.ImGui;
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
import palecek.core.utils.Constants;
import palecek.core.utils.ITexture;
import palecek.core.utils.ImageUtils;
import palecek.core.utils.RenderMode;
import palecek.core.utils.glfw.GLFWEnum;
import palecek.gui.IAddedWindow;
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
        camera.setPosition(0f, -6.36f, 10f);
        sceneManager = new SceneManager(0, camera);
        directionalLight = new DirectionalLight(new Vector3f(1f,1f,1f), new Vector3f(1f,0,0f), 1.0f);
    }

    @Override
    public void init(MouseInput mouseInput) throws Exception {
        windowManager = Main.getWindowManager();
        logicManager = Main.getLogicManager();
        showGui = false;

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

        HillariePostprocessModule hillariePostprocessModule = new HillariePostprocessModule(hillarieModel, textures, scatteringSize, transmittanceSize, skyViewSize, arialPerspectiveSize);
        renderManager.init(List.of(hillariePostprocessModule), "hillaire_small_planet", camera, planetRenderer);

        planetGenerator = new PlanetGenerator(objectLoader, new ComputeShaderManager());
        planetGenerator.createPlanet(0, -6.36f, 0, 6.36f, 0f, 0f, new Vector2f(0, 0), 0f, sceneManager, Planet.PlanetType.TEMPERATE);

        float[] valBottomRadius = new float[]{hillarieModel.getBottomRadius()};
        float[] valTopRadius = new float[]{hillarieModel.getTopRadius()};
        float[] valRayleighDensityExpScale = new float[]{hillarieModel.getRayleighDensityExpScale()};
        float[] valRayleighScattering = new float[]{hillarieModel.getRayleighScattering().x, hillarieModel.getRayleighScattering().y, hillarieModel.getRayleighScattering().z};
        float[] valMieDensityExpScale = new float[]{hillarieModel.getMieDensityExpScale()};
        float[] valMieScattering = new float[]{hillarieModel.getMieScattering().x, hillarieModel.getMieScattering().y, hillarieModel.getMieScattering().z};
        float[] valMieExtinction = new float[]{hillarieModel.getMieExtinction().x, hillarieModel.getMieExtinction().y, hillarieModel.getMieExtinction().z};
        float[] valMieAbsorption = new float[]{hillarieModel.getMieAbsorption().x, hillarieModel.getMieAbsorption().y, hillarieModel.getMieAbsorption().z};
        float[] valMiePhaseG = new float[]{hillarieModel.getMiePhaseG()};
        float[] valAbsorptionDensity0LayerWidth = new float[]{hillarieModel.getAbsorptionDensity0LayerWidth()};
        float[] valAbsorptionExtinction = new float[]{hillarieModel.getAbsorptionExtinction().x, hillarieModel.getAbsorptionExtinction().y, hillarieModel.getAbsorptionExtinction().z};
        float[] valGroundAlbedo = new float[]{hillarieModel.getGroundAlbedo().x, hillarieModel.getGroundAlbedo().y, hillarieModel.getGroundAlbedo().z};
        float[] valExposure = new float[]{hillarieModel.getExposure()};
        float[] valSunIlluminance = new float[]{hillarieModel.getSunIlluminance().x, hillarieModel.getSunIlluminance().y, hillarieModel.getSunIlluminance().z};

        imGuiLayer = new PauseMenu(windowManager.getWindow(), logicManager, () -> showGui = false, (w, h) -> {
            ImGui.text("Atmosphere Geometry");
            if (IAddedWindow.centeredSlider("Bottom Radius", valBottomRadius, 1000f, 10000f, 200f, w, h))
                hillarieModel.setBottomRadius(valBottomRadius[0]);

            if (IAddedWindow.centeredSlider("Top Radius", valTopRadius, 1000f, 10000f, 200f, w, h))
                hillarieModel.setTopRadius(valTopRadius[0]);

            ImGui.separator();
            ImGui.text("Rayleigh Scattering");
            if (IAddedWindow.centeredSlider("Rayleigh Exp Scale", valRayleighDensityExpScale, -1f, 0f, 200f, w, h))
                hillarieModel.setRayleighDensityExpScale(valRayleighDensityExpScale[0]);

            if (IAddedWindow.centeredVector3("Rayleigh Scat", valRayleighScattering, 0f, 0.1f, 200f, w, h))
                hillarieModel.setRayleighScattering(new Vector3f(valRayleighScattering[0], valRayleighScattering[1], valRayleighScattering[2]));

            ImGui.separator();
            ImGui.text("Mie Scattering");
            if (IAddedWindow.centeredSlider("Mie Exp Scale", valMieDensityExpScale, -20f, 0f, 200f, w, h))
                hillarieModel.setMieDensityExpScale(valMieDensityExpScale[0]);

            if (IAddedWindow.centeredSlider("Mie Phase G", valMiePhaseG, -0.99f, 0.99f, 200f, w, h))
                hillarieModel.setMiePhaseG(valMiePhaseG[0]);

            if (IAddedWindow.centeredVector3("Mie Scat", valMieScattering, 0f, 0.1f, 200f, w, h))
                hillarieModel.setMieScattering(new Vector3f(valMieScattering[0], valMieScattering[1], valMieScattering[2]));

            if (IAddedWindow.centeredVector3("Mie Ext", valMieExtinction, 0f, 0.1f, 200f, w, h))
                hillarieModel.setMieExtinction(new Vector3f(valMieExtinction[0], valMieExtinction[1], valMieExtinction[2]));
            if (IAddedWindow.centeredVector3("Mie Abs", valMieAbsorption, 0f, 0.1f, 200f, w, h))
                hillarieModel.setMieAbsorption(new Vector3f(valMieAbsorption[0], valMieAbsorption[1], valMieAbsorption[2]));

            ImGui.separator();
            ImGui.text("Absorption / Ozone");
            if (IAddedWindow.centeredSlider("Abs Width", valAbsorptionDensity0LayerWidth, 0f, 50f, 200f, w, h))
                hillarieModel.setAbsorptionDensity0LayerWidth(valAbsorptionDensity0LayerWidth[0]);
            if (IAddedWindow.centeredVector3("Abs Extinction", valAbsorptionExtinction, 0f, 0.1f, 200f, w, h))
                hillarieModel.setAbsorptionExtinction(new Vector3f(valAbsorptionExtinction[0], valAbsorptionExtinction[1], valAbsorptionExtinction[2]));

            ImGui.separator();
            ImGui.text("Lighting & Tone");
            if (IAddedWindow.centeredVector3("Sun Illuminance", valSunIlluminance, 0f, 20f, 200f, w, h))
                hillarieModel.setSunIlluminance(new Vector3f(valSunIlluminance[0], valSunIlluminance[1], valSunIlluminance[2]));

            if (IAddedWindow.centeredVector3("Ground Albedo", valGroundAlbedo, 0f, 1f, 200f, w, h))
                hillarieModel.setGroundAlbedo(new Vector3f(valGroundAlbedo[0], valGroundAlbedo[1], valGroundAlbedo[2]));

            if (IAddedWindow.centeredSlider("Exposure", valExposure, 0f, 10f, 200f, w, h))
                hillarieModel.setExposure(valExposure[0]);

            if (IAddedWindow.centeredButton("Recompute Atmosphere", 200f, 100f, w, h)) {
                try {
                    hillariePrecompute.precompute(hillarieModel, camera);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

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