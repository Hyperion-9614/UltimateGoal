package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.UIClient;

import org.json.JSONObject;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

/**
 * Displays text data
 */

public class TextPane extends VBox {

    public TextArea unimetryDisplay;
    public TextArea constantsDisplay;
    public double width;

    public TextPane() {
        try {
            setBackground(Background.EMPTY);
            setPadding(new Insets(10, 7, 10, 0));
            setSpacing(10);
            setAlignment(Pos.TOP_CENTER);

            width = (Screen.getPrimary().getVisualBounds().getWidth() - UIClient.fieldPane.fieldSize) / 2.0 - 62;
            if (System.getProperty("os.name").startsWith("Windows")) width += 40;

            Label unimetryLabel = new Label("Telemetry");
            unimetryLabel.setTextFill(Color.WHITE);
            unimetryLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            unimetryLabel.setPrefWidth(width);
            getChildren().add(unimetryLabel);

            unimetryDisplay = new TextArea();
            unimetryDisplay.setStyle("-fx-font: 14px \"Arial\";");
            unimetryDisplay.setPrefSize(width, width);
            unimetryDisplay.setEditable(false);
            getChildren().add(unimetryDisplay);

            Label constantsLabel = new Label("Constants");
            constantsLabel.setTextFill(Color.WHITE);
            constantsLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            constantsLabel.setPrefWidth(width);
            getChildren().add(constantsLabel);

            constantsDisplay = new TextArea();
            constantsDisplay.setStyle("-fx-font: 14px \"Arial\";");
            constantsDisplay.setPrefSize(width, width);
            constantsDisplay.setEditable(true);
            constantsDisplay.textProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    if (!newValue.equals(oldValue)) {
                        UIClient.constants.read(new JSONObject(newValue));
                        UIClient.sendConstants();
                        UIClient.constants.write();
                    }
                } catch (Exception ignored) {

                }
            });
            getChildren().add(constantsDisplay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUnimetryDisplayText() {
        StringBuilder unimetryStr = new StringBuilder();

        for (String key : UIClient.unimetry.keySet()) {
            String value = UIClient.unimetry.get(key);
            if (!key.isEmpty() && value.isEmpty()) {
                unimetryStr.append(key);
            } else if (!key.isEmpty()) {
                unimetryStr.append(key).append(" : ").append(value);
            }
            unimetryStr.append("\n");
        }

        unimetryDisplay.setText(unimetryStr.toString().trim());
    }

}
