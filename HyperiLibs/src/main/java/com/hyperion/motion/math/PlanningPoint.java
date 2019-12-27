package com.hyperion.motion.math;

import com.hyperion.common.Utils;
import com.hyperion.motion.trajectory.SplineTrajectory;

public class PlanningPoint {

    public double T = 0;
    public double distance = 0;
    public Pose pose;
    public Vector2D translationalVelocity = new Vector2D();
    public Vector2D translationalAcceleration = new Vector2D();
    public double angularVelocity = 0;
    public double angularAcceleration = 0;

    public PlanningPoint(double distance) {
        this.distance = distance;
    }

    public PlanningPoint(Pose pose) {
        this.pose = pose;
    }

    public PlanningPoint(double T, double distance, Pose pose) {
        this.T = T;
        this.distance = distance;
        this.pose = pose;
    }

    public PlanningPoint(double T, double distance, double theta, SplineTrajectory sT) {
        this.T = T;
        this.pose = sT.getPoseFromT(T);
        this.pose.setT(theta);
        this.distance = distance;
    }

    public PlanningPoint(PlanningPoint planningPoint) {
        this.T = planningPoint.T;
        this.distance = planningPoint.distance;
        this.pose = new Pose(planningPoint.pose);
        this.translationalVelocity = new Vector2D(planningPoint.translationalVelocity);
        this.translationalAcceleration = new Vector2D(planningPoint.translationalAcceleration);
        this.angularVelocity = planningPoint.angularVelocity;
        this.angularAcceleration = planningPoint.angularAcceleration;
    }

    public double[] toArray() {
        return Utils.combineArrs(new double[]{ T, distance }, pose.toArray());
    }

    @Override
    public String toString() {
        return pose.toString() + " | tVel: " + translationalVelocity + " | tAcc: " + translationalAcceleration + " | aVel: " + angularVelocity + " | aAcc: " + angularAcceleration;
    }

}