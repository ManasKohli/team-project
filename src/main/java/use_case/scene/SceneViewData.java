package use_case.scene;

import entity.Ball;
import entity.Scene;
import entity.Vector3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable values to display after a scene is loaded or updated. */
public final class SceneViewData {
    private final HeightGrid terrain;
    private final List<BallData> balls;

    private SceneViewData(HeightGrid terrain, List<BallData> balls) {
        this.terrain = terrain;
        this.balls = List.copyOf(balls);
    }

    public static SceneViewData from(Scene scene, HeightGrid grid) {
        List<BallData> balls = new ArrayList<>();
        for (Ball ball : scene.getBalls()) {
            balls.add(new BallData(ball));
        }
        return new SceneViewData(grid, balls);
    }

    /** Builds display-only scene values directly from the current domain scene. */
    public static SceneViewData from(Scene scene) {
        HeightGrid grid = null;
        if (scene.getTerrain() != null) {
            grid = new HeightGrid(scene.getTerrain().getName(), scene.getTerrain().getCellSize(),
                    scene.getTerrain().getHeights());
        }
        return from(scene, grid);
    }

    public HeightGrid getTerrain() {
        return terrain;
    }

    public List<BallData> getBalls() {
        return balls;
    }

    public static final class HeightGrid {
        private final String name;
        private final double cellSize;
        private final double[][] heights;

        public HeightGrid(String name, double cellSize, double[][] heights) {
            this.name = Objects.requireNonNull(name, "name");
            if (!Double.isFinite(cellSize) || cellSize <= 0) {
                throw new IllegalArgumentException("Cell size must be positive and finite");
            }
            if (heights == null || heights.length < 2 || heights[0] == null || heights[0].length < 2) {
                throw new IllegalArgumentException("The height grid needs at least two rows and columns");
            }

            this.cellSize = cellSize;
            this.heights = new double[heights.length][];
            int columns = heights[0].length;
            for (int row = 0; row < heights.length; row++) {
                if (heights[row] == null || heights[row].length != columns) {
                    throw new IllegalArgumentException("Height grid rows must have the same length");
                }
                this.heights[row] = heights[row].clone();
                for (double height : this.heights[row]) {
                    if (!Double.isFinite(height)) {
                        throw new IllegalArgumentException("Heights must be finite");
                    }
                }
            }
        }

        public String getName() {
            return name;
        }

        public double getCellSize() {
            return cellSize;
        }

        public int getRows() {
            return heights.length;
        }

        public int getCols() {
            return heights[0].length;
        }

        public double getHeight(int row, int col) {
            return heights[row][col];
        }
    }

    public static final class BallData {
        private final double x;
        private final double y;
        private final double z;
        private final double radius;
        private final int colour;

        private BallData(Ball ball) {
            Vector3D position = ball.getPosition();
            x = position.getX();
            y = position.getY();
            z = position.getZ();
            radius = ball.getPreset().getRadius();
            colour = ball.getPreset().getColour();
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public double getRadius() {
            return radius;
        }

        public int getColour() {
            return colour;
        }
    }
}
