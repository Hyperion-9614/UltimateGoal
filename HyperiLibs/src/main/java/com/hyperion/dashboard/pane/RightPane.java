package com.hyperion.dashboard.pane;

import com.hyperion.common.Constants;
import com.hyperion.common.TextUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.net.Message;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
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

    public TextArea metricsDisplay;
    public double width;

    public RightPane() {
        try {
            setBackground(Background.EMPTY);
            setPadding(new Insets(10, 12, 10, 0));
            setSpacing(10);
            setAlignment(Pos.TOP_CENTER);

            width = (Screen.getPrimary().getVisualBounds().getWidth() - Dashboard.fieldPane.fieldSize) / 2.0 - 75;
            if (System.getProperty("os.name").startsWith("Windows")) width += 40;

            Label metricsLabel = new Label("Metrics");
            metricsLabel.setTextFill(Color.WHITE);
            metricsLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            metricsLabel.setPrefWidth(width);
            getChildren().add(metricsLabel);

            metricsDisplay = new TextArea();
            metricsDisplay.setStyle("-fx-font: 14px \"Arial\";");
            metricsDisplay.setPrefSize(width, width + 490);
            metricsDisplay.setEditable(false);
            getChildren().add(metricsDisplay);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMetricsDisplayText() {
        StringBuilder metricsStr = new StringBuilder();

        for (String key : Dashboard.metrics.keySet()) {
            String value = Dashboard.metrics.get(key).trim();
            if (!key.isEmpty() && value.isEmpty()) {
                metricsStr.append(key);
            } else if (!key.isEmpty()) {
                metricsStr.append(key).append(" : ").append(value);
            } else if (value.isEmpty()) {
                metricsStr.append("\n");
            }
            metricsStr.append("\n");
        }

        int caretPosition = metricsDisplay.caretPositionProperty().get();
        double scrollLeft = metricsDisplay.getScrollLeft();
        double scrollTop = metricsDisplay.getScrollTop();
        metricsDisplay.setText(metricsStr.toString().trim());
        metricsDisplay.positionCaret(caretPosition);
        metricsDisplay.setScrollLeft(scrollLeft);
        metricsDisplay.setScrollTop(scrollTop);
    }

}
