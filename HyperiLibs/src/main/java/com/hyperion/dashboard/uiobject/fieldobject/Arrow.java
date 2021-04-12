package com.hyperion.dashboard.uiobject.fieldobject;

import com.hyperion.common.ID;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

/*
 * Credit: kn0412 on GitHub
 * https://gist.github.com/kn0412/2086581e98a32c8dfa1f69772f14bca4
 */

public class Arrow extends FieldObject {

    public Pose origin;
    public Vector2D vec;

    public Color color;
    public double headSize;

    public double startX;
    public double startY;
    public double endX;
    public double endY;

    public Group displayGroup;
    public Line shaft;
    public Line lHead;
    public Line rHead;

    public Arrow(ID id, Color color, double headSize, Pose origin, Vector2D vec) {
        this.id = id;
        this.color = color;
        this.headSize = headSize;
        this.origin = origin;
        this.vec = vec;
        createDisplayGroup();
    }

    @Override
    public void createDisplayGroup() {
        displayGroup = new Group();

        shaft = new Line();
        shaft.setStroke(color);
        shaft.setStrokeWidth(3);

        lHead = new Line();
        lHead.setStroke(color);
        lHead.setStrokeWidth(3);

        rHead = new Line();
        rHead.setStroke(color);
        rHead.setStrokeWidth(3);

        displayGroup.getChildren().addAll(shaft, lHead, rHead);
    }

    @Override
    public void addDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().add(displayGroup));
    }

    public double[] getStart() {
        return new double[]{ shaft.getStartX(), shaft.getStartY() };
    }
    public double[] getEnd() {
        return new double[]{ shaft.getEndX(), shaft.getEndY() };
    }

    @Override
    public void refreshDisplayGroup() {
        Platform.runLater(() -> {
            double[] start = Dashboard.fieldPane.poseToDisplay(origin, 0);
            double[] end = Dashboard.fieldPane.poseToDisplay(origin.addVector(vec.scaled(0.3).magnituded(Math.min(vec.mag, 50))), 0);

            startX = start[0];
            startY = start[1];
            endX = end[0];
            endY = end[1];

            shaft.setStartX(startX);
            shaft.setStartY(startY);
            shaft.setEndX(endX);
            shaft.setEndY(endY);

            // Head
            double angle = Math.atan2((endY - startY), (endX - startX)) - Math.PI / 2.0;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            // Point 1
            double x1 = (-1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * headSize + endX;
            double y1 = (-1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * headSize + endY;

            // Point 2
            double x2 = (1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * headSize + endX;
            double y2 = (1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * headSize + endY;

            lHead.setStartX(x1);
            lHead.setStartY(y1);
            lHead.setEndX(endX);
            lHead.setEndY(endY);

            rHead.setStartX(x2);
            rHead.setStartY(y2);
            rHead.setEndX(endX);
            rHead.setEndY(endY);
        });
    }

    @Override
    public void removeDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().remove(displayGroup));
    }
}
