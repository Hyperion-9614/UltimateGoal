package com.hyperion.motion.math;

import com.hyperion.common.Utils;
import com.hyperion.motion.trajectory.SplineTrajectory;

public class RigidBody {

    public double T = 0;
    public double distance = 0;
    public Pose pose;
    public Vector2D translationalVelocity = new Vector2D();
    public Vector2D translationalAcceleration = new Vector2D();
    public double angularVelocity = 0;
    public double angularAcceleration = 0;

    public RigidBody(double distance) {
        this.distance = distance;
    }

    public RigidBody(Pose pose) {
        this.pose = pose;
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
        this.translationalVelocity = new Vector2D(rigidBody.translationalVelocity);
        this.translationalAcceleration = new Vector2D(rigidBody.translationalAcceleration);
        this.angularVelocity = rigidBody.angularVelocity;
        this.angularAcceleration = rigidBody.angularAcceleration;
    }

    public double[] toArray() {
        return Utils.combineArrs(new double[]{ T, distance }, pose.toArray());
    }

    @Override
    public String toString() {
        return pose.toString() + " | tVel: " + translationalVelocity + " | tAcc: " + translationalAcceleration + " | aVel: " + angularVelocity + " | aAcc: " + angularAcceleration;
    }

}