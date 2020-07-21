package com.hyperion.motion.math;

import com.hyperion.common.*;

/**
 * 2D vector object
 */

public class Vector2D {

    public double x, y;
    public double theta;
    public double magnitude;

    public Vector2D() {

    }

    public Vector2D(Vector2D vector2D) {
        setVec(vector2D);
    }

    public Vector2D(double a, double b, boolean isXY) {
        if (isXY) {
            x = a;
            y = b;

            magnitude = MathUtils.round(Math.sqrt(x * x + y * y), 3);
            theta = MathUtils.norm(Math.atan2(y, x));
        } else {
            magnitude = Math.abs(MathUtils.round(a, 3));
            theta = MathUtils.norm(b);

            x = magnitude * Math.cos(theta);
            y = magnitude * Math.sin(theta);
        }
    }

    public Vector2D(Pose a, Pose b) {
        this(b.x - a.x, b.y - a.y, true);
    }

    public Vector2D added(Vector2D v2) {
        return new Vector2D(x + v2.x, y + v2.y, true);
    }

    public void add(Vector2D v2) {
        this.x += v2.x;
        this.y += v2.y;
        this.magnitude = MathUtils.round(Math.sqrt(x * x + y * y), 3);
        this.theta = MathUtils.norm(Math.atan2(y, x));
    }

    public Vector2D subtracted(Vector2D v2) {
        return new Vector2D(x - v2.x, y - v2.y, true);
    }

    public Vector2D scaled(double k) {
        return new Vector2D(k * magnitude, theta, false);
    }

    public Vector2D rotated(double dTheta) {
        return new Vector2D(magnitude, theta + dTheta, false);
    }

    public double dot(Vector2D vec) {
        return x * vec.x + y * vec.y;
    }

    public Vector2D[] normals() {
        return new Vector2D[]{ rotated(-Math.PI / 2), rotated(Math.PI / 2) };
    }

    public Vector2D magnituded(double newMag) {
        return new Vector2D(newMag * Math.cos(theta), newMag * Math.sin(theta), true);
    }

    public Vector2D unit() {
        return magnituded(1);
    }

    public void setMagnitude(double newMag) {
        setVec(new Vector2D(newMag, theta, false));
    }

    public void setTheta(double newTheta) {
        setVec(thetaed(newTheta));
    }

    public Vector2D thetaed(double newTheta) {
        return new Vector2D(magnitude, newTheta, false);
    }

    public void rotate(double dTheta) {
        setTheta(theta + dTheta);
    }

    public double[] toArray() {
        return new double[]{ magnitude, theta };
    }

    public void setVec(Vector2D newVec) {
        x = newVec.x;
        y = newVec.y;
        magnitude = newVec.magnitude;
        theta = newVec.theta;
    }

    public boolean equals(Vector2D other) {
        return (MathUtils.round(other.magnitude, 3) == MathUtils.round(magnitude, 3)) && (MathUtils.round(MathUtils.norm(other.theta, 0, 2 * Math.PI), 3) == MathUtils.round(MathUtils.norm(theta, 0, 2 * Math.PI), 3));
    }

    @Override
    public String toString() {
        return MathUtils.round(magnitude, 3) + ", " + MathUtils.round(Math.toDegrees(theta), 3) + "°";
    }

}
