package palecek;

import palecek.core.ApplicationManager;
import palecek.core.ILogic;
import palecek.core.WindowManager;
import palecek.core.utils.Constants;
import palecek.scene.TerrainScene;

public class Main {

    private static WindowManager windowManager;
    private static ILogic scene;

    public static void main(String[] args) {
        windowManager = new WindowManager(Constants.TITLE, 0, 0, true);
        scene = new TerrainScene();
        ApplicationManager applicationManager = new ApplicationManager();
        try {
            applicationManager.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static WindowManager getWindowManager() {
        return windowManager;
    }
    public static ILogic getScene() {
        return scene;
    }
}