package com.hyperion.dashboard.pane;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.dashboard.uiobject.Arrow;
import com.hyperion.dashboard.uiobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.FieldObject;
import com.hyperion.dashboard.uiobject.Robot;
import com.hyperion.dashboard.uiobject.Simulator;
import com.hyperion.dashboard.uiobject.Waypoint;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;

import org.json.JSONArray;

import javafx.application.Platform;
import javafx.scene.Group;
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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Displays field UI
 */

public class FieldPane extends Pane {

    public double fieldSize;
    public double robotSize;

    private Rectangle wbbBorder;
    private Rectangle wbbLeftLeg;
    private Rectangle wbbRightLeg;

    public Group deterministicSamplingGrid;

    public double mouseX, mouseY;
    public long startDragTime;
    public boolean isDragging;

    public Waypoint selectedWP;

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

            deterministicSamplingGrid = new Group();
            double fsl = Constants.getDouble("localization.fieldSideLength");
            double mHat = Constants.getDouble("pathing.gridMhat");
            double buffer = (fsl % mHat) / 2.0;
            int n = (int) MathUtils.round((fsl - 2 * buffer) / mHat, 0);
            for (int r = 0; r <= n; r++) {
                for (int c = 0; c <= n; c++) {
                    double x = -(fsl / 2.0) + buffer + r * mHat;
                    double y = -(fsl / 2.0) + buffer + c * mHat;

                    double[] poseArr = poseToDisplay(new Pose(x, y, 0), 0);
                    Circle gridPoint = new Circle(poseArr[0], poseArr[1], 1.25 * Constants.getDouble("dashboard.gui.sizes.planningPoint"));
                    gridPoint.setFill(Color.BLACK);
                    deterministicSamplingGrid.getChildren().add(gridPoint);
                }
            }
            deterministicSamplingGrid.setVisible(false);
            getChildren().add(deterministicSamplingGrid);
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
                if (mouseEvent.getButton() == MouseButton.PRIMARY && isDragging && System.currentTimeMillis() - startDragTime > Constants.getInt("dashboard.gui.dragTime") && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane) && selectedWP != null) {
                    if (selectedWP.parentSpline != null) {
                        selectedWP.parentSpline.spline.endPath();
                        Dashboard.editField(new FieldEdit(selectedWP.parentSpline.id, FieldEdit.Type.EDIT_BODY, selectedWP.parentSpline.spline.writeJSON()));
                    } else {
                        Dashboard.editField(new FieldEdit(selectedWP.id, FieldEdit.Type.EDIT_BODY, new JSONArray(selectedWP.pose.toArray())));
                    }
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_MOVED) {
                mouseX = mouseEvent.getX();
                mouseY = mouseEvent.getY();
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                isDragging = true;
                if (mouseEvent.getButton() == MouseButton.PRIMARY && System.currentTimeMillis() - startDragTime > Constants.getInt("dashboard.gui.dragTime") && (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget().equals(this)) && selectedWP != null) {
                    Vector2D vec = new Vector2D(selectedWP.pose, displayToPose(mouseEvent.getX(), mouseEvent.getY(), 0));
                    selectedWP.pose.theta = MathUtils.norm(vec.theta, 0, 2 * Math.PI);
                    if (selectedWP.parentSpline != null) {
                        selectedWP.parentSpline.spline.waypoints.get(selectedWP.parentSpline.waypoints.indexOf(selectedWP)).setPose(selectedWP.pose);
                    }
                    selectedWP.imgView.setRotate(Math.toDegrees(2 * Math.PI - vec.theta));
                    selectedWP.selection.setRotate(selectedWP.imgView.getRotate());
                    selectedWP.info.setText(selectedWP.pose.toString().replace(" | ", "\n"));
                }
            } else if (mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
                if (!isDragging) {
                    if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                        if (mouseEvent.getTarget() instanceof Rectangle || mouseEvent.getTarget() instanceof FieldPane) {
                            select(null);
                        }
                    } else if (mouseEvent.getButton() == MouseButton.SECONDARY && mouseEvent.getTarget().equals(wbbBorder)) {
                        if (isInWBB(mouseX - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0, mouseY - Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0, Constants.getDouble("dashboard.gui.sizes.waypoint"))) {
                            Pose newPose = displayToPose(mouseX, mouseY, 0);
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
        boolean[] intersects = new boolean[]{true, true};
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

    // Deselect previous and select new selected
    public void select(Waypoint newSelected) {
        if (selectedWP != newSelected) {
            if (selectedWP != null) {
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