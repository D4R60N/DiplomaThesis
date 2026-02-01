package palecek.gui;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import palecek.core.LogicManager;
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

    public PauseMenu(long window, LogicManager logicManager, CloseCallback closeCallback) {
        super(window);
        this.logicManager = logicManager;
        this.closeCallback = closeCallback;
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


        if (centeredButton("Preetham", 100, 30)) {
            logicManager.switchToLogic(1);
            closeCallback.execute();
        }

        if (centeredButton("Hosek Wilkie", 100, 30)) {
            logicManager.switchToLogic(2);
            closeCallback.execute();
        }

        if (centeredButton("Bruneton", 100, 30)) {
            logicManager.switchToLogic(3);
            closeCallback.execute();
        }

        if (centeredButton("Hillaire", 100, 30)) {
            logicManager.switchToLogic(4);
            closeCallback.execute();
        }

        if (centeredButton("Hillaire Small Planet", 100, 30)) {
            logicManager.switchToLogic(5);
            closeCallback.execute();
        }

        if (centeredButton("Main Menu", 100, 30)) {
            logicManager.switchToLogic(0);
            closeCallback.execute();
        }


        ImGui.popStyleVar(2);
        ImGui.end();
    }

    private boolean centeredButton(String label, float width, float height) {
        float windowWidth = ImGui.getWindowSizeX();
        float cursorX = (windowWidth - width) / 2.0f;
        float cursorY = (windowHeight - height) / 6.0f;

        ImGui.setCursorPosX(cursorX);
        ImGui.setCursorPosY(cursorY + ImGui.getCursorPosY());

        return ImGui.button(label, width, height);
    }

    public interface CloseCallback {
        void execute();
    }

}
