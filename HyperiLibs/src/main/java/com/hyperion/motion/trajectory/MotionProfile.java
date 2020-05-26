package com.hyperion.motion.trajectory;

import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;

/**
 * Full feed-forward motion profile
 * Essentially, input: distance along path, output: expected pose, velocity, & acceleration
 * References:
 * (1)
 */

public class MotionProfile {

    public SplineTrajectory spline;

    public MotionProfile(SplineTrajectory spline) {
        this.spline = spline;
    }

    public void recreate() {
        initializePlanningPoints();
    }

    // Initialize planning points with max velocity and corresponding acceleration
    public void initializePlanningPoints() {
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            RigidBody p = spline.planningPoints.get(i);
            p.tVel = new Vector2D();
        }
        spline.planningPoints.get(0).tVel.setMagnitude(0);
    }

    public Vector2D getTransVel(double distance) {
        return new Vector2D();
    }

    public Vector2D getTransAcc(double distance) {
        return new Vector2D();
    }

    public double getAngVel(double distance) {
        return 0;
    }

    public double getAngAcc(double distance) {
        return 0;
    }

    public RigidBody getRigidBody(double distance) {
        RigidBody toReturn = new RigidBody(distance);
        toReturn.setPose(spline.getDPose(distance));
        toReturn.tVel = getTransVel(distance);
        toReturn.tAcc = getTransAcc(distance);
        toReturn.aVel = getAngVel(distance);
        toReturn.aAcc = getAngAcc(distance);
        return toReturn;
    }

}
