package view;

import interface_adapter.simulation.CameraPose;
import interface_adapter.simulation.ScenePresenter;
import interface_adapter.simulation.SceneView;
import interface_adapter.simulation.SimulationState;
import interface_adapter.drop_object.DropObjectController;
import interface_adapter.drop_object.DropObjectViewModel;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;

/** Swing implementation of the passive scene view. */
public final class ScenePanel extends JPanel implements SceneView {
    private static final int TICK_MILLIS = 16;

    private final JPanel fileControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 7));
    private final JPanel simulationControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 7));
    private final JButton saveButton = new JButton("Save Scene");
    private final JLabel status = new JLabel();
    private final JFileChooser chooser = new JFileChooser();
    private final TerrainView3D terrain;
    private final DropObjectPanel dropObjects;
    private final Timer clock;

    public ScenePanel(ScenePresenter presenter, DropObjectController dropController,
                      DropObjectViewModel dropViewModel) {
        super(new BorderLayout());
        terrain = new TerrainView3D(presenter);
        dropObjects = new DropObjectPanel(dropController, dropViewModel);
        chooser.setFileFilter(new FileNameExtensionFilter("JSON scene files", "json"));
        JButton loadButton = new JButton("Load Scene");
        saveButton.addActionListener(event -> presenter.saveRequested());
        loadButton.addActionListener(event -> presenter.loadRequested());
        fileControls.add(saveButton);
        fileControls.add(loadButton);
        fileControls.add(status);

        clock = new Timer(TICK_MILLIS, event -> presenter.clockTicked(TICK_MILLIS / 1000.0));
        JButton playButton = new JButton("Play");
        JButton pauseButton = new JButton("Pause");
        JButton stopButton = new JButton("Stop");
        playButton.addActionListener(event -> presenter.playRequested());
        pauseButton.addActionListener(event -> presenter.pauseRequested());
        stopButton.addActionListener(event -> presenter.stopRequested());
        simulationControls.add(playButton);
        simulationControls.add(pauseButton);
        simulationControls.add(stopButton);
    }

    public void addSimulationView() {
        add(terrain, BorderLayout.CENTER);
        add(dropObjects, BorderLayout.EAST);
    }

    public void addFileControls() {
        add(fileControls, BorderLayout.NORTH);
    }

    public void addSimulationControls() {
        add(simulationControls, BorderLayout.SOUTH);
    }

    @Override
    public void startClock() {
        clock.start();
    }

    @Override
    public void stopClock() {
        clock.stop();
    }

    @Override
    public Path chooseSaveFile() {
        return chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }

    @Override
    public Path chooseLoadFile() {
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }

    @Override
    public boolean confirmOverwrite(String fileName) {
        return JOptionPane.showConfirmDialog(this, "Replace " + fileName + "?", "Save Scene",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    @Override
    public void display(SimulationState state, CameraPose camera) {
        status.setText(state.getStatusText());
        saveButton.setEnabled(state.hasTerrain());
        terrain.display(state, camera);
        dropObjects.setSimulationState(state);
    }
}
