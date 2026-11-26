package interface_adapter.drop_object;

import java.util.Objects;

import use_case.drop_object.DropObjectOutputBoundary;
import use_case.drop_object.DropObjectOutputData;
import interface_adapter.simulation.SimulationViewModel;

public final class DropObjectPresenter implements DropObjectOutputBoundary {

    private final DropObjectViewModel viewModel;
    private final SimulationViewModel simulationViewModel;

    public DropObjectPresenter(DropObjectViewModel viewModel, SimulationViewModel simulationViewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.simulationViewModel = Objects.requireNonNull(simulationViewModel, "simulationViewModel");
    }

    @Override
    public void presentDropResult(DropObjectOutputData outputData) {
        Objects.requireNonNull(outputData, "outputData");
        viewModel.setLastDropSuccessful(outputData.wasDropped());
        if (outputData.wasDropped()) {
            simulationViewModel.setState(simulationViewModel.getState()
                    .withBalls(outputData.getScene().getBalls(), "Ball dropped"));
        }
    }
}
