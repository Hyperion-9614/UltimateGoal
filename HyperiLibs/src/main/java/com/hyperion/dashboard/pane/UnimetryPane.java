package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.UIClient;
import com.hyperion.motion.math.Piecewise;

import java.util.ArrayList;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

/**
 * Displays graphs & camera view
 */

public class UnimetryPane extends VBox {

    public TextArea unimetryDisplay;
    public double width;

    public UnimetryPane() {
        setBackground(Background.EMPTY);
        setPadding(new Insets(10, 7, 10, 0));
        setSpacing(10);
        setAlignment(Pos.TOP_CENTER);
        width = (Screen.getPrimary().getVisualBounds().getWidth() - UIClient.fieldPane.fieldSize) / 2.0 - 62;

        Label unimetryLabel = new Label("Telemetry");
        unimetryLabel.setTextFill(Color.WHITE);
        unimetryLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
        unimetryLabel.setPrefWidth(width);
        getChildren().add(unimetryLabel);

        unimetryDisplay = new TextArea(UIClient.unimetry);
        unimetryDisplay.setStyle("-fx-font: 14px \"Arial\";");
        unimetryDisplay.setPrefSize(width, width);
        unimetryDisplay.setEditable(false);
        getChildren().add(unimetryDisplay);
    }

}
