package com.hyperion.dashboard.pane;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.net.FieldEdit;
import com.hyperion.dashboard.uiobject.fieldobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.fieldobject.ObstacleObj;
import com.hyperion.dashboard.uiobject.fieldobject.Waypoint;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.pathplanning.Obstacle;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Random;

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

    public double fieldSize;
    public double robotSize;

    private Rectangle fieldBorder;

    public double mouseX, mouseY;
    public long startDragTime;
    public boolean isDragging;

    public Waypoint selectedWP;

    public boolean isObstacleFixed;
    public ArrayList<Obstacle> dynamicObstacles = new ArrayList<>();
    public ArrayList<Obstacle> fixedObstacles = new ArrayList<>();
    public int obstacleSelState;
    public ObstacleObj currObstacle;

    public FieldPane(Stage stage) {
        this.fieldSize = stage.getHeight() - 48;
        this.robotSize = 0.125 * fieldSize;

        setPrefSize(fieldSize, fieldSize);
        addEventHandler(MouseEvent.ANY, this::mouseHandler);

        try {
            Image fieldBG = new Image(Constants.getFile("img", "field.png").toURI().toURL().toString());
            BackgroundImage bgImg = new BackgroundImage(fieldBG,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.DEFAULT, new BackgroundSize(fieldSize, fieldSize, false, false, false, false));
            setBackground(new Background(bgImg));

            fieldBorder = new Rectangle(0, 0, fieldSize, fieldSize);
            fieldBorder.getStrokeDashArray().addAll(20d, 15d);
            fieldBorder.setFill(Color.TRANSPARENT);
            fieldBorder.setStroke(new Color(1, 1, 1, 1));
            fieldBorder.setStrokeWidth(4);
            getChildren().add(fieldBorder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mouseHandler(MouseEvent mouseEvent) {
        try {
            if (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED && mouseEvent.getButton() == MouseButton.PRIMARY && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget().equals(this))) {
                Pose pressedAt = displayToPose(mouseX, mouseY, 0);
                ID obstacleID = new ID("obstacle", (obstacleSelState == 1 ? "rect" : "circle"), (isObstacleFixed ? "" : "un") + "fixed", (int) MathUtils.randInRange(new Random(), 0, 1000000));
                ObstacleObj ob = null;
                if (obstacleSelState == 1)
                    ob = new ObstacleObj.Rect(obstacleID, pressedAt);
                else if (obstacleSelState == 2)
                    ob = new ObstacleObj.Circle(obstacleID, pressedAt);
                if (ob != null)
                    Dashboard.editField(new FieldEdit(ob.id, FieldEdit.Type.CREATE, ob.toJSONObject()));

                isDragging = false;
                startDragTime = System.currentTimeMillis();
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_RELEASED) {
                if (mouseEvent.getButton() == MouseButton.PRIMARY && isDragging && System.currentTimeMillis() - startDragTime > Constants.getInt("dashboard.gui.dragTime") && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane) && (selectedWP != null || obstacleSelState > 0)) {
                    if (obstacleSelState > 0) {
                        Dashboard.editField(new FieldEdit(currObstacle.id, FieldEdit.Type.EDIT_BODY, currObstacle.toJSONObject()));
                        if (isObstacleFixed)
                            fixedObstacles.add(currObstacle.obstacle);
                        else
                            dynamicObstacles.add(currObstacle.obstacle);
                    } else {
                        if (selectedWP.parentSpline != null) {
                            selectedWP.parentSpline.spline.endPath();
                            Dashboard.editField(new FieldEdit(selectedWP.parentSpline.id, FieldEdit.Type.EDIT_BODY, selectedWP.parentSpline.spline.writeJSON()));
                        } else {
                            Dashboard.editField(new FieldEdit(selectedWP.id, FieldEdit.Type.EDIT_BODY, new JSONArray(selectedWP.pose.toArray())));
                        }
                    }
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_MOVED) {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                isDragging = true;
                if (mouseEvent.getButton() == MouseButton.PRIMARY && System.currentTimeMillis() - startDragTime > Constants.getInt("dashboard.gui.dragTime") && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget().equals(this)) && (selectedWP != null || obstacleSelState > 0)) {
                    Pose pose = displayToPose(mouseEvent.getX(), mouseEvent.getY(), 0);
                    if (selectedWP != null) {
                        Vector2D vec = new Vector2D(selectedWP.pose, pose);
                        selectedWP.pose.theta = MathUtils.norm(vec.theta, 0, 2 * Math.PI);
                        if (selectedWP.parentSpline != null) {
                            selectedWP.parentSpline.spline.waypoints.get(selectedWP.parentSpline.waypoints.indexOf(selectedWP)).setPose(selectedWP.pose);
                        }
                        selectedWP.imgView.setRotate(Math.toDegrees(2 * Math.PI - vec.theta));
                        selectedWP.selection.setRotate(selectedWP.imgView.getRotate());
                        selectedWP.info.setText(selectedWP.pose.toString().replace(" | ", "\n"));
                    } else if (obstacleSelState > 0 && currObstacle != null) {
                        currObstacle.end = new Pose(pose);
                        if (currObstacle.obstacle == null) currObstacle.createObstacle();
                        currObstacle.refreshDisplayGroup();
                    }
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
                if (!isDragging) {
                    Pose newPose = displayToPose(mouseX, mouseY, 0);
                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                        if (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane) {
                            select(null);
                        }
                    } else if (mouseEvent.getButton() == MouseButton.SECONDARY && mouseEvent.getTarget().equals(fieldBorder)) {
                        if (fieldBorder.contains(mouseX - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0, mouseY - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0)) {
                            if (Dashboard.isBuildingPaths) {
                                if (selectedWP != null && selectedWP.parentSpline != null) {
                                    selectedWP.parentSpline.spline.waypoints.add(new RigidBody(newPose));
                                    selectedWP.parentSpline.spline.endPath();
                                    selectedWP.id.set(5, selectedWP.parentSpline.spline.waypoints.size() - 1);
                                    Dashboard.editField(new FieldEdit(selectedWP.parentSpline.id, FieldEdit.Type.EDIT_BODY, selectedWP.parentSpline.spline.writeJSON()));
                                } else {
                                    DisplaySpline newSpline = new DisplaySpline(newPose);
                                    Dashboard.editField(new FieldEdit(newSpline.id, FieldEdit.Type.CREATE, newSpline.spline.writeJSON()));
                                }
                            } else {
                                Waypoint newWP = new Waypoint(new ID(Dashboard.opModeID, "waypoint", " "), newPose, null, true, true);
                                Dashboard.editField(new FieldEdit(newWP.id, FieldEdit.Type.CREATE, new JSONArray(newWP.pose.toArray())));
                            }
                        }
                    } else if (mouseEvent.getButton() == MouseButton.MIDDLE && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane)) {
                        Dashboard.editField(new FieldEdit(new ID("pathPoint", Dashboard.numPathPoints), FieldEdit.Type.CREATE, new JSONArray(newPose.toArray())));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Converts pose to view coords
    public double[] poseToDisplay(Pose original, double size) {
        double x1 = (original.x / (Constants.getDouble("localization.fieldSideLength") / 2)) * (fieldSize / 2.0);
        double y1 = (original.y / (Constants.getDouble("localization.fieldSideLength")  / 2)) * (fieldSize / 2.0);
        double x2 = x1 + (fieldSize / 2.0);
        double y2 = y1 + (fieldSize / 2.0);
        double x3 = x2;
        double y3 = fieldSize - y2;

        return new double[]{ x3 - size / 2.0, y3 - size / 2.0, Math.toDegrees(MathUtils.norm(2 * Math.PI - original.theta, 0, 2 * Math.PI))};
    }

    // Converts display xy to pose
    public Pose displayToPose(double x, double y, double size) {
        double x1 = x + size / 2.0;
        double y1 = fieldSize - (y + size / 2.0);
        double x2 = x1 - (fieldSize / 2.0);
        double y2 = y1 - (fieldSize / 2.0);
        double x3 = (x2 / (fieldSize / 2.0)) * (Constants.getDouble("localization.fieldSideLength") / 2);
        double y3 = (y2 / (fieldSize / 2.0)) * (Constants.getDouble("localization.fieldSideLength") / 2);

        return new Pose(Math.round(x3 * 1000.0) / 1000.0, Math.round(y3 * 1000.0) / 1000.0);
    }

    // Converts view to pose
    public Pose viewToPose(ImageView view, double size) {
        Pose toReturn = displayToPose(view.getLayoutX(), view.getLayoutY(), size);
        toReturn.theta = MathUtils.norm(2 * Math.PI - Math.toRadians(view.getRotate()), 0, 2 * Math.PI);
        return toReturn;
    }

    // Get intersection with WBBs
    public boolean[] getWBBIntersects(Rectangle rect, Rectangle rectX, Rectangle rectY) {
        boolean[] intersects = new boolean[]{true, true};
        if (rect.getX() <= fieldBorder.getX() || rect.getX() + rect.getWidth() >= fieldBorder.getX() + fieldBorder.getWidth()) {
            intersects[0] = false;
        }
        if (rect.getY() <= fieldBorder.getY() || rect.getY() + rect.getHeight() >= fieldBorder.getY() + fieldBorder.getHeight()) {
            intersects[1] = false;
        }
        return intersects;
    }

    // Deselect previous and select new selected
    public void select(Waypoint newSelected) {
        if (selectedWP != newSelected) {
            if (selectedWP != null) {
                if (selectedWP.renderID)
                    selectedWP.idField.setText(selectedWP.id.get(-1));
                selectedWP.displayGroup.getChildren().remove(selectedWP.selection);
                requestFocus();
                if (selectedWP.parentSpline != null) {
                    selectedWP.parentSpline.displayGroup.getChildren().remove(selectedWP.parentSpline.selection);
                }
                selectedWP.info.setVisible(false);
            }

            selectedWP = newSelected;
            if (selectedWP != null) {
                selectedWP.displayGroup.getChildren().add(selectedWP.selection);
                selectedWP.selection.toBack();
                if (selectedWP.parentSpline != null) {
                    selectedWP.parentSpline.displayGroup.getChildren().add(selectedWP.parentSpline.selection);
                    selectedWP.parentSpline.selection.toBack();
                    Dashboard.visualPane.updateGraphs(selectedWP.parentSpline);
                }
                selectedWP.info.setVisible(true);
            }
        }
    }

}