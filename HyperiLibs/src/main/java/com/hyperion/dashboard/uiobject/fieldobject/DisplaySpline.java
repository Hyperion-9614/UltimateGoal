package com.hyperion.dashboard.uiobject.fieldobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.trajectory.SplineTrajectory;

import org.json.JSONObject;

import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class DisplaySpline extends FieldObject {

    public SplineTrajectory spline;
    public ArrayList<Waypoint> waypoints;

    public DisplaySpline() {

    }

    public DisplaySpline(JSONObject obj) {
        spline = new SplineTrajectory(obj);
        refreshDisplayGroup();
    }

    public DisplaySpline(ID id, SplineTrajectory spline) {
        this.id = id;
        this.spline = spline;
        refreshDisplayGroup();
    }

    public DisplaySpline(Pose start) {
        this.id = new ID(Dashboard.opModeID, "spline", " ");
        spline = new SplineTrajectory(new RigidBody(start));
        refreshDisplayGroup();
        Dashboard.fieldPane.select(waypoints.get(0));
    }

    public DisplaySpline(ID id, JSONObject obj) {
        this(id, new SplineTrajectory(obj));
    }

    public void createDisplayGroup() {
        if (spline.waypoints.size() >= 2) {
            double[] lastPoseArr = Dashboard.fieldPane.poseToDisplay(spline.planningPoints.get(0), 0);
            for (int i = 1; i < spline.planningPoints.size(); i++) {
                double[] poseArr = Dashboard.fieldPane.poseToDisplay(spline.planningPoints.get(i), 0);
                Line segment = new Line(lastPoseArr[0], lastPoseArr[1], poseArr[0], poseArr[1]);
                segment.setStroke(Color.hsb(360.0 * (i - 1.0) / spline.planningPoints.size(), 1.0, 1.0));
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

                if (i < spline.planningPoints.size() - 1) {
                    Color selectColor = Color.DIMGRAY;
                    Rectangle selectRect = new Rectangle(poseArr[0] - Dashboard.fieldPane.robotSize / 2.0, poseArr[1] - Dashboard.fieldPane.robotSize / 2.0,
                            Dashboard.fieldPane.robotSize, Dashboard.fieldPane.robotSize);
                    selectRect.setStroke(selectColor);
                    selectRect.setStrokeWidth(2);
                    selectRect.setRotate(poseArr[2]);
                    selectRect.setFill(new Color(selectColor.getRed(), selectColor.getGreen(), selectColor.getBlue(), 0.3));
                    selection.getChildren().add(selectRect);
                    selectRect.toBack();
                }

                if (i == spline.planningPoints.size() - 1) {
                    double[] lastParr = Dashboard.fieldPane.poseToDisplay(spline.waypoints.get(spline.waypoints.size() - 1), 0);
                    Line lastSeg = new Line(poseArr[0], poseArr[1], lastParr[0], lastParr[1]);
                    lastSeg.setStroke(Color.hsb(360.0 * i / spline.planningPoints.size(), 1.0, 1.0));
                    lastSeg.setStrokeWidth(3);
                    displayGroup.getChildren().add(lastSeg);
                    lastSeg.toBack();
                }

                lastPoseArr = poseArr.clone();
            }
        }

        for (int i = 0; i < spline.waypoints.size(); i++) {
            RigidBody wpPP = spline.waypoints.get(i);
            Waypoint waypoint = new Waypoint(new ID(id, i), wpPP, this, (i == 0), false);
            waypoints.add(waypoint);
            waypoint.addDisplayGroup();
        }

        if (Dashboard.fieldPane.selectedWP != null && Dashboard.fieldPane.selectedWP.id.get(4).equals(id.get(4))) {
            int i = Integer.parseInt(Dashboard.fieldPane.selectedWP.id.get(5));
            Dashboard.fieldPane.select(waypoints.get(i));
        }
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> {
            if (id.sub(0, 3).equals(Dashboard.opModeID.toString())) {
                Dashboard.fieldPane.getChildren().add(displayGroup);
            }
        });
    }

    public void refreshDisplayGroup() {
        if (displayGroup != null) {
            displayGroup.getChildren().clear();
        } else {
            displayGroup = new Group();
        }
        waypoints = new ArrayList<>();
        selection = new Group();
        createDisplayGroup();
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().remove(displayGroup));
    }

}
