package com.hyperion.dashboard.uiobject;

import com.hyperion.common.Constants;
import com.hyperion.dashboard.UIMain;
import com.hyperion.motion.math.RigidBody;

import org.json.JSONArray;

import java.io.File;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.image.ImageView;

public class Robot extends FieldObject {

    public ImageView imgView;
    public RigidBody rigidBody;

    public Robot(JSONArray arr) {
        this.id = "robot";
        createDisplayGroup();
        rigidBody = new RigidBody(arr);
        refreshDisplayGroup();
    }

    public void createDisplayGroup() {
        try {
            displayGroup = new Group();

            imgView = new ImageView(new File(Constants.RES_IMG + "/robot.png").toURI().toURL().toString());
            imgView.setFitWidth(UIMain.fieldPane.robotSize);
            imgView.setFitHeight(UIMain.fieldPane.robotSize);
            imgView.setPickOnBounds(true);
            displayGroup.getChildren().add(imgView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> UIMain.fieldPane.getChildren().add(displayGroup));
    }

    public void refreshDisplayGroup() {
        double[] display = UIMain.fieldPane.poseToDisplay(rigidBody, UIMain.fieldPane.robotSize);
        imgView.relocate(display[0], display[1]);
        imgView.setRotate(display[2]);
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> UIMain.fieldPane.getChildren().remove(displayGroup));
        UIMain.isRobotOnField = false;
    }

    public void select() {

    }

    public void deselect() {

    }

}
