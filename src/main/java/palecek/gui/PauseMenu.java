package palecek.gui;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import palecek.Main;
import palecek.core.LogicManager;
import palecek.core.WindowManager;
import palecek.core.gui.ImGuiLayer;

public class PauseMenu extends ImGuiLayer {
    private final int windowFlags =
            ImGuiWindowFlags.NoMove |
                    ImGuiWindowFlags.NoResize |
                    ImGuiWindowFlags.NoCollapse;
    //                    ImGuiWindowFlags.NoTitleBar;
    private float displayWidth;
    private float displayHeight;
    private float windowWidth = 300;
    private float windowHeight = 600;
    private float posX;
    private float posY;

    private LogicManager logicManager;
    private CloseCallback closeCallback;
    private IAddedWindow addedWindow;

    public PauseMenu(long window, LogicManager logicManager, CloseCallback closeCallback, IAddedWindow addedWindow) {
        super(window);
        this.logicManager = logicManager;
        this.closeCallback = closeCallback;
        this.addedWindow = addedWindow;
    }
    public PauseMenu(long window, LogicManager logicManager, CloseCallback closeCallback) {
        super(window);
        this.logicManager = logicManager;
        this.closeCallback = closeCallback;
        this.addedWindow = (windowWidth, windowHeight) -> {;
            // No additional window
        };
    }

    @Override
    protected void setupImGui() {
        // Get the screen size

        displayWidth = ImGui.getIO().getDisplaySizeX();
        displayHeight = ImGui.getIO().getDisplaySizeY();
        posX = (displayWidth - windowWidth) / 2.0f;
        posY = (displayHeight - windowHeight) / 2.0f;
        ImGui.setNextWindowPos(posX, posY, ImGuiCond.Always);
        ImGui.setNextWindowSize(windowWidth, windowHeight, ImGuiCond.Always);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 8f);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 12f, 8f);


        ImGui.begin("Main Menu", windowFlags);


        if (centeredButton("Preetham", 200, 30)) {
            logicManager.switchToLogic(1);
            closeCallback.execute();
        }

        if (centeredButton("Hosek Wilkie", 200, 30)) {
            logicManager.switchToLogic(2);
            closeCallback.execute();
        }

        if (centeredButton("Bruneton", 200, 30)) {
            logicManager.switchToLogic(3);
            closeCallback.execute();
        }

        if (centeredButton("Hillaire", 200, 30)) {
            logicManager.switchToLogic(4);
            closeCallback.execute();
        }

        if (centeredButton("Hillaire Small Planet", 200, 30)) {
            logicManager.switchToLogic(5);
            closeCallback.execute();
        }

        if (centeredButton("Main Menu", 200, 30)) {
            logicManager.switchToLogic(0);
            closeCallback.execute();
        }

        ImGui.popStyleVar(2);
        ImGui.end();

        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(400, Main.getWindowManager().getHeight(), ImGuiCond.Always);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 8f);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 12f, 8f);


        ImGui.begin("Params", windowFlags);
        ImGui.setCursorPosX(10);
        ImGui.setCursorPosY(10);

        addedWindow.setup(windowWidth, windowHeight);


        ImGui.popStyleVar(2);
        ImGui.end();
    }

    private boolean centeredButton(String label, float width, float height) {
        float windowWidth = ImGui.getWindowSizeX();
        float cursorX = (windowWidth - width) / 2.0f;
        float cursorY = (windowHeight - height) / 12.0f;

        ImGui.setCursorPosX(cursorX);
        ImGui.setCursorPosY(cursorY + ImGui.getCursorPosY());

        return ImGui.button(label, width, height);
    }

    public interface CloseCallback {
        void execute();
    }

}
