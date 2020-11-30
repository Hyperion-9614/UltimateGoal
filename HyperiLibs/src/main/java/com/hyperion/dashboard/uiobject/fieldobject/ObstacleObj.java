package com.hyperion.dashboard.uiobject.fieldobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.net.FieldEdit;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.pathplanning.Obstacle;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public abstract class ObstacleObj extends FieldObject {

    public Pose start;
    public Pose end;

    public Obstacle obstacle;
    public javafx.scene.shape.Circle centerDot;

    private long startDragTime;
    private double dragDx, dragDy;

    public double rBuffer = Constants.getDouble("pathing.obstacles.rBuffer");

    public ObstacleObj(ID id, Pose start, Pose end) {
        this.id = id;
        this.start = start;
        this.end = end;
        displayGroup = new Group();

        createDisplayGroup();
        addListeners();
        createObstacle();
    }

    public ObstacleObj(ID id, Pose start) {
        this(id, start, start);
    }

    public ObstacleObj(ID id, JSONObject obj) {
        this(id, new Pose(obj.getJSONArray("start")), new Pose(obj.getJSONArray("end")));
        Dashboard.fieldPane.currObstacle = this;
    }

    private void addListeners() {
        displayGroup.setOnMouseClicked((event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                Dashboard.editField(new FieldEdit(id, FieldEdit.Type.DELETE, "{}"));
                if (id.contains("fixed"))
                    Dashboard.fieldPane.fixedObstacles.remove(obstacle);
                else
                    Dashboard.fieldPane.dynamicObstacles.remove(obstacle);
            }
        }));
        displayGroup.setOnMousePressed((event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                startDragTime = System.currentTimeMillis();
                dragDx = centerDot.getCenterX() - event.getSceneX();
                dragDy = centerDot.getCenterY() - event.getSceneY();
            }
        }));
        displayGroup.setOnMouseDragged((event -> {
            double[] startArr = Dashboard.fieldPane.poseToDisplay(this.start, 0);
            double[] endArr = Dashboard.fieldPane.poseToDisplay(this.end, 0);
            if (this instanceof Circle) {
                double radius = MathUtils.distance(startArr[0], startArr[1], endArr[0], endArr[1]);
                this.start = Dashboard.fieldPane.displayToPose(event.getSceneX() + dragDx, event.getSceneY() + dragDy, 0);
                this.end = Dashboard.fieldPane.displayToPose(event.getSceneX() + dragDx + radius, event.getSceneY() + dragDy, 0);
            } else {
                double width = endArr[0] - startArr[0];
                double height = endArr[1] - startArr[1];
                this.start = Dashboard.fieldPane.displayToPose(event.getSceneX() + dragDx - width / 2, event.getSceneY() + dragDy - height / 2, 0);
                this.end = Dashboard.fieldPane.displayToPose(event.getSceneX() + dragDx + width / 2, event.getSceneY() + dragDy + height / 2, 0);
            }
            refreshDisplayGroup();
        }));
        displayGroup.setOnMouseReleased((event -> {
            if (System.currentTimeMillis() - startDragTime > Constants.getInt("dashboard.gui.dragTime") && event.getButton() == MouseButton.PRIMARY) {
                Dashboard.editField(new FieldEdit(id, FieldEdit.Type.EDIT_BODY, toJSONObject()));
            }
        }));
    }

    public abstract void createObstacle();

    public JSONObject toJSONObject() {
        JSONObject toReturn = new JSONObject();
        toReturn.put("start", new JSONArray(start.toArray()));
        toReturn.put("end", new JSONArray(end.toArray()));
        return toReturn;
    }

    @Override
    public void addDisplayGroup() {
        Platform.runLater(() -> {
            Dashboard.fieldPane.getChildren().add(displayGroup);
            displayGroup.toFront();
        });
    }

    @Override
    public void removeDisplayGroup() {
        Platform.runLater(() -> Dashboard.fieldPane.getChildren().remove(displayGroup));
    }

    public static class Rect extends ObstacleObj {

        public Rectangle rect;
        public Rectangle buffer;

        public Rect(ID id, Pose start) {
            super(id, start);
        }
        public Rect(ID id, Pose start, Pose end) {
            super(id, start, end);
        }
        public Rect(ID id, JSONObject obj) {
            super(id, obj);
        }

        @Override
        public void createObstacle() {
            this.obstacle = new Obstacle.Rect(start, end);
        }

        @Override
        public void createDisplayGroup() {
            double[] startArr = Dashboard.fieldPane.poseToDisplay(start, 0);
            double[] endArr = Dashboard.fieldPane.poseToDisplay(end, 0);
            double[] bufferStartArr = Dashboard.fieldPane.poseToDisplay(start.addVector(new Vector2D(-rBuffer, rBuffer, true)), 0);
            double[] bufferEndArr = Dashboard.fieldPane.poseToDisplay(end.addVector(new Vector2D(rBuffer, -rBuffer, true)), 0);

            buffer = new Rectangle();
            buffer.setX(bufferStartArr[0]);
            buffer.setY(bufferStartArr[1]);
            buffer.setWidth(bufferEndArr[0] - bufferStartArr[0]);
            buffer.setHeight(bufferEndArr[1] - bufferStartArr[1]);
            buffer.setStroke(Color.GRAY);
            buffer.setStrokeWidth(2);
            buffer.setFill(new Color(Color.GRAY.getRed(), Color.GRAY.getGreen(), Color.GRAY.getBlue(), 0.3));
            displayGroup.getChildren().add(buffer);

            rect = new Rectangle();
            rect.setX(startArr[0]);
            rect.setY(startArr[1]);
            rect.setWidth(endArr[0] - startArr[0]);
            rect.setHeight(endArr[1] - startArr[1]);
            Color color = (id.contains("fixed") ? Color.YELLOW : Color.RED);
            rect.setStroke(color);
            rect.setStrokeWidth(2);
            rect.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.3));
            displayGroup.getChildren().add(rect);

            centerDot = new javafx.scene.shape.Circle();
            centerDot.setCenterX(MathUtils.halfway(startArr[0], endArr[0]));
            centerDot.setCenterY(MathUtils.halfway(startArr[1], endArr[1]));
            centerDot.setRadius(1.25 * Constants.getDouble("dashboard.gui.sizes.planningPoint"));
            centerDot.setFill(Color.WHITE);
            displayGroup.getChildren().add(centerDot);
        }

        @Override
        public void refreshDisplayGroup() {
            double[] startArr = Dashboard.fieldPane.poseToDisplay(start, 0);
            double[] endArr = Dashboard.fieldPane.poseToDisplay(end, 0);
            double[] bufferStartArr = Dashboard.fieldPane.poseToDisplay(start.addVector(new Vector2D(-rBuffer, rBuffer, true)), 0);
            double[] bufferEndArr = Dashboard.fieldPane.poseToDisplay(end.addVector(new Vector2D(rBuffer, -rBuffer, true)), 0);

            buffer.setX(bufferStartArr[0]);
            buffer.setY(bufferStartArr[1]);
            buffer.setWidth(bufferEndArr[0] - bufferStartArr[0]);
            buffer.setHeight(bufferEndArr[1] - bufferStartArr[1]);
            rect.setX(startArr[0]);
            rect.setY(startArr[1]);
            rect.setWidth(endArr[0] - startArr[0]);
            rect.setHeight(endArr[1] - startArr[1]);
            centerDot.setCenterX(MathUtils.halfway(startArr[0], endArr[0]));
            centerDot.setCenterY(MathUtils.halfway(startArr[1], endArr[1]));

            obstacle.set(start, end);
        }
    }

    public static class Circle extends ObstacleObj {

        public javafx.scene.shape.Circle circle;
        public javafx.scene.shape.Circle buffer;

        public Circle(ID id, Pose start) {
            super(id, start);
        }
        public Circle(ID id, Pose start, Pose end) {
            super(id, start, end);
        }
        public Circle(ID id, JSONObject obj) {
            super(id, obj);
        }

        @Override
        public void createObstacle() {
            this.obstacle = new Obstacle.Circle(start, end);
        }

        @Override
        public void createDisplayGroup() {
            double[] startArr = Dashboard.fieldPane.poseToDisplay(start, 0);
            double[] endArr = Dashboard.fieldPane.poseToDisplay(end, 0);
            double[] bufferEndArr = Dashboard.fieldPane.poseToDisplay(start.addVector(new Vector2D(start.distanceTo(end) + rBuffer, 0, false)), 0);

            buffer = new javafx.scene.shape.Circle();
            buffer.setCenterX(startArr[0]);
            buffer.setCenterY(startArr[1]);
            buffer.setRadius(MathUtils.distance(startArr[0], startArr[1], bufferEndArr[0], bufferEndArr[1]));
            buffer.setStroke(Color.GRAY);
            buffer.setStrokeWidth(2);
            buffer.setFill(new Color(Color.GRAY.getRed(), Color.GRAY.getGreen(), Color.GRAY.getBlue(), 0.3));
            displayGroup.getChildren().add(buffer);

            circle = new javafx.scene.shape.Circle();
            circle.setCenterX(startArr[0]);
            circle.setCenterY(startArr[1]);
            circle.setRadius(MathUtils.distance(startArr[0], startArr[1], endArr[0], endArr[1]));
            Color color = (id.contains("fixed") ? Color.YELLOW : Color.RED);
            circle.setStroke(color);
            circle.setStrokeWidth(2);
            circle.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.3));
            displayGroup.getChildren().add(circle);

            centerDot = new javafx.scene.shape.Circle();
            centerDot.setCenterX(startArr[0]);
            centerDot.setCenterY(startArr[1]);
            centerDot.setRadius(1.25 * Constants.getDouble("dashboard.gui.sizes.planningPoint"));
            centerDot.setFill(Color.WHITE);
            displayGroup.getChildren().add(centerDot);
        }

        @Override
        public void refreshDisplayGroup() {
            double[] startArr = Dashboard.fieldPane.poseToDisplay(start, 0);
            double[] endArr = Dashboard.fieldPane.poseToDisplay(end, 0);
            double[] bufferEndArr = Dashboard.fieldPane.poseToDisplay(start.addVector(new Vector2D(start.distanceTo(end) + rBuffer, 0, false)), 0);

            buffer.setCenterX(startArr[0]);
            buffer.setCenterY(startArr[1]);
            buffer.setRadius(MathUtils.distance(startArr[0], startArr[1], bufferEndArr[0], bufferEndArr[1]));
            circle.setCenterX(startArr[0]);
            circle.setCenterY(startArr[1]);
            circle.setRadius(MathUtils.distance(startArr[0], startArr[1], endArr[0], endArr[1]));
            centerDot.setCenterX(startArr[0]);
            centerDot.setCenterY(startArr[1]);

            obstacle.set(start, end);
        }
    }

}
