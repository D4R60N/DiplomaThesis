package palecek.gui;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import palecek.core.LogicManager;
import palecek.core.MouseInput;
import palecek.core.gui.ImGuiLayer;

public class MainMenu extends ImGuiLayer {
    private final int windowFlags =
            ImGuiWindowFlags.NoMove |
                    ImGuiWindowFlags.NoResize |
                    ImGuiWindowFlags.NoCollapse;
    //                    ImGuiWindowFlags.NoTitleBar;
    private float displayWidth;
    private float displayHeight;
    private float windowWidth = 400;
    private float windowHeight = 600;
    private float posX;
    private float posY;

    private final LogicManager logicManager;
    private final MouseInput mouseInput;

    public MainMenu(long window, LogicManager logicManager, MouseInput mouseInput) {
        super(window);
        this.logicManager = logicManager;
        this.mouseInput = mouseInput;
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


        if (centeredButton("Preetham", 200, 40)) {
            logicManager.switchToLogic(1);
            mouseInput.restoreMouseForApp();
        }

        if (centeredButton("Hosek Wilkie", 200, 40)) {
            logicManager.switchToLogic(2);
            mouseInput.restoreMouseForApp();
        }

        if (centeredButton("Nishita", 200, 40)) {
            logicManager.switchToLogic(3);
            mouseInput.restoreMouseForApp();
        }

        if (centeredButton("Quit", 200, 40)) {

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


}
