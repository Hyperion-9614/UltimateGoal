package com.hyperion.dashboard.uiobject;

import com.hyperion.dashboard.UIClient;
import com.hyperion.motion.math.RigidBody;

import org.json.JSONArray;

import java.io.File;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.image.ImageView;

public class Robot extends FieldObject {

    public ImageView imgView;
    public RigidBody rigidBody;
    public boolean isViewable;

    public Robot(JSONArray arr) {
        this.id = "robot";
        createDisplayGroup();
        rigidBody = new RigidBody(arr);
        refreshDisplayGroup();
    }

    public void createDisplayGroup() {
        try {
            displayGroup = new Group();

            imgView = new ImageView(new File(UIClient.constants.RES_IMG_PREFIX + "/robot.png").toURI().toURL().toString());
            imgView.setFitWidth(UIClient.fieldPane.robotSize);
            imgView.setFitHeight(UIClient.fieldPane.robotSize);
            imgView.setPickOnBounds(true);
            displayGroup.getChildren().add(imgView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> UIClient.fieldPane.getChildren().add(displayGroup));
        isViewable = true;
    }

    public void refreshDisplayGroup() {
        if (!isViewable) {
            addDisplayGroup();
        }
        this.rigidBody = new RigidBody(rigidBody);
        double[] display = UIClient.fieldPane.poseToDisplay(rigidBody.pose, UIClient.fieldPane.robotSize);
        imgView.relocate(display[0], display[1]);
        imgView.setRotate(display[2]);
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> UIClient.fieldPane.getChildren().remove(displayGroup));
        isViewable = false;
    }

    public void select() {

    }

    public void deselect() {

    }

}
