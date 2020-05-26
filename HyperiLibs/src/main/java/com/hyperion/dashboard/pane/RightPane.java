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
    public TextArea constantsDisplay;
    public double width;

    public RightPane() {
        try {
            setBackground(Background.EMPTY);
            setPadding(new Insets(10, 7, 10, 0));
            setSpacing(10);
            setAlignment(Pos.TOP_CENTER);

            width = (Screen.getPrimary().getVisualBounds().getWidth() - Dashboard.fieldPane.fieldSize) / 2.0 - 62;
            if (System.getProperty("os.name").startsWith("Windows")) width += 40;

            Label metricsLabel = new Label("Metrics");
            metricsLabel.setTextFill(Color.WHITE);
            metricsLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            metricsLabel.setPrefWidth(width);
            getChildren().add(metricsLabel);

            metricsDisplay = new TextArea();
            metricsDisplay.setStyle("-fx-font: 14px \"Arial\";");
            metricsDisplay.setPrefSize(width, width);
            metricsDisplay.setEditable(false);
            getChildren().add(metricsDisplay);

            Label constantsLabel = new Label("Constants");
            constantsLabel.setTextFill(Color.WHITE);
            constantsLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            constantsLabel.setPrefWidth(width);
            getChildren().add(constantsLabel);

            constantsDisplay = new TextArea();
            constantsDisplay.setStyle("-fx-font: 14px \"Arial\";");
            constantsDisplay.setPrefSize(width, width);
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
