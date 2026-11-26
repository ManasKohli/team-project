package use_case.drop_object;

import entity.Ball;
import entity.BallFactory;
import entity.Scene;
import use_case.scene.SceneViewData;

import java.util.Objects;

public final class DropObjectInteractor implements DropObjectInputBoundary {

    private final DropObjectOutputBoundary outputBoundary;
    private final DropObjectDataAccessInterface dataAccess;
    private final BallFactory ballFactory;

    public DropObjectInteractor(BallFactory ballFactory,
                                DropObjectOutputBoundary outputBoundary,
                                DropObjectDataAccessInterface dataAccess) {
        this.ballFactory = Objects.requireNonNull(ballFactory, "ballFactory");
        this.outputBoundary = Objects.requireNonNull(outputBoundary, "outputBoundary");
        this.dataAccess = Objects.requireNonNull(dataAccess, "dataAccess");
    }

    @Override
    public void execute(DropObjectInputData inputData) {
        Objects.requireNonNull(inputData, "inputData");
        if (inputData.getPreset() == null) {
            throw new IllegalArgumentException("A ball preset must be selected");
        }
        if (inputData.getDropSpeed() < 0) {
            throw new IllegalArgumentException("dropSpeed must be non-negative");
        }

        Scene scene = Objects.requireNonNull(dataAccess.getCurrentScene(), "current scene");
        double x = inputData.getX();
        double y = inputData.getY();

        if (scene.getTerrain() == null || !dataAccess.isInsideTerrain(x, y)) {
            outputBoundary.presentDropResult(new DropObjectOutputData(false, null));
            return;
        }

        double groundHeight = dataAccess.getTerrainHeightAt(x, y);
        Ball ball = ballFactory.createDroppedBall(
                inputData.getPreset(), x, y, groundHeight, inputData.getDropSpeed());
        scene.addBall(ball);
        dataAccess.saveCurrentScene(scene);
        outputBoundary.presentDropResult(new DropObjectOutputData(true, SceneViewData.from(scene)));
    }
}
