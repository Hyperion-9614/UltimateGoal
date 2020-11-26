package com.hyperion.motion.pathplanning;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
//import com.hyperion.dashboard.Dashboard;
//import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.motion.math.Pose;

import org.json.JSONObject;

public abstract class Obstacle {

    public Pose pose;

    public abstract boolean contains(Pose other);
    public abstract void set(Pose start, Pose end);

    public static class Rect extends Obstacle {

        public double width;
        public double height;

        public Rect(Pose topLeft, double width, double height) {
            this.pose = topLeft;
            this.width = width;
            this.height = height;
        }

        public Rect(Pose topLeft, Pose bottomRight) {
            set(topLeft, bottomRight);
        }

        public Rect(JSONObject obj) {
            this(new Pose(obj.getJSONArray("start")), new Pose(obj.getJSONArray("end")));
        }

        @Override
        public boolean contains(Pose other) {
            return MathUtils.isInRange(other.x, pose.x, pose.x + width) &&
                   MathUtils.isInRange(other.y, pose.y - height, pose.y);
        }

        @Override
        public void set(Pose start, Pose end) {
            this.pose = start;
            this.width = end.x - start.x;
            this.height = start.y - end.y;
        }

    }

    public static class Circle extends Obstacle {

        public double rInner = Constants.getDouble("pathing.obstacles.rInner");
        public double rBuffer = Constants.getDouble("pathing.obstacles.rBuffer");

        public Circle(Pose pose) {
            this.pose = pose;
        }

        public Circle(Pose start, Pose end) {
            set(start, end);
        }

        public Circle(JSONObject obj) {
            this(new Pose(obj.getJSONArray("start")), new Pose(obj.getJSONArray("end")));
        }

        public Circle(Pose start, double rInner) {
            this.pose = start;
            this.rInner = rInner;
        }

        public Circle(Pose start, double rInner, double rBuffer) {
            this.pose = start;
            this.rInner = rInner;
            this.rBuffer = rBuffer;
        }

        public double radius() {
            return rInner + rBuffer;
        }

        @Override
        public boolean contains(Pose other) {
            return MathUtils.distance(pose, other) <= radius();
        }

        @Override
        public void set(Pose start, Pose end) {
            this.pose = start;
            this.rInner = start.distanceTo(end);
        }

    }

}
