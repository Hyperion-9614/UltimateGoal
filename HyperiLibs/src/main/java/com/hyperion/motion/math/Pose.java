package com.hyperion.motion.math;

import com.hyperion.common.MathUtils;

/**
 * Orientation object for global positioning, pathing, etc.
 */

public class Pose {

    public double x;
    public double y;
    public double theta = 0;

    public Pose() {

    }

    public Pose(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Pose(Pose copyFrom) {
        this.x = copyFrom.x;
        this.y = copyFrom.y;
        this.theta = copyFrom.theta;
    }

    public Pose(double[] coords) {
        this.x = coords[0];
        this.y = coords[1];
    }

    public Pose(double x, double y, double theta) {
        this.x = x;
        this.y = y;
        this.theta = theta;
    }

    public void setXY(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setT(double theta) {
        this.theta = theta;
    }

    public void setXYT(double x, double y, double theta) {
        this.x = x;
        this.y = y;
        this.theta = theta;
    }

    public void addXY(double dX, double dY) {
        this.x += dX;
        this.y += dY;
    }

    public void addXYT(double dX, double dY, double dTheta) {
        this.x += dX;
        this.y += dY;
        this.theta += dTheta;
    }

    public double distanceTo(Pose o1) {
        return Math.sqrt(Math.pow(this.x - o1.x, 2) + Math.pow(this.y - o1.y, 2));
    }

    public void setPose(Pose newPose) {
        this.x = newPose.x;
        this.y = newPose.y;
        this.theta = newPose.theta;
    }

    public Pose addVector(Vector2D vec) {
        return new Pose(this.x + vec.x, this.y + vec.y, this.theta);
    }

    public double[] toArray() {
        return new double[] { x, y, theta};
    }

    public boolean equals(Pose other) {
        return x == other.x && y == other.y && theta == other.theta;
    }

    @Override
    public String toString() {
        return "X: " + MathUtils.round(x, 2) + " | Y: " + MathUtils.round(y, 2)
                + " | Theta: " + MathUtils.round(Math.toDegrees(theta), 2) + "\u00B0";
    }

}
