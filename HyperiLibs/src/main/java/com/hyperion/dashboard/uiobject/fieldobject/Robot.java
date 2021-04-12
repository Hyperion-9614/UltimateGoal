package com.hyperion.dashboard.uiobject.fieldobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;

import org.json.JSONArray;

import javafx.application.Platform;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class Robot extends FieldObject {

    public RigidBody rB;
    public Arrow acc;
    public Arrow vel;

    public ImageView imgView;
    public Text info;

    public Robot(ID id, RigidBody rB) {
        this.id = id;
        this.rB = rB;
        createDisplayGroup();
        refreshDisplayGroup();
    }

    public Robot(ID id) {
        this(id, new Pose(0, 0, 0));
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
            displayGroup.setCache(true);
            displayGroup.setCacheHint(CacheHint.SPEED);

            imgView = new ImageView(Constants.getFile("img", "robot.png").toURI().toURL().toString());
            imgView.setFitWidth(Dashboard.fieldPane.robotSize);
            imgView.setFitHeight(Dashboard.fieldPane.robotSize);
            imgView.setPickOnBounds(true);
            if (id.get(-1).equals("setPoint")) {
                ColorAdjust greenify = new ColorAdjust();
                greenify.setHue(-0.5);
                imgView.setEffect(greenify);
            }
            displayGroup.getChildren().add(imgView);

            info = new Text();
            info.setFill(Color.WHITE);
            displayGroup.getChildren().add(info);

            acc = new Arrow(new ID(id, "arrow", "acc"), Color.WHITE, 15, rB, rB.tAcc);
            vel = new Arrow(new ID(id, "arrow", "vel"), Color.BLACK, 15, rB, rB.tVel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().add(displayGroup));
        acc.addDisplayGroup();
        vel.addDisplayGroup();
    }

    public void refreshDisplayGroup() {
        Platform.runLater(() -> {
            double[] display = Dashboard.fieldPane.poseToDisplay(rB, Dashboard.fieldPane.robotSize);
            imgView.relocate(display[0], display[1]);
            imgView.setRotate(display[2]);

            info.setText(rB.toString().replace(" | ", "\n")
                    .replace("°", "\u00B0")
                    .replace("θ", "\u03F4".toLowerCase())
                    .replace("²", "\u00B2"));
            info.relocate(display[0] + Dashboard.fieldPane.robotSize + 3, display[1] + Dashboard.fieldPane.robotSize - 21);
        });
        acc.refreshDisplayGroup();
        vel.refreshDisplayGroup();
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().remove(displayGroup));
        Dashboard.fieldPane.isRobotOnField = false;
        acc.removeDisplayGroup();
        vel.removeDisplayGroup();
    }

    public void select() {

    }

    public void deselect() {

    }

}
