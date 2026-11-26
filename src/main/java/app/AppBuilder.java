package app;

import data_access.FileSceneDataAccess;
import data_access.FileSceneDataAccess.TerrainMapper;
import data_access.InMemorySceneDataAccess;
import entity.Scene;
import entity.BallFactory;
import entity.SimplePhysicsEngine;
import interface_adapter.drop_object.DropObjectController;
import interface_adapter.drop_object.DropObjectPresenter;
import interface_adapter.drop_object.DropObjectViewModel;
import interface_adapter.load_scene.LoadSceneController;
import interface_adapter.save_scene.SaveSceneController;
import interface_adapter.simulation.ScenePresenter;
import interface_adapter.simulation.SimulationController;
import interface_adapter.simulation.SimulationState;
import interface_adapter.simulation.SimulationViewModel;
import use_case.load_scene.LoadSceneInteractor;
import use_case.drop_object.DropObjectInteractor;
import use_case.save_scene.SaveSceneInteractor;
import use_case.scene.SceneViewData;
import use_case.simulation.PlayInteractor;
import use_case.simulation.StepInteractor;
import use_case.simulation.StopInteractor;
import view.ScenePanel;

import javax.swing.JPanel;

public final class AppBuilder {
    private final ScenePanel app;

    public AppBuilder(Scene initialScene, TerrainMapper terrainMapper) {
        InMemorySceneDataAccess scenes = new InMemorySceneDataAccess(initialScene);
        FileSceneDataAccess files = new FileSceneDataAccess(scenes, terrainMapper);
        SceneViewData display = SceneViewData.from(initialScene,
                initialScene.getTerrain() == null ? null : terrainMapper.toHeightGrid(initialScene.getTerrain()));

        SimulationViewModel viewModel = new SimulationViewModel(SimulationState.from(display, ""));
        ScenePresenter presenter = new ScenePresenter(viewModel);
        DropObjectViewModel dropViewModel = new DropObjectViewModel();
        DropObjectController dropController = new DropObjectController(
                new DropObjectInteractor(new BallFactory(),
                        new DropObjectPresenter(dropViewModel, viewModel), scenes));
        app = new ScenePanel(presenter, dropController, dropViewModel);
        SaveSceneController save = new SaveSceneController(new SaveSceneInteractor(files, presenter));
        LoadSceneController load = new LoadSceneController(new LoadSceneInteractor(files, presenter));
        SimulationController simulation = new SimulationController(
                new PlayInteractor(scenes, presenter),
                new StepInteractor(scenes, presenter, new SimplePhysicsEngine()),
                new StopInteractor(scenes, presenter));
        presenter.start(app, save, load, simulation);
    }

    public AppBuilder addSimulationView() {
        app.addSimulationView();
        return this;
    }

    public AppBuilder addSaveSceneUseCase() {
        app.addFileControls();
        return this;
    }

    public AppBuilder addSimulationControlUseCases() {
        app.addSimulationControls();
        return this;
    }

    public JPanel build() {
        return app;
    }
}
