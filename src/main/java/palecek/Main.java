package palecek;

import palecek.core.ApplicationManager;
import palecek.core.LogicManager;
import palecek.core.WindowManager;
import palecek.core.utils.Constants;
import palecek.logic.HosekWilkieScene;
import palecek.logic.MainMenuScene;
import palecek.logic.PreethamScene;

import java.util.List;

public class Main {

    private static WindowManager windowManager = new WindowManager(Constants.TITLE, 0, 0, true);
    private static LogicManager logicManager = new LogicManager(
            List.of(
                    new MainMenuScene(),
                    new PreethamScene(),
                    new HosekWilkieScene()
            )
    );

    public static void main(String[] args) {

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

    public static LogicManager getLogicManager() {
        return logicManager;
    }
}