package com.hyperion.dashboard.uiobj;

import com.hyperion.common.Constants;
import com.hyperion.dashboard.UIClient;
import com.hyperion.motion.math.Pose;
import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;

import java.io.File;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class Waypoint {

    public Constants constants;
    public String id;
    public Pose pose;
    public boolean renderID;
    public DisplaySpline parentSpline;

    public Group displayGroup;
    public ImageView imgView;
    public TextField idField;
    public Text info;
    public Rectangle selectRect;
    public Color selectColor;

    private long startDragTime;
    private double dragDx, dragDy;

    public Waypoint(String id, double x, double y, double theta, Constants constants, DisplaySpline parentSpline, boolean renderID) {
        this.constants = constants;
        this.id = id;
        this.pose = new Pose(x, y, theta);
        this.renderID = renderID;
        this.parentSpline = parentSpline;
        createDisplayGroup();
    }

    public Waypoint(String id, Pose pose, Constants constants, DisplaySpline parentSpline, boolean renderID) {
        this(id, pose.x, pose.y, pose.theta, constants, parentSpline, renderID);
    }

    public void createDisplayGroup() {
        try {
            displayGroup = new Group();

            imgView = new ImageView(new File(UIClient.constants.RES_IMG_PREFIX + "/waypoint.png").toURI().toURL().toString());
            imgView.setFitWidth(constants.WAYPOINT_SIZE);
            imgView.setFitHeight(constants.WAYPOINT_SIZE);
            displayGroup.getChildren().add(imgView);

            if (renderID) {
                idField = new TextField(id.replace(UIClient.opModeID + ".waypoint.", ""));
                idField.setStyle("-fx-background-radius: 0; -fx-text-inner-color: rgba(255, 255, 255, 1.0); -fx-background-color: rgba(0, 0, 0, 0.6);");
                idField.setMinWidth(17);
                idField.setMaxWidth(150);

                FontMetrics metrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(idField.getFont());
                idField.setPrefWidth(metrics.computeStringWidth(idField.getText()) + 17);
                idField.textProperty().addListener((observable, oldTextString, newTextString) ->
                    idField.setPrefWidth(metrics.computeStringWidth(newTextString) + 17)
                );
                idField.setTextFormatter(new TextFormatter<>(change -> {
                    if (!change.isContentChange()) {
                        return change;
                    }
                    String txt = change.getControlNewText();
                    if (txt.contains(".")) return null;
                    return change;
                }));

                if (parentSpline == null) {
                    idField.setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.ENTER) {
                            this.id = UIClient.opModeID + ".waypoint." + idField.getText();
                            UIClient.sendDashboard();
                        }
                    });
                }
                idField.setOnMousePressed((event -> select()));
                displayGroup.getChildren().add(idField);
            }

            info = new Text(pose.toString());
            info.setFill(Color.WHITE);
            info.setVisible(false);
            displayGroup.getChildren().add(info);

            selectColor = Color.hsb(360 * Math.random(), 1.0, 1.0);
            selectRect = new Rectangle(UIClient.fieldPane.robotSize, UIClient.fieldPane.robotSize);
            selectRect.setStroke(selectColor);
            selectRect.setStrokeWidth(2);
            selectRect.setFill(new Color(selectColor.getRed(), selectColor.getGreen(), selectColor.getBlue(), 0.3));
            selectRect.setVisible(false);
            displayGroup.getChildren().add(selectRect);

            setListeners();
            refreshDisplayGroup();
            info.toFront();
            if (renderID) {
                idField.toFront();
            }
            imgView.toFront();
            selectRect.toBack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setListeners() {
        displayGroup.setOnMouseClicked((event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                removeDisplayGroup();
                if (parentSpline != null) {
                    parentSpline.spline.waypoints.remove(parentSpline.waypoints.indexOf(this));
                    parentSpline.waypoints.remove(this);
                    if (parentSpline.waypoints.size() == 0) {
                        parentSpline.removeDisplayGroup();
                        UIClient.splines.remove(parentSpline);
                    } else if (parentSpline.waypoints.size() == 1) {
                        parentSpline.waypoints.get(0).select();
                    }
                } else {
                    UIClient.waypoints.remove(this);
                }
                UIClient.sendDashboard();
            }
        }));

        displayGroup.setOnMousePressed((event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                select();
                startDragTime = System.currentTimeMillis();
                dragDx = imgView.getLayoutX() - event.getSceneX();
                dragDy = imgView.getLayoutY() - event.getSceneY();
            }
        }));

        displayGroup.setOnMouseDragged((event -> {
            double newX = event.getSceneX() + dragDx;
            double newY = event.getSceneY() + dragDy;

            Rectangle rect = new Rectangle(newX, newY, UIClient.constants.WAYPOINT_SIZE, UIClient.constants.WAYPOINT_SIZE);
            Rectangle rectX = new Rectangle(newX, imgView.getLayoutY(), UIClient.constants.WAYPOINT_SIZE, UIClient.constants.WAYPOINT_SIZE);
            Rectangle rectY = new Rectangle(imgView.getLayoutX(), newY, UIClient.constants.WAYPOINT_SIZE, UIClient.constants.WAYPOINT_SIZE);
            boolean[] intersects = UIClient.fieldPane.getWBBIntersects(rect, rectX, rectY);
            if (intersects[0]) imgView.setLayoutX(newX);
            if (intersects[1]) imgView.setLayoutY(newY);
            pose = UIClient.fieldPane.viewToPose(imgView, UIClient.constants.WAYPOINT_SIZE);
            refreshDisplayGroup();
        }));

        displayGroup.setOnMouseReleased((event -> {
            if (System.currentTimeMillis() - startDragTime > 200) {
                if (parentSpline != null) {
                    parentSpline.spline.waypoints.get(parentSpline.waypoints.indexOf(this)).pose = pose;
                }
                UIClient.sendDashboard();
            }
        }));
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> {
            if (parentSpline != null && parentSpline.id.startsWith(UIClient.opModeID)) {
                parentSpline.displayGroup.getChildren().add(displayGroup);
            } else if (id.startsWith(UIClient.opModeID)) {
                UIClient.fieldPane.getChildren().add(displayGroup);
            }
        });
    }

    public void refreshDisplayGroup() {
        double[] poseToDisplay = UIClient.fieldPane.poseToDisplay(pose, constants.WAYPOINT_SIZE);
        imgView.relocate(poseToDisplay[0], poseToDisplay[1]);
        imgView.setRotate(poseToDisplay[2]);
        if (renderID) {
            idField.relocate(poseToDisplay[0] + constants.WAYPOINT_SIZE + 3, poseToDisplay[1] - 24);
        }
        info.setText(pose.toString().replace(" | ", "\n"));
        info.relocate(poseToDisplay[0] + constants.WAYPOINT_SIZE + 3, poseToDisplay[1] + constants.WAYPOINT_SIZE - 21);
        selectRect.relocate(poseToDisplay[0] + constants.WAYPOINT_SIZE / 2.0 - UIClient.fieldPane.robotSize / 2.0,
                            poseToDisplay[1] + constants.WAYPOINT_SIZE / 2.0 - UIClient.fieldPane.robotSize / 2.0);
        selectRect.setRotate(poseToDisplay[2]);
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> {
            if (parentSpline != null) {
                parentSpline.displayGroup.getChildren().remove(displayGroup);
            } else {
                UIClient.fieldPane.getChildren().remove(displayGroup);
            }
        });
    }

    public void select() {
        if (parentSpline != null) {
            for (Waypoint wp : parentSpline.waypoints) {
                if (wp != this) {
                    wp.deselect();
                }
            }
        }
        for (Waypoint wp : UIClient.waypoints) {
            if (wp != this) {
                wp.deselect();
            }
        }
        if (parentSpline != null) {
            parentSpline.select();
            parentSpline.selectedWaypointIndex = parentSpline.waypoints.indexOf(this);
        }
        UIClient.selectedWaypoint = this;
        info.setVisible(true);
        selectRect.setVisible(true);
    }

    public void deselect() {
        if (UIClient.selectedWaypoint == this) UIClient.selectedWaypoint = null;
        info.setVisible(false);
        selectRect.setVisible(false);
    }

    @Override
    public String toString() {
        return id + " " + pose;
    }

}
