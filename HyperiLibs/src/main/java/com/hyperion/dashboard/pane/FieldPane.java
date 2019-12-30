package com.hyperion.dashboard.pane;

import com.hyperion.common.Utils;
import com.hyperion.dashboard.UIClient;
import com.hyperion.dashboard.uiobj.DisplaySpline;
import com.hyperion.dashboard.uiobj.Waypoint;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;

import java.io.File;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Displays field UI
 */

public class FieldPane extends Pane {

    public Stage stage;
    public double fieldSize;
    public double robotSize;

    public Rectangle wbbBorder;
    public Rectangle wbbLeftLeg;
    public Rectangle wbbRightLeg;

    public double mouseX, mouseY;
    private long startDragTime;
    public boolean isDragging;

    public FieldPane(Stage stage) {
        this.stage = stage;
        this.fieldSize = stage.getHeight() - 48;
        this.robotSize = 0.125 * fieldSize;

        setPrefSize(fieldSize, fieldSize);
        addEventHandler(MouseEvent.ANY, this::mouseHandler);

        try {
            Image fieldBG = new Image(new File(UIClient.constants.RES_IMG_PREFIX + "/field.png").toURI().toURL().toString());
            BackgroundImage bgImg = new BackgroundImage(fieldBG,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.DEFAULT, new BackgroundSize(fieldSize, fieldSize, false, false, false, false));
            setBackground(new Background(bgImg));

            wbbBorder = new Rectangle(robotSize / 2 - UIClient.constants.WAYPOINT_SIZE / 2.0, robotSize / 2 - UIClient.constants.WAYPOINT_SIZE / 2.0, fieldSize - robotSize + UIClient.constants.WAYPOINT_SIZE, fieldSize - robotSize + UIClient.constants.WAYPOINT_SIZE);
            wbbBorder.getStrokeDashArray().addAll(20d, 15d);
            wbbBorder.setFill(Color.TRANSPARENT);
            wbbBorder.setStroke(new Color(UIClient.constants.WBB_GRAY_SCALE, UIClient.constants.WBB_GRAY_SCALE, UIClient.constants.WBB_GRAY_SCALE, 0.7));
            wbbBorder.setStrokeWidth(UIClient.constants.WBB_STROKE_WIDTH);
            getChildren().add(wbbBorder);

            wbbLeftLeg = new Rectangle(12 * fieldSize / 36 - robotSize / 2.0 + UIClient.constants.WAYPOINT_SIZE / 2.0, 16 * fieldSize / 36 - robotSize / 2.0 + UIClient.constants.WAYPOINT_SIZE / 6.0, robotSize + fieldSize / 36 - 7.0 * UIClient.constants.WAYPOINT_SIZE / 8.0, robotSize + 4 * fieldSize / 36 - UIClient.constants.WAYPOINT_SIZE / 2.0);
            wbbLeftLeg.getStrokeDashArray().addAll(20d, 15d);
            wbbLeftLeg.setFill(Color.TRANSPARENT);
            wbbLeftLeg.setStroke(new Color(UIClient.constants.WBB_GRAY_SCALE, UIClient.constants.WBB_GRAY_SCALE, UIClient.constants.WBB_GRAY_SCALE, 0.7));
            wbbLeftLeg.setStrokeWidth(UIClient.constants.WBB_STROKE_WIDTH);
            getChildren().add(wbbLeftLeg);

            wbbRightLeg = new Rectangle(23 * fieldSize / 36 - robotSize / 2.0 + UIClient.constants.WAYPOINT_SIZE / 2.0, 16 * fieldSize / 36 - robotSize / 2.0 + UIClient.constants.WAYPOINT_SIZE / 6.0, robotSize + fieldSize / 36 - 7.0 * UIClient.constants.WAYPOINT_SIZE / 8.0, robotSize + 4 * fieldSize / 36 - UIClient.constants.WAYPOINT_SIZE / 2.0);
            wbbRightLeg.getStrokeDashArray().addAll(20d, 15d);
            wbbRightLeg.setFill(Color.TRANSPARENT);
            wbbRightLeg.setStroke(new Color(UIClient.constants.WBB_GRAY_SCALE, UIClient.constants.WBB_GRAY_SCALE, UIClient.constants.WBB_GRAY_SCALE, 0.7));
            wbbRightLeg.setStrokeWidth(UIClient.constants.WBB_STROKE_WIDTH);
            getChildren().add(wbbRightLeg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mouseHandler(MouseEvent mouseEvent) {
        try {
            if (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED) {
                isDragging = false;
                startDragTime = System.currentTimeMillis();
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_RELEASED) {
                if (mouseEvent.getButton() == MouseButton.PRIMARY && isDragging && System.currentTimeMillis() - startDragTime > 200 && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane)) {
                    UIClient.sendDashboard();
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_MOVED) {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                isDragging = true;
                if (mouseEvent.getButton() == MouseButton.PRIMARY && System.currentTimeMillis() - startDragTime > 200 && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane) && UIClient.selectedWaypoint != null) {
                    Vector2D vec = new Vector2D(UIClient.selectedWaypoint.pose, displayToPose(mouseEvent.getX(), mouseEvent.getY(), 0));
                    UIClient.selectedWaypoint.pose.theta = Utils.normalizeTheta(vec.theta, 0, 2 * Math.PI);
                    if (UIClient.selectedWaypoint.parentSpline != null) {
                        UIClient.selectedWaypoint.parentSpline.spline.waypoints.get(UIClient.selectedWaypoint.parentSpline.waypoints.indexOf(UIClient.selectedWaypoint)).pose = UIClient.selectedWaypoint.pose;
                    }
                    UIClient.selectedWaypoint.imgView.setRotate(Math.toDegrees(2 * Math.PI - vec.theta));
                    UIClient.selectedWaypoint.selectRect.setRotate(UIClient.selectedWaypoint.imgView.getRotate());
                    UIClient.selectedWaypoint.info.setText(UIClient.selectedWaypoint.pose.toString().replace(" | ", "\n"));
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
                if (!isDragging) {
                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                        if (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane) {
                            deselectAll();
                        }
                    } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                        if (mouseEvent.getTarget() instanceof Rectangle && ((Rectangle) mouseEvent.getTarget()).getWidth() == wbbBorder.getWidth()) {
                            if (isInWBB(mouseX - UIClient.constants.WAYPOINT_SIZE / 2.0, mouseY - UIClient.constants.WAYPOINT_SIZE / 2.0, UIClient.constants.WAYPOINT_SIZE)) {
                                Pose newPose = displayToPose(mouseX, mouseY, 0);
                                Waypoint newWP = new Waypoint(UIClient.opModeID + ".waypoint.", newPose, UIClient.constants, null, true);
                                if (UIClient.isBuildingPaths) {
                                    if (UIClient.selectedSpline != null) {
                                        UIClient.selectedSpline.spline.waypoints.add(new RigidBody(newWP.pose));
                                        int i = UIClient.selectedWaypoint != null ? UIClient.selectedSpline.waypoints.indexOf(UIClient.selectedWaypoint) : -1;
                                        UIClient.selectedSpline.refreshDisplayGroup();
                                        if (i >= 0) {
                                            UIClient.selectedSpline.waypoints.get(i).select();
                                        }
                                    } else {
                                        DisplaySpline newSpline = new DisplaySpline(newWP.pose, UIClient.constants);
                                        newSpline.addDisplayGroup();
                                        newSpline.select();
                                        newSpline.waypoints.get(0).select();
                                        UIClient.splines.add(newSpline);
                                    }
                                } else {
                                    deselectAll();
                                    UIClient.waypoints.add(newWP);
                                    newWP.addDisplayGroup();
                                    newWP.select();
                                }
                                UIClient.sendDashboard();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Converts pose to view coords
    public double[] poseToDisplay(Pose original, double size) {
        double x1 = (original.x / (UIClient.constants.COORD_AXIS_LENGTH_UNITS / 2)) * (fieldSize / 2.0);
        double y1 = (original.y / (UIClient.constants.COORD_AXIS_LENGTH_UNITS / 2)) * (fieldSize / 2.0);
        double x2 = x1 + (fieldSize / 2.0);
        double y2 = y1 + (fieldSize / 2.0);
        double x3 = x2;
        double y3 = fieldSize - y2;

        return new double[]{ x3 - size / 2.0, y3 - size / 2.0, Math.toDegrees(Utils.normalizeTheta(2 * Math.PI - original.theta, 0, 2 * Math.PI))};
    }

    // Converts display xy to pose
    public Pose displayToPose(double x, double y, double size) {
        double x1 = x + size / 2.0;
        double y1 = fieldSize - (y + size / 2.0);
        double x2 = x1 - (fieldSize / 2.0);
        double y2 = y1 - (fieldSize / 2.0);
        double x3 = (x2 / (fieldSize / 2.0)) * (UIClient.constants.COORD_AXIS_LENGTH_UNITS / 2);
        double y3 = (y2 / (fieldSize / 2.0)) * (UIClient.constants.COORD_AXIS_LENGTH_UNITS / 2);

        return new Pose(Math.round(x3 * 1000.0) / 1000.0, Math.round(y3 * 1000.0) / 1000.0);
    }

    // Converts view to pose
    public Pose viewToPose(ImageView view, double size) {
        Pose toReturn = displayToPose(view.getLayoutX(), view.getLayoutY(), size);
        toReturn.theta = Utils.normalizeTheta(2 * Math.PI - Math.toRadians(view.getRotate()), 0, 2 * Math.PI);
        return toReturn;
    }

    public boolean isInWBB(double x, double y, double size) {
        return (wbbBorder.contains(x, y) && wbbBorder.contains(x, y) &&
                !wbbLeftLeg.contains(x, y) && !wbbLeftLeg.contains(x, y) &&
                !wbbRightLeg.contains(x, y) && !wbbRightLeg.contains(x, y));
    }

    // Get intersection with WBBs
    public boolean[] getWBBIntersects(Rectangle rect, Rectangle rectX, Rectangle rectY) {
        boolean[] intersects = new boolean[]{ true, true };
        if (rect.getX() <= wbbBorder.getX() || rect.getX() + rect.getWidth() >= wbbBorder.getX() + wbbBorder.getWidth()) {
            intersects[0] = false;
        }
        if (rect.getY() <= wbbBorder.getY() || rect.getY() + rect.getHeight() >= wbbBorder.getY() + wbbBorder.getHeight()) {
            intersects[1] = false;
        }
        for (int i = 0; i < 2; i++) {
            Rectangle wbbLeg = wbbRightLeg;
            if (i == 0) wbbLeg = wbbLeftLeg;
            if (rectX.intersects(wbbLeg.getLayoutBounds()) && (rectX.getX() + rectX.getWidth() <= wbbLeg.getX() + wbbLeg.getWidth() || rectX.getX() + rectX.getWidth() >= wbbLeg.getX())) {
                intersects[0] = false;
            }
            if (rectY.intersects(wbbLeg.getLayoutBounds()) && (rectY.getY() + rectY.getHeight() <= wbbLeg.getY() + wbbLeg.getHeight() || rectY.getY() + rectY.getHeight() >= wbbLeg.getY())) {
                intersects[1] = false;
            }
        }
        return intersects;
    }

    public void deselectAll() {
        UIClient.displayPane.updateGraphs(null);
        UIClient.selectedWaypoint = null;
        for (Waypoint wp : UIClient.waypoints) {
            wp.deselect();
        }
        UIClient.selectedSpline = null;
        for (DisplaySpline spline : UIClient.splines) {
            spline.deselect();
        }
    }

}