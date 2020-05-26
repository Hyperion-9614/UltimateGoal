package com.hyperion.dashboard.uiobject;

import com.hyperion.common.Constants;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.trajectory.SplineTrajectory;

import org.json.JSONObject;

import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class DisplaySpline extends FieldObject {

    public SplineTrajectory spline;
    public int selectedWaypointIndex = -1;

    public ArrayList<Waypoint> waypoints;
    public ArrayList<Rectangle> selectRects;

    public DisplaySpline() {

    }

    public DisplaySpline(JSONObject obj) {
        spline = new SplineTrajectory(obj);
        displayGroup = new Group();
        refreshDisplayGroup();
    }

    public DisplaySpline(String id, SplineTrajectory spline) {
        this.id = id;
        this.spline = spline;
        displayGroup = new Group();
        refreshDisplayGroup();
    }

    public DisplaySpline(Pose start) {
        this.id = Dashboard.opModeID + ".spline.";
        ArrayList<RigidBody> wps = new ArrayList<>();
        wps.add(new RigidBody(start));
        spline = new SplineTrajectory(wps);
        displayGroup = new Group();
        refreshDisplayGroup();
    }

    public DisplaySpline(String id, JSONObject obj) {
        this(id, new SplineTrajectory(obj));
    }

    public void createDisplayGroup() {
        if (spline.waypoints.size() >= 2) {
            Color tauPathPointColor = new Color(0.3, 0.3, 0.3, 0.5);
            for (double t = 0; t <= spline.waypoints.size() - 1; t += 0.2) {
                double[] poseArr = Dashboard.fieldPane.poseToDisplay(spline.getTPose(t), 0);
                Circle pathPoint = new Circle(poseArr[0], poseArr[1], Constants.getDouble("dashboard.gui.sizes.pathPoint"));
                pathPoint.setFill(tauPathPointColor);
                displayGroup.getChildren().add(pathPoint);
            }

            Color distancePathPointColor = new Color(1.0, 1.0, 1.0, 0.5);
            for (double d = 0; d <= spline.waypoints.get(spline.waypoints.size() - 1).distance; d += 10) {
                double[] poseArr = Dashboard.fieldPane.poseToDisplay(spline.getDPose(d), 0);
                Circle pathPoint = new Circle(poseArr[0], poseArr[1], Constants.getDouble("dashboard.gui.sizes.pathPoint"));
                pathPoint.setFill(distancePathPointColor);
                displayGroup.getChildren().add(pathPoint);
            }

            double[] lastPoseArr = Dashboard.fieldPane.poseToDisplay(spline.planningPoints.get(0), 0);
            for (int i = 1; i < spline.planningPoints.size(); i++) {
                double[] poseArr = Dashboard.fieldPane.poseToDisplay(spline.planningPoints.get(i), 0);
                Line segment = new Line(lastPoseArr[0], lastPoseArr[1], poseArr[0], poseArr[1]);
                segment.setStroke(Color.hsb(360 * (i - 1.0) / spline.planningPoints.size(), 1.0, 1.0));
                segment.setStrokeWidth(2);
                displayGroup.getChildren().add(segment);
                segment.toBack();

                if (i == 1) {
                    Circle pp0 = new Circle(poseArr[0], poseArr[1], Constants.getDouble("dashboard.gui.sizes.planningPoint"));
                    pp0.setFill(Color.WHITE);
                    displayGroup.getChildren().add(pp0);
                }

                Circle pp = new Circle(poseArr[0], poseArr[1], Constants.getDouble("dashboard.gui.sizes.planningPoint"));
                pp.setFill(Color.WHITE);
                displayGroup.getChildren().add(pp);

                Color selectColor = Color.DIMGRAY;
                Rectangle selectRect = new Rectangle(poseArr[0] - Dashboard.fieldPane.robotSize / 2.0, poseArr[1] - Dashboard.fieldPane.robotSize / 2.0,
                        Dashboard.fieldPane.robotSize, Dashboard.fieldPane.robotSize);
                selectRect.setStroke(selectColor);
                selectRect.setStrokeWidth(2);
                selectRect.setRotate(poseArr[2]);
                selectRect.setFill(new Color(selectColor.getRed(), selectColor.getGreen(), selectColor.getBlue(), 0.3));
                selectRect.setVisible(false);
                displayGroup.getChildren().add(selectRect);
                selectRect.toBack();
                selectRects.add(selectRect);

                if (i == spline.planningPoints.size() - 1) {
                    double[] lastParr = Dashboard.fieldPane.poseToDisplay(spline.waypoints.get(spline.waypoints.size() - 1), 0);
                    Line lastSeg = new Line(poseArr[0], poseArr[1], lastParr[0], lastParr[1]);
                    lastSeg.setStroke(Color.hsb(360 * i / spline.planningPoints.size(), 1.0, 1.0));
                    lastSeg.setStrokeWidth(3);
                    displayGroup.getChildren().add(lastSeg);
                    lastSeg.toBack();
                }

                lastPoseArr = poseArr.clone();
            }
        }

        for (int i = 0; i < spline.waypoints.size(); i++) {
            RigidBody wpPP = spline.waypoints.get(i);
            Waypoint waypoint = new Waypoint("", wpPP, this, (i == 0));
            if (waypoint.renderID) {
                waypoint.idField.setText(id.replace(Dashboard.opModeID + ".spline.", ""));
                waypoint.idField.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        String oldID = id;
                        id = Dashboard.opModeID + ".spline." + waypoint.idField.getText();
                        Dashboard.editField(new FieldEdit(oldID, FieldEdit.Type.EDIT_ID, id));
                    }
                });
            }
            waypoints.add(waypoint);
            waypoint.addDisplayGroup();
        }

        displayGroup.setOnMousePressed((event -> select()));
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> {
            if (id.startsWith(Dashboard.opModeID)) {
                Dashboard.fieldPane.getChildren().add(displayGroup);
            }
        });
    }

    public void refreshDisplayGroup() {
        spline.endPath();
        displayGroup.getChildren().clear();
        waypoints = new ArrayList<>();
        selectRects = new ArrayList<>();
        createDisplayGroup();
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().remove(displayGroup));
    }

    public void select() {
        for (FieldObject object : Dashboard.fieldObjects) {
            if (object instanceof DisplaySpline && !object.equals(this)) {
                object.deselect();
            }
        }
        Dashboard.selectedSpline = this;
        for (Rectangle selectRect : selectRects) {
            selectRect.setVisible(true);
        }
        Dashboard.visualPane.updateGraphs(this);
    }

    public void deselect() {
        Dashboard.selectedSpline = null;
        selectedWaypointIndex = -1;
        for (Waypoint waypoint : waypoints) {
            waypoint.deselect();
        }
        for (Rectangle selectRect : selectRects) {
            selectRect.setVisible(false);
        }
    }

}
