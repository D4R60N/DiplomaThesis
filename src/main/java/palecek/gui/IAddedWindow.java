package palecek.gui;

import imgui.ImGui;

public interface IAddedWindow {
    void setup(float windowWidth, float windowHeight);
    public static boolean centeredButton(String label, float width, float height, float windowWidth, float windowHeight) {

        float cursorX = 10f;
        float cursorY = (windowHeight - height) / 12.0f;

        ImGui.setCursorPosX(cursorX);
        ImGui.setCursorPosY(cursorY + ImGui.getCursorPosY());

        return ImGui.button(label, width, height);
    }
    public static boolean centeredSlider(String label, float[] value, float min, float max, float width, float windowWidth, float windowHeight) {
        float cursorX = 10f;
        float cursorY = (windowHeight - 20f) / 12.0f;

        ImGui.setCursorPosX(cursorX);
        ImGui.setCursorPosY(cursorY + ImGui.getCursorPosY());

        ImGui.setNextItemWidth(width);

        return ImGui.sliderFloat(label, value, min, max);
    }
    public static boolean centeredVector3(String label, float[] values, float min, float max, float width, float windowWidth, float windowHeight) {
        float cursorX = 10f;
        float cursorY = (windowHeight - 20f) / 12.0f;

        ImGui.setCursorPosX(cursorX);
        ImGui.setCursorPosY(cursorY + ImGui.getCursorPosY());

        ImGui.setNextItemWidth(width);

        return ImGui.sliderFloat3(label, values, min, max);
    }
    public static boolean centeredVector2(String label, float[] values, float min, float max, float width, float windowWidth, float windowHeight) {
        float cursorX = 10f;
        float cursorY = (20f) / 12.0f;
        ImGui.setCursorPosX(cursorX);
        ImGui.setCursorPosY(cursorY + ImGui.getCursorPosY());
        ImGui.setNextItemWidth(width);
        return ImGui.sliderFloat2(label, values, min, max);
    }
}
