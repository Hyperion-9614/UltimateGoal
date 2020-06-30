package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.dashboard.uiobject.FieldObject;

import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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

    @SuppressWarnings("unchecked")
    public LeftPane() {
        try {
            setBackground(Background.EMPTY);
            setPadding(new Insets(10, 0, 10, 0));
            setSpacing(10);
            setAlignment(Pos.TOP_CENTER);

            double halfWidth = (Screen.getPrimary().getBounds().getWidth() - Dashboard.fieldPane.fieldSize) / 4.0 - 24;

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
            clearOpMode.setPrefSize(halfWidth, 50);
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
            clearAllOpModes.setStyle("-fx-font: 14px \"Arial\";");
            clearAllOpModes.setTextAlignment(TextAlignment.CENTER);
            clearAllOpModes.setPrefSize(halfWidth, 50);
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
            final ComboBox opModeSelector = new ComboBox(options);
            opModeSelector.valueProperty().setValue("auto.blue.full");
            opModeSelector.setStyle("-fx-font: 24px \"Arial\";");
            opModeSelector.setPrefSize(halfWidth * 2 + 10, 91);
            opModeSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
                Dashboard.fieldPane.select(null);
                Dashboard.opModeID = newValue.toString();
                for (FieldObject o : Dashboard.fieldObjects) {
                    if (o.id.contains(Dashboard.opModeID)) {
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