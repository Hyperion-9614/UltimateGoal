package com.hyperion.dashboard.uiobj;

import com.hyperion.dashboard.UIClient;
import com.hyperion.motion.math.RigidBody;

import java.io.File;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.image.ImageView;

public class Robot {

    public Group displayGroup;
    public ImageView imgView;
    public RigidBody rigidBody;

    public Robot() {
        try {
            displayGroup = new Group();

            imgView = new ImageView(new File(UIClient.constants.RES_IMG_PREFIX + "/robot.png").toURI().toURL().toString());
            imgView.setFitWidth(UIClient.fieldPane.robotSize);
            imgView.setFitHeight(UIClient.fieldPane.robotSize);
            displayGroup.getChildren().add(imgView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> UIClient.fieldPane.getChildren().add(displayGroup));
    }

    public void refreshDisplayGroup(RigidBody rigidBody) {
        this.rigidBody = new RigidBody(rigidBody);
        double[] display = UIClient.fieldPane.poseToDisplay(rigidBody.pose, UIClient.fieldPane.robotSize);
        imgView.relocate(display[0], display[1]);
        imgView.setRotate(display[2]);
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> UIClient.fieldPane.getChildren().remove(displayGroup));
    }

}
