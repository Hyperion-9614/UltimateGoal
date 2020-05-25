package com.hyperion.dashboard.pane;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.UIMain;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.dashboard.uiobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.FieldObject;
import com.hyperion.dashboard.uiobject.Waypoint;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;

import org.json.JSONArray;

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

    private Stage stage;
    public double fieldSize;
    public double robotSize;

    private Rectangle wbbBorder;
    private Rectangle wbbLeftLeg;
    private Rectangle wbbRightLeg;

    public double mouseX, mouseY;
    public long startDragTime;
    public boolean isDragging;

    public FieldPane(Stage stage) {
        this.stage = stage;
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

            wbbBorder = new Rectangle(robotSize / 2 - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0, robotSize / 2 - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0, fieldSize - robotSize + Constants.getDouble("dashboard.gui.sizes.waypoint"), fieldSize - robotSize + Constants.getDouble("dashboard.gui.sizes.waypoint"));
            wbbBorder.getStrokeDashArray().addAll(20d, 15d);
            wbbBorder.setFill(Color.TRANSPARENT);
            wbbBorder.setStroke(new Color(Constants.getDouble("dashboard.gui.wbb.grayScale"), Constants.getDouble("dashboard.gui.wbb.grayScale"), Constants.getDouble("dashboard.gui.wbb.grayScale"), 0.7));
            wbbBorder.setStrokeWidth(Constants.getDouble("dashboard.gui.wbb.strokeWidth"));
            getChildren().add(wbbBorder);

            wbbLeftLeg = new Rectangle(12 * fieldSize / 36 - robotSize / 2.0 + Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0, 16 * fieldSize / 36 - robotSize / 2.0 + Constants.getDouble("dashboard.gui.sizes.waypoint") / 6.0, robotSize + fieldSize / 36 - 7.0 * Constants.getDouble("dashboard.gui.sizes.waypoint") / 8.0, robotSize + 4 * fieldSize / 36 - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0);
            wbbLeftLeg.getStrokeDashArray().addAll(20d, 15d);
            wbbLeftLeg.setFill(Color.TRANSPARENT);
            wbbLeftLeg.setStroke(new Color(Constants.getDouble("dashboard.gui.wbb.grayScale"), Constants.getDouble("dashboard.gui.wbb.grayScale"), Constants.getDouble("dashboard.gui.wbb.grayScale"), 0.7));
            wbbLeftLeg.setStrokeWidth(Constants.getDouble("dashboard.gui.wbb.strokeWidth"));
            getChildren().add(wbbLeftLeg);

            wbbRightLeg = new Rectangle(23 * fieldSize / 36 - robotSize / 2.0 + Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0, 16 * fieldSize / 36 - robotSize / 2.0 + Constants.getDouble("dashboard.gui.sizes.waypoint") / 6.0, robotSize + fieldSize / 36 - 7.0 * Constants.getDouble("dashboard.gui.sizes.waypoint") / 8.0, robotSize + 4 * fieldSize / 36 - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0);
            wbbRightLeg.getStrokeDashArray().addAll(20d, 15d);
            wbbRightLeg.setFill(Color.TRANSPARENT);
            wbbRightLeg.setStroke(new Color(Constants.getDouble("dashboard.gui.wbb.grayScale"), Constants.getDouble("dashboard.gui.wbb.grayScale"), Constants.getDouble("dashboard.gui.wbb.grayScale"), 0.7));
            wbbRightLeg.setStrokeWidth(Constants.getDouble("dashboard.gui.wbb.strokeWidth"));
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
                if (mouseEvent.getButton() == MouseButton.PRIMARY && isDragging && System.currentTimeMillis() - startDragTime > 200 && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane) && UIMain.selectedWaypoint != null) {
                    if (UIMain.selectedWaypoint.parentSpline != null) {
                        UIMain.queueFieldEdits(new FieldEdit(UIMain.selectedSpline.id, FieldEdit.Type.EDIT_BODY, UIMain.selectedSpline.spline.writeJSON().toString()));
                    } else {
                        UIMain.queueFieldEdits(new FieldEdit(UIMain.selectedWaypoint.id, FieldEdit.Type.EDIT_BODY, new JSONArray(UIMain.selectedWaypoint.pose.toArray()).toString()));
                    }
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_MOVED) {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                isDragging = true;
                if (mouseEvent.getButton() == MouseButton.PRIMARY && System.currentTimeMillis() - startDragTime > 200 && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget().equals(this)) && UIMain.selectedWaypoint != null) {
                    Vector2D vec = new Vector2D(UIMain.selectedWaypoint.pose, displayToPose(mouseEvent.getX(), mouseEvent.getY(), 0));
                    UIMain.selectedWaypoint.pose.theta = MathUtils.norm(vec.theta, 0, 2 * Math.PI);
                    if (UIMain.selectedWaypoint.parentSpline != null) {
                        UIMain.selectedWaypoint.parentSpline.spline.waypoints.get(UIMain.selectedWaypoint.parentSpline.waypoints.indexOf(UIMain.selectedWaypoint)).setPose(UIMain.selectedWaypoint.pose);
                    }
                    UIMain.selectedWaypoint.imgView.setRotate(Math.toDegrees(2 * Math.PI - vec.theta));
                    UIMain.selectedWaypoint.selectRect.setRotate(UIMain.selectedWaypoint.imgView.getRotate());
                    UIMain.selectedWaypoint.info.setText(UIMain.selectedWaypoint.pose.toString().replace(" | ", "\n"));
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
                if (!isDragging) {
                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                        if (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane) {
                            deselectAll();
                        }
                    } else if (mouseEvent.getButton() == MouseButton.SECONDARY && mouseEvent.getTarget().equals(wbbBorder)) {
                        if (isInWBB(mouseX - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0, mouseY - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0, Constants.getDouble("dashboard.gui.sizes.waypoint"))) {
                            Pose newPose = displayToPose(mouseX, mouseY, 0);
                            if (UIMain.isBuildingPaths) {
                                if (UIMain.selectedSpline != null) {
                                    UIMain.selectedSpline.spline.waypoints.add(new RigidBody(newPose));
                                    int i = UIMain.selectedWaypoint != null ? UIMain.selectedSpline.waypoints.indexOf(UIMain.selectedWaypoint) : -1;
                                    UIMain.selectedSpline.refreshDisplayGroup();
                                    if (i >= 0) {
                                        UIMain.selectedSpline.waypoints.get(i).select();
                                    }
                                    UIMain.queueFieldEdits(new FieldEdit(UIMain.selectedSpline.id, FieldEdit.Type.EDIT_BODY, UIMain.selectedSpline.spline.writeJSON().toString()));
                                } else {
                                    DisplaySpline newSpline = new DisplaySpline(newPose);
                                    UIMain.fieldObjects.add(newSpline);
                                    newSpline.addDisplayGroup();
                                    newSpline.waypoints.get(0).select();
                                    UIMain.queueFieldEdits(new FieldEdit(newSpline.id, FieldEdit.Type.CREATE, newSpline.spline.writeJSON().toString()));
                                }
                            } else {
                                deselectAll();
                                Waypoint newWP = new Waypoint(UIMain.opModeID + ".waypoint.", newPose, null, true);
                                UIMain.fieldObjects.add(newWP);
                                newWP.addDisplayGroup();
                                UIMain.queueFieldEdits(new FieldEdit(newWP.id, FieldEdit.Type.CREATE, new JSONArray(newWP.pose.toArray()).toString()));
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
        UIMain.visualPane.updateGraphs(null);
        UIMain.selectedWaypoint = null;
        UIMain.selectedSpline = null;
        for (FieldObject obj : UIMain.fieldObjects) {
            obj.deselect();
        }
    }

}