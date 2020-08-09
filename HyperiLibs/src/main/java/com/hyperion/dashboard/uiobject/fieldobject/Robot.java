package com.hyperion.dashboard.uiobject.fieldobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.uiobject.fieldobject.FieldObject;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;

import org.json.JSONArray;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class Robot extends FieldObject {

    public RigidBody rB;

    public ImageView imgView;
    public Text info;

    public Robot(ID id) {
        this.id = id;
        createDisplayGroup();
    }

    public Robot(ID id, RigidBody rB) {
        this(id);
        this.rB = new RigidBody(rB);
        refreshDisplayGroup();
    }

    public Robot(ID id, Pose pose) {
        this(id, new RigidBody(pose));
    }

    public Robot(ID id, JSONArray arr) {
        this(id, new RigidBody(arr));
    }

    public void createDisplayGroup() {
        try {
            displayGroup = new Group();

            imgView = new ImageView(Constants.getFile("img", "robot.png").toURI().toURL().toString());
            imgView.setFitWidth(Dashboard.fieldPane.robotSize);
            imgView.setFitHeight(Dashboard.fieldPane.robotSize);
            imgView.setPickOnBounds(true);
            displayGroup.getChildren().add(imgView);

            info = new Text();
            info.setFill(Color.WHITE);
            displayGroup.getChildren().add(info);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().add(displayGroup));
    }

    public void refreshDisplayGroup() {
        double[] display = Dashboard.fieldPane.poseToDisplay(rB, Dashboard.fieldPane.robotSize);
        imgView.relocate(display[0], display[1]);
        imgView.setRotate(display[2]);

        info.setText(rB.toString().replace(" | ", "\n")
                .replace("°", "\u00B0")
                .replace("θ", "\u03F4".toLowerCase())
                .replace("²", "\u00B2"));
        info.relocate(display[0] + Dashboard.fieldPane.robotSize + 3, display[1] + Dashboard.fieldPane.robotSize - 21);
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
