package use_case.drop_object;

import use_case.scene.SceneViewData;

public class DropObjectOutputData {

    private final boolean dropped;
    private final SceneViewData scene;

    public DropObjectOutputData(boolean dropped, SceneViewData scene) {
        this.dropped = dropped;
        this.scene = scene;
    }

    public boolean wasDropped() {
        return dropped;
    }

    public SceneViewData getScene() {
        return scene;
    }
}
