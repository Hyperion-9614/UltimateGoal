package com.hyperion.dashboard.pane;

import com.hyperion.common.Constants;
import com.hyperion.dashboard.Dashboard;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MenuPane extends BorderPane {

    public double xOffset, yOffset;
    public Label title;

    public MenuPane(Stage stage) {
        setBackground(new Background(new BackgroundFill(new Color(1.0, 1.0, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)));
        setMinWidth(stage.getWidth());

        title = new Label("Hyperion Dashboard v" + Constants.getString("dashboard.version"));
        title.setFont(new Font(14));
        setMargin(title, new Insets(4, 0, 0, 5));
        setLeft(title);

        try {
            HBox buttons = new HBox();
            Button iconify = new Button();
            iconify.setBackground(Background.EMPTY);
            iconify.setOnMouseEntered(e -> iconify.setBackground(new Background(new BackgroundFill(new Color(0.5, 0.5, 0.5, 1.0), CornerRadii.EMPTY, Insets.EMPTY))));
            iconify.setOnMouseExited(e -> iconify.setBackground(new Background(new BackgroundFill(new Color(1.0, 1.0, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY))));
            ImageView iconifyIcon = new ImageView(Constants.getFile("img", "iconify.png").toURI().toURL().toString());
            iconifyIcon.setFitWidth(20);
            iconifyIcon.setFitHeight(20);
            iconify.setGraphic(iconifyIcon);
            iconify.setOnMouseClicked(event -> stage.setIconified(!stage.isIconified()));
            buttons.getChildren().add(iconify);

            Button close = new Button();
            close.setBackground(Background.EMPTY);
            close.setOnMouseEntered(e -> close.setBackground(new Background(new BackgroundFill(new Color(0.5, 0.5, 0.5, 1.0), CornerRadii.EMPTY, Insets.EMPTY))));
            close.setOnMouseExited(e -> close.setBackground(new Background(new BackgroundFill(new Color(1.0, 1.0, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY))));
            ImageView closeIcon = new ImageView(Constants.getFile("img", "close.png").toURI().toURL().toString());
            closeIcon.setFitWidth(20);
            closeIcon.setFitHeight(20);
            close.setGraphic(closeIcon);
            close.setOnMouseClicked(event -> {
                if (Dashboard.dbSocket.isValid())
                    Dashboard.dbSocket.close();
                System.exit(0);
            });
            buttons.getChildren().add(close);
            setRight(buttons);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setOnMousePressed(event -> {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });
        setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(Math.max(event.getScreenY() + yOffset, 0));
        });
        setOnMouseReleased(event -> {
            if (stage.getY() == 0) {
                stage.setX(0);
            }
        });
    }

}