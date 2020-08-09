package com.hyperion.dashboard.uiobject.fieldobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.dashboard.uiobject.fieldobject.FieldObject;
import com.hyperion.motion.math.Pose;

import org.json.JSONArray;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class PathPoint extends FieldObject {

    public Pose pose;
    public Circle pathPoint;

    private long startDragTime;
    private double dragDx, dragDy;

    public PathPoint(ID id, JSONArray pose) {
        this.id = id;
        this.pose = new Pose(pose);
        createDisplayGroup();
    }

    @Override
    public void createDisplayGroup() {
        displayGroup = new Group();
        double[] poseArr = Dashboard.fieldPane.poseToDisplay(pose, 0);
        pathPoint = new Circle(poseArr[0], poseArr[1], 1.25 * Constants.getDouble("dashboard.gui.sizes.planningPoint"));
        pathPoint.setFill(Color.BLACK);

        displayGroup.setOnMouseClicked((event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
                Dashboard.editField(new FieldEdit(id, FieldEdit.Type.DELETE, "{}"));
            }
        }));
        displayGroup.setOnMousePressed((event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                startDragTime = System.currentTimeMillis();
                dragDx = pathPoint.getCenterX() - event.getSceneX();
                dragDy = pathPoint.getCenterY() - event.getSceneY();
            }
        }));
        displayGroup.setOnMouseDragged((event -> {
            pathPoint.setCenterX(event.getSceneX() + dragDx);
            pathPoint.setCenterY(event.getSceneY() + dragDy);
            pose = Dashboard.fieldPane.displayToPose(pathPoint.getCenterX(), pathPoint.getCenterY(), 0);
            refreshDisplayGroup();
        }));
        displayGroup.setOnMouseReleased((event -> {
            if (System.currentTimeMillis() - startDragTime > Constants.getInt("dashboard.gui.dragTime") && event.getButton() == MouseButton.PRIMARY) {
                Dashboard.editField(new FieldEdit(id, FieldEdit.Type.EDIT_BODY, new JSONArray(pose.toArray())));
            }
        }));
        displayGroup.getChildren().add(pathPoint);
    }

    @Override
    public void addDisplayGroup() {
        Platform.runLater(() -> {
            Dashboard.fieldPane.getChildren().add(displayGroup);
            displayGroup.toFront();
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
