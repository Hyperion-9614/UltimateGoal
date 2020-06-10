package com.hyperion.motion.trajectory;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Piecewise;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;

/**
 * Full feed-forward motion profile
 * Essentially, input: distance along path, output: expected pose, velocity, & acceleration
 * References:
 * (1) http://www2.informatik.uni-freiburg.de/~lau/students/Sprunk2008.pdf
 * (2) https://www.math.usm.edu/lambers/mat169/fall09/lecture32.pdf
 */

public class MotionProfile {

    public SplineTrajectory spline;

    public Piecewise transVelProfile;

    public MotionProfile(SplineTrajectory spline) {
        this.spline = spline;
        transVelProfile = new Piecewise();
    }

    public void recreate() {
        initializePlanningPoints();
        forwardTransAccConsistency();
        backwardTransAccConsistency();
        capTransVels();
        computeTransAccs();
        generateTransVelProfile();
    }

    // Compute maximum velocity based on isolated constraints (i.e. curvature, obstacles, etc.)
    public double getIsolatedMaxVelocity(double distance) {
        // Based on maximum angular velocity & curvature
        double tVMax = Constants.getDouble("motionProfile.maxes.aVel") / Math.abs(spline.getCurvature(distance));

        return tVMax;
    }

    // Initialize planning points with max isolated velocity
    public void initializePlanningPoints() {
        for (RigidBody p : spline.planningPoints) {
            double dX = spline.distanceX.evaluate(p.distance, 1, true);
            double dY = spline.distanceY.evaluate(p.distance, 1, true);
            p.tVel = new Vector2D(getIsolatedMaxVelocity(p.distance), MathUtils.norm(Math.atan2(dY, dX)), false);
        }
        printPlanningPoints("init");
    }

    // Establish forward consistency among velocities of planning points based on maximum acceleration (accelerational constraints)
    public void forwardTransAccConsistency() {
        spline.planningPoints.get(0).tVel.setMagnitude(0);
        for (int i = 1; i < spline.planningPoints.size() - 1; i++) {
            RigidBody p0 = spline.planningPoints.get(i - 1);
            RigidBody p1 = spline.planningPoints.get(i);

            double vMinAt = 0;
            if (Math.pow(p0.tVel.magnitude, 2) > 2 * Constants.getDouble("motionProfile.maxes.tAcc") * (p1.distance - p0.distance))
                vMinAt = Math.sqrt(Math.pow(p0.tVel.magnitude, 2) - 2 * Constants.getDouble("motionProfile.maxes.tAcc") * (p1.distance - p0.distance));
            double vMaxAt = Math.sqrt(Math.pow(p0.tVel.magnitude, 2) + 2 * Constants.getDouble("motionProfile.maxes.tAcc") * (p1.distance - p0.distance));
            p1.tVel.setMagnitude(MathUtils.clip(p1.tVel.magnitude, vMinAt, vMaxAt));
        }
        printPlanningPoints("for");
    }

    // Establish backward consistency among velocities of planning points based on maximum acceleration (accelerational constraints)
    public void backwardTransAccConsistency() {
        spline.planningPoints.get(spline.planningPoints.size() - 1).tVel.setMagnitude(0);
        for (int i = spline.planningPoints.size() - 2; i > 0; i--) {
            RigidBody p0 = spline.planningPoints.get(i + 1);
            RigidBody p1 = spline.planningPoints.get(i);

            double vMinAt = 0;
            if (Math.pow(p0.tVel.magnitude, 2) > 2 * Constants.getDouble("motionProfile.maxes.tAcc") * (p0.distance - p1.distance))
                vMinAt = Math.sqrt(Math.pow(p0.tVel.magnitude, 2) - 2 * Constants.getDouble("motionProfile.maxes.tAcc") * (p0.distance - p1.distance));
            double vMaxAt = Math.sqrt(Math.pow(p0.tVel.magnitude, 2) + 2 * Constants.getDouble("motionProfile.maxes.tAcc") * (p0.distance - p1.distance));
            p1.tVel.setMagnitude(MathUtils.clip(p1.tVel.magnitude, vMinAt, vMaxAt));
        }
        printPlanningPoints("back");
    }

    // Cap translational velocities at the given constant
    public void capTransVels() {
        for (RigidBody p : spline.planningPoints) {
            p.tVel.setMagnitude(Math.min(p.tVel.magnitude, Constants.getDouble("motionProfile.maxes.tVel")));
        }
        printPlanningPoints("cap");
    }

    // Compute translational accelerations on planning point intervals given final velocities
    public void computeTransAccs() {
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            RigidBody p0 = spline.planningPoints.get(i);
            RigidBody p1 = spline.planningPoints.get(i + 1);
            double acc = (Math.pow(p1.tVel.magnitude, 2) - Math.pow(p0.tVel.magnitude, 2)) / (2 * (p1.distance - p0.distance));
            p0.tAcc.setMagnitude(acc);
            p0.tAcc.setTheta(p0.tVel.theta + (acc < 0 ? Math.PI : 0));
        }
        spline.planningPoints.get(spline.planningPoints.size() - 1).tAcc.setMagnitude(0);
    }

    // Generate piecewise velocity profile
    public void generateTransVelProfile() {
        transVelProfile = new Piecewise();
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            setTVPInterval(transVelProfile, spline.planningPoints.get(i), spline.planningPoints.get(i + 1));
        }
        setTVPInterval(transVelProfile, spline.planningPoints.get(spline.planningPoints.size() - 2), spline.planningPoints.get(spline.planningPoints.size() - 1));
    }

    // Set an interval in the translational velocity profile
    public void setTVPInterval(Piecewise profile, RigidBody pp0, RigidBody pp1) {
        double slope = MathUtils.slope(pp0.distance, pp0.tVel.magnitude, pp1.distance, pp1.tVel.magnitude);
        profile.setInterval(pp0.distance, pp1.distance, "(" + slope + ")*t + (" + -(slope * pp0.distance) + ") + (" + pp0.tVel.magnitude + ")");
    }

    public Vector2D getTransVel(double distance) {
        double paramD = spline.paramDistance(distance);
        double dX = spline.distanceX.evaluate(paramD, 1, true);
        double dY = spline.distanceY.evaluate(paramD, 1, true);
        return new Vector2D(transVelProfile.evaluate(distance, 0, true), MathUtils.norm(Math.atan2(dY, dX)), false);
    }

    public Vector2D getTransAcc(double distance) {
        RigidBody floored = spline.planningPoints.get(0);
        for (int i = spline.planningPoints.size() - 1; i >= 0; i--) {
            if (spline.planningPoints.get(i).distance <= distance) {
                floored = spline.planningPoints.get(i);
            }
        }
        return floored.tAcc;
    }

    public void printPlanningPoints(String label) {
        System.out.println(label);
        for (RigidBody p : spline.planningPoints)
            System.out.println(p.tVel);
        System.out.println();
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
