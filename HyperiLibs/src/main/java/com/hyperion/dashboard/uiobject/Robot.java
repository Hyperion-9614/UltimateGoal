package com.hyperion.dashboard.uiobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.motion.math.RigidBody;

import org.json.JSONArray;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.image.ImageView;

public class Robot extends FieldObject {

    public ImageView imgView;
    public RigidBody rigidBody;

    public Robot(JSONArray arr) {
        this.id = new ID("robot");
        createDisplayGroup();
        rigidBody = new RigidBody(arr);
        refreshDisplayGroup();
    }

    public void createDisplayGroup() {
        try {
            displayGroup = new Group();

            imgView = new ImageView(Constants.getFile("img", "robot.png").toURI().toURL().toString());
            imgView.setFitWidth(Dashboard.fieldPane.robotSize);
            imgView.setFitHeight(Dashboard.fieldPane.robotSize);
            imgView.setPickOnBounds(true);
            displayGroup.getChildren().add(imgView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().add(displayGroup));
    }

    public void refreshDisplayGroup() {
        double[] display = Dashboard.fieldPane.poseToDisplay(rigidBody, Dashboard.fieldPane.robotSize);
        imgView.relocate(display[0], display[1]);
        imgView.setRotate(display[2]);
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().remove(displayGroup));
        Dashboard.isRobotOnField = false;
    }

    public void select() {

    }

    public void deselect() {

    }

}
