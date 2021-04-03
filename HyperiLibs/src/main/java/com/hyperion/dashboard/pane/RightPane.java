package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.Dashboard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

/**
 * Contains unimetry and constants
 */

public class RightPane extends VBox {

    public TextArea telemetryDisplay;
    public double width;

    public RightPane() {
        try {
            setBackground(Background.EMPTY);
            setPadding(new Insets(10, 10, 10, 0));
            setSpacing(10);
            setAlignment(Pos.TOP_CENTER);

            width = (Screen.getPrimary().getVisualBounds().getWidth() - Dashboard.fieldPane.fieldSize) / 2.0 - 35;

            Label telemetryLabel = new Label("Telemetry");
            telemetryLabel.setTextFill(Color.YELLOW);
            telemetryLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            telemetryLabel.setPrefWidth(width);
            getChildren().add(telemetryLabel);

            telemetryDisplay = new TextArea();
            telemetryDisplay.setStyle("-fx-font: 14px \"Arial\"; -fx-focus-color: transparent;");
            telemetryDisplay.setPrefSize(width, width + 655);
            telemetryDisplay.setEditable(false);
            getChildren().add(telemetryDisplay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTelemetryDisplayText() {
        StringBuilder telemetryStr = new StringBuilder();

        for (String key : Dashboard.telemetry.keySet()) {
            String value = Dashboard.telemetry.get(key).trim();
            if (!key.isEmpty() && value.isEmpty()) {
                telemetryStr.append(key);
            } else if (!key.isEmpty()) {
                telemetryStr.append(key).append(" : ").append(value);
            } else if (value.isEmpty()) {
                telemetryStr.append("\n");
            }
            telemetryStr.append("\n");
        }

        int caretPosition = telemetryDisplay.caretPositionProperty().get();
        double scrollLeft = telemetryDisplay.getScrollLeft();
        double scrollTop = telemetryDisplay.getScrollTop();
        telemetryDisplay.setText(telemetryStr.toString().trim());
        telemetryDisplay.positionCaret(caretPosition);
        telemetryDisplay.setScrollLeft(scrollLeft);
        telemetryDisplay.setScrollTop(scrollTop);
    }

}
