package com.hyperion.motion.math;

import com.hyperion.common.Utils;
import com.hyperion.motion.trajectory.SplineTrajectory;

import java.util.Scanner;

public class RigidBody {

    public double T = 0;
    public double distance = 0;
    public Pose pose;
    public Vector2D tVel = new Vector2D();
    public Vector2D tAcc = new Vector2D();
    public double aVel = 0;
    public double aAcc = 0;

    public RigidBody(double distance) {
        this.distance = distance;
    }

    public RigidBody(Pose pose) {
        this.pose = new Pose(pose);
    }

    public RigidBody(double T, double distance, Pose pose) {
        this.T = T;
        this.distance = distance;
        this.pose = pose;
    }

    public RigidBody(double T, double distance, double theta, SplineTrajectory sT) {
        this.T = T;
        this.pose = sT.getTPose(T);
        this.pose.setT(theta);
        this.distance = distance;
    }

    public RigidBody(RigidBody rigidBody) {
        this.T = rigidBody.T;
        this.distance = rigidBody.distance;
        this.pose = new Pose(rigidBody.pose);
        this.tVel = new Vector2D(rigidBody.tVel);
        this.tAcc = new Vector2D(rigidBody.tAcc);
        this.aVel = rigidBody.aVel;
        this.aAcc = rigidBody.aAcc;
    }

    public RigidBody(String str) {
        str = str.replaceAll("[^\\d. -]", "");
        Scanner scanner = new Scanner(str);
        this.pose = new Pose(scanner.nextDouble(), scanner.nextDouble(), Math.toRadians(scanner.nextDouble()));
        this.tVel = new Vector2D(scanner.nextDouble(), Math.toRadians(scanner.nextDouble()), false);
        this.tAcc = new Vector2D(scanner.nextDouble(), Math.toRadians(scanner.nextDouble()), false);
        this.aVel = scanner.nextDouble();
        this.aAcc = scanner.nextDouble();
        scanner.close();
    }

    public double[] toArray() {
        return Utils.combineArrs(new double[]{ T, distance }, pose.toArray());
    }

    @Override
    public String toString() {
        return pose.toString() + " | tVel: " + tVel + " | tAcc: " + tAcc + " | aVel: " + aVel + " | aAcc: " + aAcc;
    }

}