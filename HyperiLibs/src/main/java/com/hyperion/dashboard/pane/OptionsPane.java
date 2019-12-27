package com.hyperion.dashboard.pane;

import com.hyperion.common.Utils;
import com.hyperion.dashboard.UIClient;
import com.hyperion.dashboard.uiobj.DisplaySpline;
import com.hyperion.dashboard.uiobj.Waypoint;
import com.hyperion.motion.math.Pose;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * Displays field options, unimetry, and config
 */

public class OptionsPane extends VBox {

    @SuppressWarnings("unchecked")
    public OptionsPane() {
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

        Button clearOpMode = new Button("Clear Current OpMode");
        clearOpMode.setTextAlignment(TextAlignment.CENTER);
        clearOpMode.setStyle("-fx-font: 14px \"Arial\";");
        clearOpMode.setPrefSize(170, 50);
        clearOpMode.setOnMouseClicked(event -> {
            if (UIClient.waypoints != null) {
                for (Iterator<Waypoint> iter = UIClient.waypoints.iterator(); iter.hasNext();) {
                    Waypoint wp = iter.next();
                    if (wp.id.startsWith(UIClient.opModeID)) {
                        iter.remove();
                    }
                }
            }
            if (UIClient.splines != null) {
                for (Iterator<DisplaySpline> iter = UIClient.splines.iterator(); iter.hasNext();) {
                    DisplaySpline spline = iter.next();
                    if (spline.id.startsWith(UIClient.opModeID)) {
                        iter.remove();
                    }
                }
            }
            UIClient.sendDashboard();
        });
        buttons.getChildren().add(clearOpMode);

        Button clearAllOpModes = new Button("Clear All OpModes");
        clearAllOpModes.setStyle("-fx-font: 14px \"Arial\";");
        clearAllOpModes.setTextAlignment(TextAlignment.CENTER);
        clearAllOpModes.setPrefSize(170, 50);
        clearAllOpModes.setOnMouseClicked(event -> {
            UIClient.waypoints = new ArrayList<>();
            UIClient.splines = new ArrayList<>();
            UIClient.uiClient.emit("dashboardJson", UIClient.writeDashboard());
            Utils.printSocketLog("UI", "SERVER", "dashboardJson", UIClient.options);
        });
        buttons.getChildren().add(clearAllOpModes);

        getChildren().add(buttons);

        ObservableList<String> options =
            FXCollections.observableArrayList(
            "auto.blue.full", "auto.red.full",
                    "auto.blue.foundation", "auto.red.foundation",
                    "auto.blue.brick", "auto.red.brick"
            );
        final ComboBox opModeSelector = new ComboBox(options);
        opModeSelector.valueProperty().setValue("auto.blue.full");
        opModeSelector.setStyle("-fx-font: 24px \"Arial\";");
        opModeSelector.setPrefSize(350, 70);
        opModeSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            UIClient.fieldPane.deselectAll();
            UIClient.opModeID = newValue.toString();
            UIClient.sendDashboard();
        });
        getChildren().add(opModeSelector);

        CheckBox enablePathBuilder = new CheckBox("Preset Path Builder");
        enablePathBuilder.setStyle("-fx-font: 22px \"Arial\";");
        enablePathBuilder.setTextAlignment(TextAlignment.CENTER);
        enablePathBuilder.setTextFill(Color.WHITE);
        enablePathBuilder.setPrefSize(350, 30);
        enablePathBuilder.selectedProperty().addListener((observable, oldValue, newValue) -> UIClient.isBuildingPaths = newValue);
        enablePathBuilder.setSelected(false);
        getChildren().add(enablePathBuilder);
    }

}