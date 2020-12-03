package com.hyperion.dashboard.pane;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.TextUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.net.FieldEdit;
import com.hyperion.net.Message;
import com.hyperion.dashboard.uiobject.Simulator;
import com.hyperion.dashboard.uiobject.fieldobject.FieldObject;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.pathplanning.DStarLite;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;

/**
 * Contains field controls, options, and config
 */

public class LeftPane extends VBox {

    public TextArea constantsDisplay;
    public Timer constantsSaveTimer = new Timer();
    private long mostRecentSaveTime;

    public TextField simText;
    public Button simulate;
    public CheckBox showPathingGrid;
    public Spinner<Double> errorMagSpinner;
    public Spinner<Integer> errorProbSpinner;

    public double width;

    public LeftPane() {
        try {
            setBackground(Background.EMPTY);
            setPadding(new Insets(10, 0, 10, 0));
            setAlignment(Pos.TOP_CENTER);
            setSpacing(10);

            width = (Screen.getPrimary().getVisualBounds().getWidth() - Dashboard.fieldPane.fieldSize) / 2.0 - 35;

            Label fieldOptionsLabel = new Label("Field Options");
            fieldOptionsLabel.setTextFill(Color.YELLOW);
            fieldOptionsLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            fieldOptionsLabel.setPrefWidth(getPrefWidth());
            getChildren().add(fieldOptionsLabel);

            HBox buttons = new HBox();
            buttons.setSpacing(10);

            Button clearOpMode = new Button("Clear Current\nOpMode");
            clearOpMode.setTextAlignment(TextAlignment.CENTER);
            clearOpMode.setStyle("-fx-font: 14px \"Arial\"; -fx-focus-color: transparent;");
            clearOpMode.setPrefSize(width / 2.0, 50);
            clearOpMode.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    for (FieldObject o : new ArrayList<>(Dashboard.fieldObjects)) {
                        if (o.id.contains(Dashboard.opModeID)) {
                            Dashboard.editField(new FieldEdit(o.id, FieldEdit.Type.DELETE, "{}"));
                        }
                    }
                }
            });
            buttons.getChildren().add(clearOpMode);

            Button clearAllOpModes = new Button("Clear All\nOpModes");
            clearAllOpModes.setStyle("-fx-font: 14px \"Arial\"; -fx-focus-color: transparent;");
            clearAllOpModes.setTextAlignment(TextAlignment.CENTER);
            clearAllOpModes.setPrefSize(width / 2.0, 50);
            clearAllOpModes.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    for (FieldObject o : new ArrayList<>(Dashboard.fieldObjects)) {
                        Dashboard.editField(new FieldEdit(o.id, FieldEdit.Type.DELETE, "{}"));
                    }
                }
            });
            buttons.getChildren().add(clearAllOpModes);

            getChildren().add(buttons);

            FlowPane pGridOptions = new FlowPane();
            pGridOptions.setHgap(10);
            pGridOptions.setPrefWrapLength(width);

            showPathingGrid = new CheckBox("Show Pathing Grid");
            showPathingGrid.setPrefWidth(2 * width / 3 - 1);
            showPathingGrid.setTextFill(Color.WHITE);
            showPathingGrid.setStyle("-fx-font: 20px \"Arial\"; -fx-focus-color: transparent;");
            showPathingGrid.setTextAlignment(TextAlignment.LEFT);
            showPathingGrid.setOnMouseClicked(event -> {
                for (FieldObject o : Dashboard.fieldObjects) {
                    if (o.id.contains("pathPoint")) {
                        if (showPathingGrid.isSelected()) o.addDisplayGroup();
                        else o.removeDisplayGroup();
                    }
                }
            });
            pGridOptions.getChildren().add(showPathingGrid);

            Button resetPathingGrid = new Button("Reset Pathing Grid");
            resetPathingGrid.setStyle("-fx-font: 14px \"Arial\"; -fx-focus-color: transparent;");
            resetPathingGrid.setTextAlignment(TextAlignment.CENTER);
            resetPathingGrid.setPrefSize(width / 3, 30);
            resetPathingGrid.setOnMouseClicked(event -> resetPathingGrid());
            pGridOptions.getChildren().add(resetPathingGrid);

            getChildren().add(pGridOptions);

            FlowPane obstacles = new FlowPane();
            obstacles.setHgap(10);
            obstacles.setPrefWrapLength(width);

            Label obstacleLabel = new Label("Obstacles:");
            obstacleLabel.setTextFill(Color.WHITE);
            obstacleLabel.setStyle("-fx-font: 20px \"Arial\";");
            obstacles.getChildren().add(obstacleLabel);

            CheckBox fixed = new CheckBox("Fixed");
            fixed.setTextFill(Color.WHITE);
            fixed.setStyle("-fx-font: 20px \"Arial\"; -fx-focus-color: transparent;");
            fixed.setTextAlignment(TextAlignment.LEFT);
            fixed.setOnMouseClicked(event -> Dashboard.fieldPane.isObstacleFixed = fixed.isSelected());
            obstacles.getChildren().add(fixed);

            CheckBox rect = new CheckBox("Rects");
            CheckBox circle = new CheckBox("Circles");

            rect.setTextFill(Color.WHITE);
            rect.setStyle("-fx-font: 20px \"Arial\"; -fx-focus-color: transparent;");
            rect.setTextAlignment(TextAlignment.LEFT);
            rect.setOnMouseClicked(event -> {
                Dashboard.fieldPane.obstacleSelState = rect.isSelected() ? 1 : 0;
                circle.setSelected(false);
            });
            obstacles.getChildren().add(rect);

            circle.setTextFill(Color.WHITE);
            circle.setStyle("-fx-font: 20px \"Arial\"; -fx-focus-color: transparent;");
            circle.setTextAlignment(TextAlignment.LEFT);
            circle.setOnMouseClicked(event -> {
                Dashboard.fieldPane.obstacleSelState = circle.isSelected() ? 2 : 0;
                rect.setSelected(false);
            });
            obstacles.getChildren().add(circle);

            getChildren().add(obstacles);

            ObservableList<String> options =
                FXCollections.observableArrayList(
             "auto.blue.full", "auto.red.full",
                    "auto.blue.foundation", "auto.red.foundation",
                    "auto.blue.brick", "auto.red.brick",
                    "tele.blue", "tele.red"
                );
            final ComboBox<String> opModeSelector = new ComboBox<>(options);
            opModeSelector.valueProperty().setValue(options.get(0));
            opModeSelector.setStyle("-fx-font: 24px \"Arial\"; -fx-focus-color: transparent;");
            opModeSelector.setPrefSize(width + 10, 60);
            opModeSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
                Dashboard.fieldPane.select(null);
                Dashboard.opModeID = new ID(newValue);
                for (FieldObject o : Dashboard.fieldObjects) {
                    if (o.id.contains(Dashboard.opModeID)) {
                        o.addDisplayGroup();
                    } else {
                        o.removeDisplayGroup();
                    }
                }
            });
            getChildren().add(opModeSelector);

            Label simulationLabel = new Label("Simulation Options");
            simulationLabel.setTextFill(Color.YELLOW);
            simulationLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            simulationLabel.setPrefWidth(width);
            getChildren().add(simulationLabel);

            FlowPane simulateOptions = new FlowPane();
            simulateOptions.setHgap(10);
            simulateOptions.setPrefWrapLength(width);

            simText = new TextField();
            simText.setStyle("-fx-font: 16px \"Arial\"; -fx-text-alignment:left; -fx-focus-color: transparent;");
            simText.setEditable(false);
            simText.setPrefSize(3.0 * width / 4.0, 40);
            simulateOptions.getChildren().add(simText);

            simulate = new Button("Select\nSimulants");
            simulate.setStyle("-fx-font: 14px \"Arial\"; -fx-text-alignment:center; -fx-focus-color: transparent;");
            simulate.setTextAlignment(TextAlignment.CENTER);
            simulate.setPrefSize(width / 4.0, 40);
            simulate.setOnMouseClicked(event -> {
                switch (Dashboard.simulator.state) {
                    case INACTIVE:
                        Dashboard.simulator.state = Simulator.State.SELECTING;
                        simulate.setText("Simulate");
                        break;
                    case SELECTING:
                        if (Dashboard.simulator.simulants[1] != null) {
                            Dashboard.simulator.simulate();
                        }
                        break;
                    case SIMULATING:
                        Dashboard.simulator.state = Simulator.State.INACTIVE;
                        simText.setText("");
                        while (Dashboard.simulator.simulationThread != null && Dashboard.simulator.simulationThread.isAlive()) {}
                        break;
                }
            });
            simulateOptions.getChildren().add(simulate);

            getChildren().add(simulateOptions);

            CheckBox simDynPath = new CheckBox("Simulate Dynamic Pathing");
            simDynPath.setPrefWidth(width);
            simDynPath.setTextFill(Color.WHITE);
            simDynPath.setStyle("-fx-font: 20px \"Arial\"; -fx-focus-color: transparent;");
            simDynPath.setTextAlignment(TextAlignment.LEFT);
            simDynPath.setOnMouseClicked(event -> Dashboard.simulator.isSimDynPath = simDynPath.isSelected());
            getChildren().add(simDynPath);

            FlowPane errorOptions = new FlowPane();
            errorOptions.setPrefWrapLength(width);

            Label magLabel = new Label(" Error Magnitude: ");
            magLabel.setTextFill(Color.WHITE);
            magLabel.setStyle("-fx-font: 20px \"Arial\"; -fx-alignment:center;");
            errorOptions.getChildren().add(magLabel);

            errorMagSpinner = new Spinner<>(0, 1, 0.5, 0.05);
            errorMagSpinner.setStyle("-fx-font: 15px \"Arial\"; -fx-text-alignment:left; -fx-focus-color: transparent;");
            errorMagSpinner.setEditable(true);
            errorMagSpinner.setPrefWidth(70);
            errorOptions.getChildren().add(errorMagSpinner);

            Label probLabel = new Label(" Probability: ");
            probLabel.setTextFill(Color.WHITE);
            probLabel.setStyle("-fx-font: 20px \"Arial\"; -fx-alignment:center;");
            errorOptions.getChildren().add(probLabel);

            errorProbSpinner = new Spinner<>(0, 100, 35, 5);
            errorProbSpinner.setStyle("-fx-font: 15px \"Arial\"; -fx-text-alignment:left; -fx-focus-color: transparent;");
            errorProbSpinner.setEditable(true);
            errorProbSpinner.setPrefWidth(80);
            errorOptions.getChildren().add(errorProbSpinner);

            getChildren().add(errorOptions);

            CheckBox simPID = new CheckBox("Simulate PID Correction");
            simPID.setPrefWidth(width);
            simPID.setTextFill(Color.WHITE);
            simPID.setStyle("-fx-font: 20px \"Arial\"; -fx-focus-color: transparent;");
            simPID.setTextAlignment(TextAlignment.LEFT);
            simPID.setOnMouseClicked(event -> {
                Dashboard.simulator.isSimPID = simPID.isSelected();
            });
            simPID.setSelected(true);
            getChildren().add(simPID);

            Label constantsLabel = new Label("Constants");
            constantsLabel.setTextFill(Color.YELLOW);
            constantsLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            constantsLabel.setPrefWidth(width);
            getChildren().add(constantsLabel);

            constantsDisplay = new TextArea();
            constantsDisplay.setStyle("-fx-font: 14px \"Arial\"; -fx-focus-color: transparent;");
            constantsDisplay.setPrefSize(width, width + 135);
            constantsDisplay.setEditable(true);
            setConstantsDisplayText(Constants.root.toString(4));
            Dashboard.constantsSave = Constants.root.toString(4);
            constantsDisplay.textProperty().addListener((observable, oldValue, newValue) -> {
                if (TextUtils.condensedEquals(newValue, Dashboard.constantsSave)) {
                    constantsLabel.setText("Constants");
                } else {
                    constantsLabel.setText("Constants (*)");
                    mostRecentSaveTime = System.currentTimeMillis() + Constants.getLong("dashboard.gui.constantsAutoSaveDelay");
                    constantsSaveTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (System.currentTimeMillis() >= mostRecentSaveTime) {
                                Dashboard.constantsSave = constantsDisplay.getText();
                                Constants.init(new JSONObject(Dashboard.constantsSave));
                                Constants.write();
                                if (Dashboard.dbSocket != null)
                                    Dashboard.dbSocket.sendMessage(Message.Event.CONSTANTS_UPDATED, Dashboard.constantsSave);
                                Platform.runLater(() -> constantsLabel.setText("Constants"));
                            }
                        }
                    }, Constants.getLong("dashboard.gui.constantsAutoSaveDelay"));
                }
            });
            getChildren().add(constantsDisplay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setConstantsDisplayText(String text) {
        int caretPosition = constantsDisplay.caretPositionProperty().get();
        double scrollLeft = constantsDisplay.getScrollLeft();
        double scrollTop = constantsDisplay.getScrollTop();
        constantsDisplay.setText(text);
        constantsDisplay.positionCaret(caretPosition);
        constantsDisplay.setScrollLeft(scrollLeft);
        constantsDisplay.setScrollTop(scrollTop);
    }

    public void resetPathingGrid() {
        for (FieldObject o : new ArrayList<>(Dashboard.fieldObjects)) {
            if (o.id.contains("pathPoint")) {
                Dashboard.editField(false, new FieldEdit(o.id, FieldEdit.Type.DELETE, "{}"));
            }
        }

        Pose[][] pathingGrid = DStarLite.generatePathingGrid();
        for (int r = 0; r < pathingGrid.length; r++) {
            for (int c = 0; c < pathingGrid[0].length; c++) {
                Dashboard.editField(false, new FieldEdit(new ID("pathPoint", r * pathingGrid.length + c), FieldEdit.Type.CREATE, pathingGrid[r][c].toJSONArray()));
            }
        }
    }

}