package com.hyperion.dashboard.uiobject.fieldobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.motion.math.Pose;

import org.json.JSONArray;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class PathPoint extends FieldObject {

    public Pose pose;
    public Circle pathPoint;

    public PathPoint(ID id, JSONArray pose) {
        this.id = id;
        this.pose = new Pose(pose);
        createDisplayGroup();
    }

    @Override
    public void createDisplayGroup() {
        displayGroup = new Group();
        double[] poseArr = Dashboard.fieldPane.poseToDisplay(pose, 0);
        pathPoint = new Circle(poseArr[0], poseArr[1], Constants.getDouble("dashboard.gui.sizes.planningPoint"));
        pathPoint.setFill(Color.DARKSLATEGRAY);

        displayGroup.getChildren().add(pathPoint);
    }

    @Override
    public void addDisplayGroup() {
        Platform.runLater(() -> {
            Dashboard.fieldPane.getChildren().add(displayGroup);
            displayGroup.toBack();
        });
    }

    @Override
    public void refreshDisplayGroup() {
        double[] poseArr = Dashboard.fieldPane.poseToDisplay(pose, 0);
        pathPoint.setCenterX(poseArr[0]);
        pathPoint.setCenterY(poseArr[1]);
    }

    @Override
    public void removeDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().remove(displayGroup));
    }
}
