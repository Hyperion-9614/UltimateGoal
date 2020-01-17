package com.hyperion.motion.trajectory;

import com.hyperion.common.Constants;
import com.hyperion.common.Utils;
import com.hyperion.motion.math.Piecewise;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Full feed-forward motion profile
 * Essentially, input: current distance along path, output: expected pose, velocity, & acceleration
 * References:
 * (1) http://www2.informatik.uni-freiburg.de/~lau/students/Sprunk2008.pdf
 */

public class MotionProfile {

    public SplineTrajectory spline;
    public Constants constants;

    public Piecewise tVelProfile;
    public Piecewise tAccProfile;

    public MotionProfile(SplineTrajectory spline) {
        this.spline = spline;
        constants = spline.constants;
        tVelProfile = new Piecewise();
        tAccProfile = new Piecewise();
    }

    public void recreate() {
        initializePlanningPoints();
        forwardConsistency();
        backwardConsistency();
        getAccelerations();
        generateAccelerationProfile();
        generateVelocityProfile();
    }

    // Initialize planning points with max velocity and corresponding acceleration
    public void initializePlanningPoints() {
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            RigidBody p = spline.planningPoints.get(i);
            double dX = spline.distanceX.evaluate(p.distance, 1, true);
            double dY = spline.distanceY.evaluate(p.distance, 1, true);
            p.tVel = new Vector2D(constants.MAX_TRANSLATIONAL_VELOCITY, Utils.normalizeTheta(Math.atan2(dY, dX), 0, 2 * Math.PI), false);
        }
        spline.planningPoints.get(0).tVel.setMagnitude(0);
    }

    // Establish forward consistency among velocities of planning points based on maximum acceleration (accelerational constraints)
    public void forwardConsistency() {
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            RigidBody p0 = spline.planningPoints.get(i);
            RigidBody p1 = spline.planningPoints.get(i + 1);
            p1.tVel.setMagnitude(Math.min(constants.MAX_TRANSLATIONAL_VELOCITY, Math.sqrt(Math.pow(p0.tVel.magnitude, 2) + 2 * constants.MAX_TRANSLATIONAL_ACCELERATION * (p1.distance - p0.distance))));
        }
    }

    // Establish backward consistency among velocities of planning points based on maximum deceleration (accelerational constraints)
    public void backwardConsistency() {
        spline.planningPoints.get(spline.planningPoints.size() - 1).tVel.setMagnitude(0);
        for (int i = spline.planningPoints.size() - 1; i > 0; i--) {
            RigidBody p1 = spline.planningPoints.get(i);
            RigidBody p0 = spline.planningPoints.get(i - 1);
            p0.tVel.setMagnitude(Math.min(p0.tVel.magnitude, Math.sqrt(Math.pow(p1.tVel.magnitude, 2) + 2 * constants.MAX_TRANSLATIONAL_ACCELERATION * (p1.distance - p0.distance))));
        }

        spline.planningPoints.get(0).tVel.setMagnitude(0);
        spline.planningPoints.get(spline.planningPoints.size() - 1).tVel.setMagnitude(0);
    }

    // Calculate accelerations on planning point intervals
    public void getAccelerations() {
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            RigidBody p0 = spline.planningPoints.get(i);
            RigidBody p1 = spline.planningPoints.get(i + 1);
            double maxAcceleration = Math.min(constants.MAX_TRANSLATIONAL_ACCELERATION, Math.abs(Math.pow(p1.tVel.magnitude, 2) - Math.pow(p0.tVel.magnitude, 2)) / (2 * (p1.distance - p0.distance)));
            p0.tAcc.setMagnitude(maxAcceleration);
            p0.tAcc.setTheta(p0.tVel.theta + (p1.tVel.magnitude < p0.tVel.magnitude ? Math.PI : 0));
        }

        spline.planningPoints.get(spline.planningPoints.size() - 1).tAcc.setMagnitude(0);
    }

    // Generate piecewise acceleration profile
    public void generateAccelerationProfile() {
        tAccProfile = new Piecewise();
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            setTAPInterval(tAccProfile, spline.planningPoints.get(i), spline.planningPoints.get(i + 1));
        }
        setTAPInterval(tAccProfile, spline.planningPoints.get(spline.planningPoints.size() - 2), spline.planningPoints.get(spline.planningPoints.size() - 1));
    }

    // Set an interval in the translational acceleration profile
    public void setTAPInterval(Piecewise profile, RigidBody pp0, RigidBody pp1) {
        profile.setInterval(pp0.distance, pp1.distance, "(" + pp0.tAcc.magnitude + ")");
    }

    // Generate piecewise velocity profile
    public void generateVelocityProfile() {
        tVelProfile = new Piecewise();
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            setTVPInterval(tVelProfile, spline.planningPoints.get(i), spline.planningPoints.get(i + 1));
        }
        setTVPInterval(tVelProfile, spline.planningPoints.get(spline.planningPoints.size() - 2), spline.planningPoints.get(spline.planningPoints.size() - 1));
    }

    // Set an interval in the translational velocity profile
    public void setTVPInterval(Piecewise profile, RigidBody pp0, RigidBody pp1) {
        double slope = Utils.slope(pp0.distance, pp0.tVel.magnitude, pp1.distance, pp1.tVel.magnitude);
        profile.setInterval(pp0.distance, pp1.distance, "(" + slope + ")*t + (" + -(slope * pp0.distance) + ") + (" + pp0.tVel.magnitude + ")");
    }

    public Vector2D getTranslationalVelocity(double distance) {
        double paramD = spline.paramDistance(distance);
        double dX = spline.distanceX.evaluate(paramD, 1, true);
        double dY = spline.distanceY.evaluate(paramD, 1, true);
        return new Vector2D(tVelProfile.evaluate(distance, 0, true), Utils.normalizeTheta(Math.atan2(dY, dX), 0, 2 * Math.PI), false);
    }

    public Vector2D getTranslationalAcceleration(double distance) {
        RigidBody floored = spline.planningPoints.get(0);
        for (int i = spline.planningPoints.size() - 1; i >= 0; i--) {
            if (spline.planningPoints.get(i).distance <= distance) {
                floored = spline.planningPoints.get(i);
            }
        }
        return new Vector2D(tAccProfile.evaluate(distance, 0, true), floored.tAcc.theta, false);
    }

    public double getAngularVelocity(double distance) {
        return 0;
    }

    public double getAngularAcceleration(double distance) {
        return 0;
    }

    public RigidBody getRigidBody(double distance) {
        RigidBody toReturn = new RigidBody(distance);
        toReturn.pose = spline.getDPose(distance);
        toReturn.tVel = getTranslationalVelocity(distance);
        toReturn.tAcc = getTranslationalAcceleration(distance);
        toReturn.aVel = getAngularVelocity(distance);
        toReturn.aAcc = getAngularAcceleration(distance);
        return toReturn;
    }

}
