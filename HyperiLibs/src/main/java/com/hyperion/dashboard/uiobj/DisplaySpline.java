package com.hyperion.dashboard.uiobj;

import com.hyperion.common.Constants;
import com.hyperion.dashboard.UIClient;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.trajectory.SplineTrajectory;

import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class DisplaySpline {

    public Constants constants;
    public String id;
    public SplineTrajectory spline;

    public Group displayGroup;
    public ArrayList<Waypoint> waypoints;
    public ArrayList<Rectangle> selectRects;

    public DisplaySpline() {

    }

    public DisplaySpline(String json, Constants constants) {
        this.constants = constants;
        spline = new SplineTrajectory(json, constants);
        displayGroup = new Group();
        refreshDisplayGroup();
    }

    public DisplaySpline(String id, SplineTrajectory spline, Constants constants) {
        this.id = id;
        this.spline = spline;
        this.constants = constants;
        displayGroup = new Group();
        refreshDisplayGroup();
    }

    public DisplaySpline(Pose start, Constants constants) {
        this.id = UIClient.opModeID + ".spline.";
        this.constants = constants;
        ArrayList<RigidBody> wps = new ArrayList<>();
        wps.add(new RigidBody(start));
        spline = new SplineTrajectory(wps, constants);
        displayGroup = new Group();
        refreshDisplayGroup();

    }

    public void addDisplayGroup() {
        Platform.runLater(() -> {
            if (id.startsWith(UIClient.opModeID)) {
                UIClient.fieldPane.getChildren().add(displayGroup);
            }
        });
    }

    public void refreshDisplayGroup() {
        spline.endPath();
        displayGroup.getChildren().clear();
        waypoints = new ArrayList<>();
        selectRects = new ArrayList<>();

        if (spline.waypoints.size() >= 2) {
            Color tauPathPointColor = new Color(0.3, 0.3, 0.3, 0.5);
            for (double t = 0; t <= spline.waypoints.size(); t += 0.2) {
                double[] poseArr = UIClient.fieldPane.poseToDisplay(spline.getTPose(t), 0);
                Circle pathPoint = new Circle(poseArr[0], poseArr[1], UIClient.constants.PATHPOINT_SIZE);
                pathPoint.setFill(tauPathPointColor);
                displayGroup.getChildren().add(pathPoint);
            }

            Color distancePathPointColor = new Color(1.0, 1.0, 1.0, 0.5);
            for (double d = 0; d <= spline.waypoints.get(spline.waypoints.size() - 1).distance; d += 10) {
                double[] poseArr = UIClient.fieldPane.poseToDisplay(spline.getDPose(d), 0);
                Circle pathPoint = new Circle(poseArr[0], poseArr[1], UIClient.constants.PATHPOINT_SIZE);
                pathPoint.setFill(distancePathPointColor);
                displayGroup.getChildren().add(pathPoint);
            }

            double[] lastPoseArr = UIClient.fieldPane.poseToDisplay(spline.planningPoints.get(0).pose, 0);
            for (int i = 1; i < spline.planningPoints.size(); i++) {
                double[] poseArr = UIClient.fieldPane.poseToDisplay(spline.planningPoints.get(i).pose, 0);
                Line segment = new Line(lastPoseArr[0], lastPoseArr[1], poseArr[0], poseArr[1]);
                segment.setStroke(Color.hsb(360 * (i - 1.0) / spline.planningPoints.size(), 1.0, 1.0));
                segment.setStrokeWidth(2);
                displayGroup.getChildren().add(segment);
                segment.toBack();

                if (i == 1) {
                    Circle pp0 = new Circle(poseArr[0], poseArr[1], UIClient.constants.PLANNINGPOINT_SIZE);
                    pp0.setFill(Color.WHITE);
                    displayGroup.getChildren().add(pp0);
                }

                Circle pp = new Circle(poseArr[0], poseArr[1], UIClient.constants.PLANNINGPOINT_SIZE);
                pp.setFill(Color.WHITE);
                displayGroup.getChildren().add(pp);

                Color selectColor = Color.DIMGRAY;
                Rectangle selectRect = new Rectangle(poseArr[0] - UIClient.fieldPane.robotSize / 2.0, poseArr[1] - UIClient.fieldPane.robotSize / 2.0,
                                                        UIClient.fieldPane.robotSize, UIClient.fieldPane.robotSize);
                selectRect.setStroke(selectColor);
                selectRect.setStrokeWidth(2);
                selectRect.setRotate(poseArr[2]);
                selectRect.setFill(new Color(selectColor.getRed(), selectColor.getGreen(), selectColor.getBlue(), 0.3));
                selectRect.setVisible(false);
                displayGroup.getChildren().add(selectRect);
                selectRect.toBack();
                selectRects.add(selectRect);

                if (i == spline.planningPoints.size() - 1) {
                    double[] lastParr = UIClient.fieldPane.poseToDisplay(spline.waypoints.get(spline.waypoints.size() - 1).pose, 0);
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
            Waypoint waypoint = new Waypoint("", wpPP.pose, constants, this, (i == 0));
            if (waypoint.renderID) {
                waypoint.idField.setText(id.replace(UIClient.opModeID + ".spline.", ""));
                waypoint.idField.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        this.id = UIClient.opModeID + ".spline." + waypoint.idField.getText();
                        UIClient.sendDashboard();
                    }
                });
            }
            waypoints.add(waypoint);
            waypoint.addDisplayGroup();
        }

        displayGroup.setOnMousePressed((event -> select()));
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> UIClient.fieldPane.getChildren().remove(displayGroup));
    }

    public void select() {
        for (DisplaySpline spline : UIClient.splines) {
            if (spline != this) {
                spline.deselect();
            }
        }
        UIClient.selectedSpline = this;
        for (Rectangle selectRect : selectRects) {
            selectRect.setVisible(true);
        }
        UIClient.displayPane.updateGraphs(this);
    }

    public void deselect() {
        UIClient.selectedSpline = null;
        for (Waypoint waypoint : waypoints) {
            waypoint.deselect();
        }
        for (Rectangle selectRect : selectRects) {
            selectRect.setVisible(false);
        }
    }

}
