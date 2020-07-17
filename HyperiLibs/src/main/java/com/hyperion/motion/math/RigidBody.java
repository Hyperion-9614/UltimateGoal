package com.hyperion.motion.math;

import com.hyperion.common.ArrayUtils;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.trajectory.SplineTrajectory;

import org.json.JSONArray;

import java.util.Scanner;

public class RigidBody extends Pose {

    public double T = 0;
    public double distance = 0;
    public Vector2D tVel = new Vector2D();
    public Vector2D tAcc = new Vector2D();
    public double aVel = 0;
    public double aAcc = 0;

    public RigidBody(double distance) {
        this.distance = distance;
    }

    public RigidBody(Pose pose) {
        setPose(pose);
    }

    public RigidBody(double T, double distance, Pose pose) {
        this.T = T;
        this.distance = distance;
        setPose(pose);
    }

    public RigidBody(double distance, SplineTrajectory sT) {
        this(distance);
        setPose(sT.getDPose(distance));
        this.tVel = sT.mP.getTransVel(distance);
        this.tAcc = sT.mP.getTransAcc(distance);
        this.aVel = sT.mP.getAngVel(distance);
        this.aAcc = sT.mP.getAngAcc(distance);
    }

    public RigidBody(double T, double distance, double theta, SplineTrajectory sT) {
        this.T = T;
        setPose(sT.getTPose(T));
        this.theta = theta;
        this.distance = distance;
    }

    public RigidBody(RigidBody rigidBody) {
        this.T = rigidBody.T;
        this.distance = rigidBody.distance;
        setPose(rigidBody);
        this.tVel = new Vector2D(rigidBody.tVel);
        this.tAcc = new Vector2D(rigidBody.tAcc);
        this.aVel = rigidBody.aVel;
        this.aAcc = rigidBody.aAcc;
    }

    public RigidBody(JSONArray arr) {
        try {
            this.T = arr.getDouble(0);
            this.distance = arr.getDouble(1);
            this.x = arr.getDouble(2);
            this.y = arr.getDouble(3);
            this.theta = arr.getDouble(4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RigidBody(String str) {
        str = str.replaceAll("[^\\d. -]", "");
        Scanner scanner = new Scanner(str);
        this.x = scanner.nextDouble();
        this.y = scanner.nextDouble();
        this.theta = Math.toRadians(scanner.nextDouble());
        this.tVel = new Vector2D(scanner.nextDouble(), Math.toRadians(scanner.nextDouble()), false);
        this.tAcc = new Vector2D(scanner.nextDouble(), Math.toRadians(scanner.nextDouble()), false);
        this.aVel = scanner.nextDouble();
        this.aAcc = scanner.nextDouble();
        scanner.close();
    }

    public Pose getPose() {
        return new Pose(this.x, this.y, this.theta);
    }

    public double[] toArray() {
        return ArrayUtils.combineArrs(new double[]{ T, distance }, new double[]{ x, y, theta });
    }

    @Override
    public String toString() {
        return new Pose(x, y, theta).toString() + " | tVel: " + tVel + " | tAcc: " + tAcc +
                                                  " | aVel: " + MathUtils.round(Math.toDegrees(aVel), 3) +
                                                  "°/s | aAcc: " + MathUtils.round(Math.toDegrees(aAcc), 3) + " °/s²";
        }

}