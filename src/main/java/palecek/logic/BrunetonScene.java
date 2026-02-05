package palecek.logic;

import imgui.ImGui;
import org.joml.*;
import palecek.Main;
import palecek.bruneton.BrunetonModel;
import palecek.bruneton.BrunetonPostprocessModule;
import palecek.core.*;
import palecek.core.entity.SceneManager;
import palecek.core.gui.ImGuiLayer;
import palecek.core.lighting.DirectionalLight;
import palecek.core.rendering.RenderManager;
import palecek.core.rendering.TerrainRenderer;
import palecek.core.terrain.Terrain;
import palecek.core.terrain.TerrainGenerator;
import palecek.core.utils.*;
import palecek.core.utils.glfw.GLFWEnum;
import palecek.gui.IAddedWindow;
import palecek.gui.PauseMenu;

import palecek.bruneton.BrunetonPrecompute;

import java.lang.Math;
import java.util.List;


public class BrunetonScene implements ILogic {
    private final RenderManager renderManager;
    private final ObjectLoader objectLoader;
    private WindowManager windowManager;
    private final SceneManager sceneManager;
    private BrunetonModel brunetonModel;
    private ImGuiLayer imGuiLayer;
    private boolean showGui;
    private TerrainRenderer terrainRenderer;
    private TerrainGenerator terrainGenerator;
    private LogicManager logicManager;

    private float speed = 0.0f;

    private Camera camera;
    Vector3f cameraInc;

    public BrunetonScene() {
        renderManager = new RenderManager();
        objectLoader = new ObjectLoader();
        camera = new Camera();
        cameraInc = new Vector3f(0, 0, 0);
        camera.setPosition(0, 1000, 0);
        sceneManager = new SceneManager(-90, camera);
    }

    @Override
    public void init(MouseInput mouseInput) throws Exception {
        windowManager = Main.getWindowManager();
        logicManager = Main.getLogicManager();
        showGui = false;

        BrunetonModel bruneton = new BrunetonModel();
        brunetonModel = bruneton;
        // renderManager init
        Vector2i transmittanceSize = new Vector2i(256, 64);
        Vector2i irradianceSize = new Vector2i(64, 16);
        Vector4i scatteringSize = new Vector4i(32, 128, 32, 8);
        BrunetonPrecompute brunetonPrecompute = new BrunetonPrecompute(transmittanceSize, irradianceSize, scatteringSize);
        ITexture[] textures = brunetonPrecompute.precompute(new ComputeShaderManager(), brunetonModel);

        // Terrain
        terrainRenderer = new TerrainRenderer();
        int[] lods = {8, 16};
        short[] lodDistances = {8, 12};
        terrainGenerator = new TerrainGenerator(objectLoader, new ComputeShaderManager(), 500, 32, 36, 128, lods, lodDistances);


        BrunetonPostprocessModule brunetonPostprocessModule = new BrunetonPostprocessModule(brunetonModel, textures, scatteringSize, transmittanceSize, irradianceSize);
        renderManager.init(List.of(brunetonPostprocessModule), "bruneton", camera, terrainRenderer);
        float[] valSolar = {brunetonModel.getSolarIrradiance().x, brunetonModel.getSolarIrradiance().y, brunetonModel.getSolarIrradiance().z};
        float[] valBottomR = {brunetonModel.getBottomRadius()};
        float[] valTopR = {brunetonModel.getTopRadius()};
        float[] valRayleigh = {brunetonModel.getRayleighScattering().x, brunetonModel.getRayleighScattering().y, brunetonModel.getRayleighScattering().z};
        float[] valMieS = {brunetonModel.getMieScattering().x, brunetonModel.getMieScattering().y, brunetonModel.getMieScattering().z};
        float[] valMieG = {brunetonModel.getMiePhaseFunctionG()};
        float[] valGAlbedo = {brunetonModel.getGroundAlbedo().x, brunetonModel.getGroundAlbedo().y, brunetonModel.getGroundAlbedo().z};
        float[] valExp = {brunetonModel.getExposure()};
        float[] valWhite = {brunetonModel.getWhitePoint().x, brunetonModel.getWhitePoint().y, brunetonModel.getWhitePoint().z};
        float[] valSunDir = {brunetonModel.getSunDirection().x, brunetonModel.getSunDirection().y, brunetonModel.getSunDirection().z};
        float[] valSunSize = {brunetonModel.getSunSize().x, brunetonModel.getSunSize().y};

        imGuiLayer = new PauseMenu(windowManager.getWindow(), logicManager, () -> showGui = false, (w, h) -> {

            if (IAddedWindow.centeredVector3("Solar Irradiance", valSolar, 0.0f, 100.0f, 200f, w, h)) {
                brunetonModel.setSolarIrradiance(new Vector3f(valSolar[0], valSolar[1], valSolar[2]));
            }

            ImGui.separator();
            ImGui.text("Planetary Parameters");

            if (IAddedWindow.centeredVector3("Ground Albedo", valGAlbedo, 0.0f, 1.0f, 200f, w, h)) {
                brunetonModel.setGroundAlbedo(new Vector3f(valGAlbedo[0], valGAlbedo[1], valGAlbedo[2]));
            }

            if (IAddedWindow.centeredSlider("Bottom Radius", valBottomR, 1000.0f, 10000.0f, 200f, w, h)) {
                brunetonModel.setBottomRadius(valBottomR[0]);
            }


            if (IAddedWindow.centeredSlider("Top Radius", valTopR, 1000.0f, 10000.0f, 200f, w, h)) {
                brunetonModel.setTopRadius(valTopR[0]);
            }

            ImGui.separator();
            ImGui.text("Reyleigh / Mie Parameters");

            if (IAddedWindow.centeredVector3("Rayleigh Scattering", valRayleigh, 0.0f, 0.1f, 200f, w, h)) {
                brunetonModel.setRayleighScattering(new Vector3f(valRayleigh[0], valRayleigh[1], valRayleigh[2]));
            }

            if (IAddedWindow.centeredVector3("Mie Scattering", valMieS, 0.0f, 0.1f, 200f, w, h)) {
                brunetonModel.setMieScattering(new Vector3f(valMieS[0], valMieS[1], valMieS[2]));
            }

            if (IAddedWindow.centeredSlider("Mie Phase G", valMieG, -0.999f, 0.999f, 200f, w, h)) {
                brunetonModel.setMiePhaseFunctionG(valMieG[0]);
            }

            ImGui.separator();
            ImGui.text("Rendering Parameters");

            if (IAddedWindow.centeredSlider("Exposure", valExp, 0.0f, 10.0f, 200f, w, h)) {
                brunetonModel.setExposure(valExp[0]);
            }

            if (IAddedWindow.centeredVector3("White Point", valWhite, 0.0f, 2.0f, 200f, w, h)) {
                brunetonModel.setWhitePoint(new Vector3f(valWhite[0], valWhite[1], valWhite[2]));
            }

            if (IAddedWindow.centeredVector3("Sun Direction", valSunDir, -1.0f, 1.0f, 200f, w, h)) {
                brunetonModel.setSunDirection(new Vector3f(valSunDir[0], valSunDir[1], valSunDir[2]));
            }

            if (IAddedWindow.centeredVector2("Sun Size", valSunSize, 0.0f, 1.0f, 200f, w, h)) {
                brunetonModel.setSunSize(new Vector2f(valSunSize[0], valSunSize[1]));
            }

            if (IAddedWindow.centeredButton("Recompute Atmosphere", 200f, 100f, w, h)) {
                try {
                    brunetonPrecompute.precompute(new ComputeShaderManager(), brunetonModel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            brunetonModel.rotateSun(-.01f, 0);
        }
        if (windowManager.isKeyPressed(GLFWEnum.GLFW_KEY_F.val)) {
            brunetonModel.rotateSun(.01f, 0);
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
        imGuiLayer.dispose();
        renderManager.cleanup();
        objectLoader.cleanUp();
    }


}