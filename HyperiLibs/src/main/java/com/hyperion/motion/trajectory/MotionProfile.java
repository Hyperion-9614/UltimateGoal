package com.hyperion.motion.trajectory;

import com.hyperion.common.Constants;
import com.hyperion.common.Utils;
import com.hyperion.motion.math.Piecewise;
import com.hyperion.motion.math.PlanningPoint;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.trajectory.SplineTrajectory;

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

    public Piecewise translationalVelocityProfile;
    public Piecewise translationalAccelerationProfile;

    public MotionProfile(SplineTrajectory spline) {
        this.spline = spline;
        constants = spline.constants;
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
            PlanningPoint p = spline.planningPoints.get(i);
            double[] derivative = spline.getDerivative(p.distance, 1);
            p.translationalVelocity = new Vector2D(constants.MAX_TRANSLATIONAL_VELOCITY, Utils.normalizeTheta(Math.atan2(derivative[1], derivative[0]), 0, 2 * Math.PI), false);
        }
        spline.planningPoints.get(0).translationalVelocity.setMagnitude(0);
    }

    // Establish forward consistency among velocities of planning points based on maximum acceleration (accelerational constraints)
    public void forwardConsistency() {
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            PlanningPoint p0 = spline.planningPoints.get(i);
            PlanningPoint p1 = spline.planningPoints.get(i + 1);
            p1.translationalVelocity.setMagnitude(Math.min(constants.MAX_TRANSLATIONAL_VELOCITY, Math.sqrt(Math.pow(p0.translationalVelocity.magnitude, 2) + 2 * constants.MAX_TRANSLATIONAL_ACCELERATION * (p1.distance - p0.distance))));
        }
    }

    // Establish backward consistency among velocities of planning points based on maximum deceleration (accelerational constraints)
    public void backwardConsistency() {
        spline.planningPoints.get(spline.planningPoints.size() - 1).translationalVelocity.setMagnitude(0);
        for (int i = spline.planningPoints.size() - 1; i > 0; i--) {
            PlanningPoint p1 = spline.planningPoints.get(i);
            PlanningPoint p0 = spline.planningPoints.get(i - 1);
            p0.translationalVelocity.setMagnitude(Math.min(p0.translationalVelocity.magnitude, Math.sqrt(Math.pow(p1.translationalVelocity.magnitude, 2) + 2 * constants.MAX_TRANSLATIONAL_DECELERATION * (p1.distance - p0.distance))));
        }
    }

    // Calculate accelerations on planning point intervals
    public void getAccelerations() {
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            PlanningPoint p0 = spline.planningPoints.get(i);
            PlanningPoint p1 = spline.planningPoints.get(i + 1);
            double maxAcceleration = Math.min(constants.MAX_TRANSLATIONAL_ACCELERATION, (Math.pow(p1.translationalVelocity.magnitude, 2) - Math.pow(p0.translationalVelocity.magnitude, 2)) / (2 * (p1.distance - p0.distance)));
            p0.translationalAcceleration.setMagnitude((p1.translationalVelocity.magnitude < p0.translationalVelocity.magnitude ? -1 : 1) * maxAcceleration);
        }
    }

    // Generate piecewise acceleration profile
    public void generateAccelerationProfile() {
        translationalAccelerationProfile = new Piecewise();
        int firstPPi = 0;
        for (int i = 1; i < spline.planningPoints.size(); i++) {
            if (i > 1 && !spline.planningPoints.get(i).translationalAcceleration.equals(spline.planningPoints.get(firstPPi).translationalAcceleration)) {
                translationalAccelerationProfile.setLTAInterval(spline.planningPoints.get(firstPPi), spline.planningPoints.get(i - 1));
                firstPPi = i - 1;
            }
        }
        translationalAccelerationProfile.setLTAInterval(spline.planningPoints.get(firstPPi), spline.planningPoints.get(spline.planningPoints.size() - 1));
    }

    // Generate piecewise velocity profile based on accelerations & velocities of planning points
    public void generateVelocityProfile() {
        translationalVelocityProfile = new Piecewise();
        int firstPPi = 0;
        for (int i = 1; i < spline.planningPoints.size(); i++) {
            if (i > 1 && !spline.planningPoints.get(i).translationalAcceleration.equals(spline.planningPoints.get(firstPPi).translationalAcceleration)) {
                translationalVelocityProfile.setLTVInterval(spline.planningPoints.get(firstPPi), spline.planningPoints.get(i - 1));
                firstPPi = i - 1;
            }
        }
        translationalVelocityProfile.setLTVInterval(spline.planningPoints.get(firstPPi), spline.planningPoints.get(spline.planningPoints.size() - 1));
    }

    public Vector2D getTranslationalVelocity(double distance) {
        return translationalVelocityProfile.evaluate(distance, spline);
    }

    public Vector2D getTranslationalAcceleration(double distance) {
        return translationalAccelerationProfile.evaluate(distance, spline);
    }

    public double getAngularVelocity(double distance) {
        return 0;
    }

    public double getAngularAcceleration(double distance) {
        return 0;
    }

    public PlanningPoint getPlanningPoint(double distance) {
        PlanningPoint toReturn = new PlanningPoint(distance);
        toReturn.pose = spline.getPoseFromDistance(distance);
        toReturn.translationalVelocity = getTranslationalVelocity(distance);
        toReturn.translationalAcceleration = getTranslationalAcceleration(distance);
        toReturn.angularVelocity = getAngularVelocity(distance);
        toReturn.angularAcceleration = getAngularAcceleration(distance);
        return toReturn;
    }

    public void fromJSON(JSONObject obj) {
        try {
            JSONObject motionProfile = obj.getJSONObject("motionProfile");
            JSONArray translationalAccelerationProfileArr = motionProfile.getJSONArray("translationalAccelerationProfile");
            JSONArray translationalVelocityProfileArr = motionProfile.getJSONArray("translationalVelocityProfile");

            translationalAccelerationProfile = new Piecewise();
            for (int i = 0; i < translationalAccelerationProfileArr.length(); i++) {
                JSONObject intervalObj = translationalAccelerationProfileArr.getJSONObject(i);
                translationalAccelerationProfile.setInterval(intervalObj.getDouble("d0"), intervalObj.getDouble("d1"), intervalObj.getString("expression"));
            }

            translationalVelocityProfile = new Piecewise();
            for (int i = 0; i < translationalVelocityProfileArr.length(); i++) {
                JSONObject intervalObj = translationalVelocityProfileArr.getJSONObject(i);
                translationalVelocityProfile.setInterval(intervalObj.getDouble("d0"), intervalObj.getDouble("d1"), intervalObj.getString("expression"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("translationalAccelerationProfile", translationalAccelerationProfile.toJSONArray());
            obj.put("translationalVelocityProfile", translationalVelocityProfile.toJSONArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

}
