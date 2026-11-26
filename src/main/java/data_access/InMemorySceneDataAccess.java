package data_access;

import entity.Scene;
import entity.SceneSnapshot;
import use_case.simulation.SimulationDataAccessInterface;
import use_case.drop_object.DropObjectDataAccessInterface;

import java.util.Objects;

/** The single scene that all use cases in the running app share. */
public final class InMemorySceneDataAccess implements SimulationDataAccessInterface, DropObjectDataAccessInterface {
    private Scene currentScene;
    private SceneSnapshot startSnapshot;

    public InMemorySceneDataAccess(Scene initialScene) {
        replaceCurrentScene(initialScene);
    }

    @Override
    public Scene getCurrentScene() {
        return currentScene;
    }

    @Override
    public boolean isInsideTerrain(double x, double y) {
        return currentScene.getTerrain() != null && currentScene.getTerrain().isInside(x, y);
    }

    @Override
    public double getTerrainHeightAt(double x, double y) {
        if (currentScene.getTerrain() == null || !currentScene.getTerrain().isInside(x, y)) {
            throw new IllegalArgumentException("Position is outside the current terrain");
        }
        return currentScene.getTerrain().heightAt(x, y);
    }

    @Override
    public void saveCurrentScene(Scene scene) {
        currentScene = Objects.requireNonNull(scene, "scene");
    }

    /** Also forgets the Play snapshot, since it belonged to the old scene. */
    public void replaceCurrentScene(Scene scene) {
        currentScene = Objects.requireNonNull(scene, "scene");
        startSnapshot = null;
    }

    @Override
    public SceneSnapshot getStartSnapshot() {
        return startSnapshot;
    }

    @Override
    public void setStartSnapshot(SceneSnapshot snapshot) {
        startSnapshot = snapshot;
    }
}
