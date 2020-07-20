package com.hyperion.dashboard.pane;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.TextUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.dashboard.net.Message;
import com.hyperion.dashboard.uiobject.FieldObject;
import com.hyperion.dashboard.uiobject.Simulator;

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
    public TextField simText;
    public Button simulate;
    public Spinner<Double> errorSpinner;

    public double width;

    public LeftPane() {
        try {
            setBackground(Background.EMPTY);
            setPadding(new Insets(10, 0, 10, 0));
            setSpacing(10);
            setAlignment(Pos.TOP_CENTER);

            width = (Screen.getPrimary().getVisualBounds().getWidth() - Dashboard.fieldPane.fieldSize) / 2.0 - 75;
            if (System.getProperty("os.name").startsWith("Windows")) width += 40;

            Label fieldOptionsLabel = new Label("Field Options");
            fieldOptionsLabel.setTextFill(Color.WHITE);
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
            simulationLabel.setTextFill(Color.WHITE);
            simulationLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            simulationLabel.setPrefWidth(width);
            getChildren().add(simulationLabel);

            HBox simulateHBox = new HBox(10);

            simText = new TextField();
            simText.setStyle("-fx-font: 16px \"Arial\"; -fx-text-alignment:left; -fx-focus-color: transparent;");
            simText.setEditable(false);
            simText.setPrefSize(3.0 * width / 4.0, 40);
            simulateHBox.getChildren().add(simText);

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
            simulateHBox.getChildren().add(simulate);

            getChildren().add(simulateHBox);

            CheckBox showGrid = new CheckBox("Show Deterministic Sampling Grid");
            showGrid.setPrefWidth(width);
            showGrid.setTextFill(Color.WHITE);
            showGrid.setStyle("-fx-font: 20px \"Arial\"; -fx-focus-color: transparent;");
            showGrid.setTextAlignment(TextAlignment.LEFT);
            showGrid.setOnMouseClicked(event -> Dashboard.fieldPane.deterministicSamplingGrid.setVisible(showGrid.isSelected()));
            getChildren().add(showGrid);

            CheckBox simDynPath = new CheckBox("Simulate Dynamic Pathing");
            simDynPath.setPrefWidth(width);
            simDynPath.setTextFill(Color.WHITE);
            simDynPath.setStyle("-fx-font: 20px \"Arial\"; -fx-focus-color: transparent;");
            simDynPath.setTextAlignment(TextAlignment.LEFT);
            simDynPath.setOnMouseClicked(event -> Dashboard.simulator.isSimDynPath = simDynPath.isSelected());
            getChildren().add(simDynPath);

            HBox errorHBox = new HBox();

            Label errorLabel = new Label(" Error Magnitude: ");
            errorLabel.setTextFill(Color.WHITE);
            errorLabel.setStyle("-fx-font: 20px \"Arial\"; -fx-alignment:center;");
            errorHBox.getChildren().add(errorLabel);

            errorSpinner = new Spinner<>(0, 1, 0.5, 0.05);
            errorSpinner.setStyle("-fx-font: 15px \"Arial\"; -fx-text-alignment:left; -fx-focus-color: transparent;");
            errorSpinner.setEditable(true);
            errorSpinner.setPrefWidth(width / 8.0);
            errorHBox.getChildren().add(errorSpinner);

            getChildren().add(errorHBox);

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
            constantsLabel.setTextFill(Color.WHITE);
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
                }
            });
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    String newConstants = constantsDisplay.getText();
                    if (!TextUtils.condensedEquals(newConstants, Dashboard.constantsSave)) {
                        Dashboard.constantsSave = newConstants;
                        Constants.init(new JSONObject(Dashboard.constantsSave));
                        Constants.write();
                        if (Dashboard.btServer != null)
                            Dashboard.btServer.sendMessage(Message.Event.CONSTANTS_UPDATED, Dashboard.constantsSave);
                    }
                    Platform.runLater(() -> constantsLabel.setText("Constants"));
                }
            }, 1000, 3000);
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

}