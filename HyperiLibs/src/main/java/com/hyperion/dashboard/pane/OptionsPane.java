package com.hyperion.dashboard.pane;

import com.github.underscore.lodash.U;
import com.hyperion.common.Utils;
import com.hyperion.dashboard.UIClient;
import com.hyperion.dashboard.uiobject.FieldEdit;
import com.hyperion.dashboard.uiobject.FieldObject;

import org.json.JSONObject;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Displays field options, unimetry, and config
 */

public class OptionsPane extends VBox {

    public TextArea optionsDisplay;
    public TextArea configDisplay;

    @SuppressWarnings("unchecked")
    public OptionsPane() {
        try {
            setBackground(Background.EMPTY);
            setPadding(new Insets(10, 0, 10, 0));
            setSpacing(10);
            setAlignment(Pos.TOP_CENTER);

            Label optionsLabel = new Label("Options");
            optionsLabel.setTextFill(Color.WHITE);
            optionsLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            optionsLabel.setPrefWidth(getPrefWidth());
            getChildren().add(optionsLabel);

            optionsDisplay = new TextArea();
            optionsDisplay.setStyle("-fx-font: 14px \"Arial\";");
            optionsDisplay.setText(UIClient.options.toJSONObject().toString(4));
            optionsDisplay.setPrefSize(50, 7 * optionsDisplay.getPrefRowCount() + 8);
            optionsDisplay.setEditable(true);
            Thread updateOptionsThread = new Thread(() -> {
                try {
                    long start = System.currentTimeMillis();
                    String save = optionsDisplay.getText();
                    while (true) {
                        if (System.currentTimeMillis() - start >= 2000) {
                            String newStr = optionsDisplay.getText();
                            if (!newStr.equals(save)) {
                                UIClient.options.read(new JSONObject(newStr));
                                Utils.writeDataJSON(optionsDisplay.getText(), "options", UIClient.constants);
                                save = newStr;
                            }
                            start = System.currentTimeMillis();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            updateOptionsThread.start();
            getChildren().add(optionsDisplay);

            HBox buttons = new HBox();
            buttons.setSpacing(10);

            Button clearOpMode = new Button("Clear Current\nOpMode");
            clearOpMode.setTextAlignment(TextAlignment.CENTER);
            clearOpMode.setStyle("-fx-font: 14px \"Arial\";");
            clearOpMode.setPrefSize(150, 50);
            clearOpMode.setOnMouseClicked(event -> {
                ArrayList<FieldEdit> toRemove = new ArrayList<>();
                for (FieldObject o : UIClient.fieldObjects) {
                    if (o.id.contains(UIClient.opModeID)) {
                        toRemove.add(new FieldEdit(o.id, FieldEdit.Type.DELETE, "{}"));
                    }
                }
                UIClient.sendFieldEdits((FieldEdit[]) toRemove.toArray());
            });
            buttons.getChildren().add(clearOpMode);

            Button clearAllOpModes = new Button("Clear All\nOpModes");
            clearAllOpModes.setStyle("-fx-font: 14px \"Arial\";");
            clearAllOpModes.setTextAlignment(TextAlignment.CENTER);
            clearAllOpModes.setPrefSize(150, 50);
            clearAllOpModes.setOnMouseClicked(event -> {
                ArrayList<FieldEdit> toRemove = new ArrayList<>();
                for (FieldObject o : UIClient.fieldObjects) {
                    toRemove.add(new FieldEdit(o.id, FieldEdit.Type.DELETE, "{}"));
                }
                UIClient.sendFieldEdits((FieldEdit[]) toRemove.toArray());
            });
            buttons.getChildren().add(clearAllOpModes);

            getChildren().add(buttons);

            CheckBox enablePathBuilder = new CheckBox("Preset Path Builder");
            enablePathBuilder.setStyle("-fx-font: 22px \"Arial\";");
            enablePathBuilder.setTextAlignment(TextAlignment.CENTER);
            enablePathBuilder.setTextFill(Color.WHITE);
            enablePathBuilder.setPrefSize(310, 30);
            enablePathBuilder.selectedProperty().addListener((observable, oldValue, newValue) -> UIClient.isBuildingPaths = newValue);
            enablePathBuilder.setSelected(false);
            getChildren().add(enablePathBuilder);

            CheckBox simulationMode = new CheckBox("Simulation Mode");
            simulationMode.setStyle("-fx-font: 22px \"Arial\";");
            simulationMode.setTextAlignment(TextAlignment.CENTER);
            simulationMode.setTextFill(Color.WHITE);
            simulationMode.setPrefSize(310, 30);
            simulationMode.selectedProperty().addListener((observable, oldValue, newValue) -> UIClient.isSimulating = newValue);
            simulationMode.setSelected(false);
            getChildren().add(simulationMode);

            ObservableList<String> options =
                    FXCollections.observableArrayList(
                            "auto.blue.full", "auto.red.full",
                            "auto.blue.foundation", "auto.red.foundation",
                            "auto.blue.brick", "auto.red.brick",
                            "tele.red", "tele.blue"
                    );
            final ComboBox opModeSelector = new ComboBox(options);
            opModeSelector.valueProperty().setValue("auto.blue.full");
            opModeSelector.setStyle("-fx-font: 24px \"Arial\";");
            opModeSelector.setPrefSize(310, 91);
            opModeSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
                UIClient.fieldPane.deselectAll();
                UIClient.opModeID = newValue.toString();
                for (FieldObject o : UIClient.fieldObjects) {
                    if (o.id.contains(UIClient.opModeID)) {
                        o.addDisplayGroup();
                    } else {
                        o.removeDisplayGroup();
                    }
                }
            });
            getChildren().add(opModeSelector);

            Label configLabel = new Label("Config");
            configLabel.setTextFill(Color.WHITE);
            configLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            configLabel.setPrefWidth(getPrefWidth());
            getChildren().add(configLabel);

            configDisplay = new TextArea();
            configDisplay.setStyle("-fx-font: 14px \"Arial\";");
            configDisplay.setPrefSize(310, 321);
            configDisplay.setEditable(true);
            Thread updateConfigThread = new Thread(() -> {
                try {
                    long start = System.currentTimeMillis();
                    String save = UIClient.config;
                    while (true) {
                        if (System.currentTimeMillis() - start >= 2000) {
                            UIClient.config = configDisplay.getText();
                            if (!UIClient.config.equals(save)) {
                                UIClient.uiClient.emit("configUpdated", UIClient.config);
                                Utils.printSocketLog("UI", "SERVER", "configUpdated", UIClient.options);
                                save = UIClient.config;
                            }
                            start = System.currentTimeMillis();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            updateConfigThread.start();
            getChildren().add(configDisplay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setConfigDisplayText() {
        try {
            int caretPosition = configDisplay.caretPositionProperty().get();
            double scrollLeft = configDisplay.getScrollLeft();
            double scrollTop = configDisplay.getScrollTop();
            configDisplay.setText(U.formatXml(UIClient.config));
            configDisplay.positionCaret(caretPosition);
            configDisplay.setScrollLeft(scrollLeft);
            configDisplay.setScrollTop(scrollTop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}