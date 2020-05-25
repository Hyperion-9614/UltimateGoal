package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.UIMain;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.dashboard.uiobject.FieldObject;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Contains field controls, options, and config
 */

public class LeftPane extends VBox {

    @SuppressWarnings("unchecked")
    public LeftPane() {
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

            HBox buttons = new HBox();
            buttons.setSpacing(10);

            Button clearOpMode = new Button("Clear Current\nOpMode");
            clearOpMode.setTextAlignment(TextAlignment.CENTER);
            clearOpMode.setStyle("-fx-font: 14px \"Arial\";");
            clearOpMode.setPrefSize(150, 50);
            clearOpMode.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    ArrayList<FieldEdit> toRemove = new ArrayList<>();
                    for (FieldObject o : UIMain.fieldObjects) {
                        if (o.id.contains(UIMain.opModeID)) {
                            toRemove.add(new FieldEdit(o.id, FieldEdit.Type.DELETE, "{}"));
                        }
                    }
                    UIMain.queueFieldEdits(toRemove.toArray(new FieldEdit[0]));
                }
            });
            buttons.getChildren().add(clearOpMode);

            Button clearAllOpModes = new Button("Clear All\nOpModes");
            clearAllOpModes.setStyle("-fx-font: 14px \"Arial\";");
            clearAllOpModes.setTextAlignment(TextAlignment.CENTER);
            clearAllOpModes.setPrefSize(150, 50);
            clearAllOpModes.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    ArrayList<FieldEdit> toRemove = new ArrayList<>();
                    for (FieldObject o : UIMain.fieldObjects) {
                        toRemove.add(new FieldEdit(o.id, FieldEdit.Type.DELETE, "{}"));
                    }
                    UIMain.queueFieldEdits(toRemove.toArray(new FieldEdit[0]));
                }
            });
            buttons.getChildren().add(clearAllOpModes);

            getChildren().add(buttons);

            CheckBox enablePathBuilder = new CheckBox("Preset Path Builder");
            enablePathBuilder.setStyle("-fx-font: 22px \"Arial\";");
            enablePathBuilder.setTextAlignment(TextAlignment.CENTER);
            enablePathBuilder.setTextFill(Color.WHITE);
            enablePathBuilder.setPrefSize(310, 30);
            enablePathBuilder.selectedProperty().addListener((observable, oldValue, newValue) -> UIMain.isBuildingPaths = newValue);
            enablePathBuilder.setSelected(false);
            getChildren().add(enablePathBuilder);

            CheckBox simulationMode = new CheckBox("Simulation Mode");
            simulationMode.setStyle("-fx-font: 22px \"Arial\";");
            simulationMode.setTextAlignment(TextAlignment.CENTER);
            simulationMode.setTextFill(Color.WHITE);
            simulationMode.setPrefSize(310, 30);
            simulationMode.selectedProperty().addListener((observable, oldValue, newValue) -> UIMain.isSimulating = newValue);
            simulationMode.setSelected(false);
            getChildren().add(simulationMode);

            ObservableList<String> options =
                    FXCollections.observableArrayList(
                            "auto.blue.full", "auto.red.full",
                            "auto.blue.foundation", "auto.red.foundation",
                            "auto.blue.brick", "auto.red.brick",
                            "tele.blue", "tele.red"
                    );
            final ComboBox opModeSelector = new ComboBox(options);
            opModeSelector.valueProperty().setValue("auto.blue.full");
            opModeSelector.setStyle("-fx-font: 24px \"Arial\";");
            opModeSelector.setPrefSize(310, 91);
            opModeSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
                UIMain.fieldPane.deselectAll();
                UIMain.opModeID = newValue.toString();
                for (FieldObject o : UIMain.fieldObjects) {
                    if (o.id.contains(UIMain.opModeID)) {
                        o.addDisplayGroup();
                    } else {
                        o.removeDisplayGroup();
                    }
                }
            });
            getChildren().add(opModeSelector);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}