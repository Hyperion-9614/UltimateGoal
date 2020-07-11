package com.hyperion.dashboard.pane;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.TextUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.dashboard.net.Message;
import com.hyperion.dashboard.uiobject.FieldObject;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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

    public TextArea constantsDisplay;
    public double width;

    @SuppressWarnings("unchecked")
    public LeftPane() {
        try {
            setBackground(Background.EMPTY);
            setPadding(new Insets(10, 0, 10, 0));
            setSpacing(10);
            setAlignment(Pos.TOP_CENTER);

            width = (Screen.getPrimary().getVisualBounds().getWidth() - Dashboard.fieldPane.fieldSize) / 2.0 - 75;
            if (System.getProperty("os.name").startsWith("Windows")) width += 40;

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
            clearOpMode.setPrefSize(width / 2.0, 50);
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
            clearAllOpModes.setPrefSize(width / 2.0, 50);
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
            final ComboBox<String> opModeSelector = new ComboBox<>(options);
            opModeSelector.valueProperty().setValue("auto.blue.full");
            opModeSelector.setStyle("-fx-font: 24px \"Arial\";");
            opModeSelector.setPrefSize(width + 10, 91);
            opModeSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
                Dashboard.fieldPane.select(null);
                Dashboard.opModeID = new ID(newValue);
                for (FieldObject o : Dashboard.fieldObjects) {
                    if (o.id.contains(Dashboard.opModeID)) {
                        o.addDisplayGroup();
                    } else {
                        o.removeDisplayGroup();
                    }
                }
            });
            getChildren().add(opModeSelector);

            Label constantsLabel = new Label("Constants");
            constantsLabel.setTextFill(Color.WHITE);
            constantsLabel.setStyle("-fx-font: 32px \"Arial\"; -fx-alignment:center;");
            constantsLabel.setPrefWidth(width);
            getChildren().add(constantsLabel);

            constantsDisplay = new TextArea();
            constantsDisplay.setStyle("-fx-font: 14px \"Arial\";");
            constantsDisplay.setPrefSize(width, width + 282);
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