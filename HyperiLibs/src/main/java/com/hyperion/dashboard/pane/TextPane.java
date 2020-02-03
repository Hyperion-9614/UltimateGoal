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
            Thread updateConstantsThread = new Thread(() -> {
                try {
                    long start = System.currentTimeMillis();
                    String save = constantsDisplay.getText();
                    while (true) {
                        if (System.currentTimeMillis() - start >= 2000) {
                            String newStr = constantsDisplay.getText();
                            if (!newStr.equals(save)) {
                                UIClient.constants.read(new JSONObject(newStr));
                                UIClient.sendConstants();
                                UIClient.constants.write();
                                save = newStr;
                            }
                            start = System.currentTimeMillis();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            updateConstantsThread.start();
            getChildren().add(constantsDisplay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUnimetryDisplayText() {
        StringBuilder unimetryStr = new StringBuilder();

        for (String key : UIClient.unimetry.keySet()) {
            String value = UIClient.unimetry.get(key).trim();
            if (!key.isEmpty() && value.isEmpty()) {
                unimetryStr.append(key);
            } else if (!key.isEmpty()) {
                unimetryStr.append(key).append(" : ").append(value);
            } else if (value.isEmpty()) {
                unimetryStr.append("\n");
            }
            unimetryStr.append("\n");
        }

        int caretPosition = unimetryDisplay.caretPositionProperty().get();
        double scrollLeft = unimetryDisplay.getScrollLeft();
        double scrollTop = unimetryDisplay.getScrollTop();
        unimetryDisplay.setText(unimetryStr.toString().trim());
        unimetryDisplay.positionCaret(caretPosition);
        unimetryDisplay.setScrollLeft(scrollLeft);
        unimetryDisplay.setScrollTop(scrollTop);
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
