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

    public Piecewise translationalVelocityProfile;
    public Piecewise translationalAccelerationProfile;

    public MotionProfile(SplineTrajectory spline) {
        this.spline = spline;
        constants = spline.constants;
        translationalVelocityProfile = new Piecewise();
        translationalAccelerationProfile = new Piecewise();
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
            double dX = spline.distanceX.evaluate(p.distance, 1);
            double dY = spline.distanceY.evaluate(p.distance, 1);
            p.translationalVelocity = new Vector2D(constants.MAX_TRANSLATIONAL_VELOCITY, Utils.normalizeTheta(Math.atan2(dY, dX), 0, 2 * Math.PI), false);
        }
        spline.planningPoints.get(0).translationalVelocity.setMagnitude(0);
    }

    // Establish forward consistency among velocities of planning points based on maximum acceleration (accelerational constraints)
    public void forwardConsistency() {
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            RigidBody p0 = spline.planningPoints.get(i);
            RigidBody p1 = spline.planningPoints.get(i + 1);
            p1.translationalVelocity.setMagnitude(Math.min(constants.MAX_TRANSLATIONAL_VELOCITY, Math.sqrt(Math.pow(p0.translationalVelocity.magnitude, 2) + 2 * constants.MAX_TRANSLATIONAL_ACCELERATION * (p1.distance - p0.distance))));
        }
    }

    // Establish backward consistency among velocities of planning points based on maximum deceleration (accelerational constraints)
    public void backwardConsistency() {
        spline.planningPoints.get(spline.planningPoints.size() - 1).translationalVelocity.setMagnitude(0);
        for (int i = spline.planningPoints.size() - 1; i > 0; i--) {
            RigidBody p1 = spline.planningPoints.get(i);
            RigidBody p0 = spline.planningPoints.get(i - 1);
            p0.translationalVelocity.setMagnitude(Math.min(p0.translationalVelocity.magnitude, Math.sqrt(Math.pow(p1.translationalVelocity.magnitude, 2) + 2 * constants.MAX_TRANSLATIONAL_DECELERATION * (p1.distance - p0.distance))));
        }

        spline.planningPoints.get(0).translationalVelocity.setMagnitude(0);
        spline.planningPoints.get(spline.planningPoints.size() - 1).translationalVelocity.setMagnitude(0);
    }

    // Calculate accelerations on planning point intervals
    public void getAccelerations() {
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            RigidBody p0 = spline.planningPoints.get(i);
            RigidBody p1 = spline.planningPoints.get(i + 1);
            double maxAcceleration = Math.min(constants.MAX_TRANSLATIONAL_ACCELERATION, (Math.pow(p1.translationalVelocity.magnitude, 2) - Math.pow(p0.translationalVelocity.magnitude, 2)) / (2 * (p1.distance - p0.distance)));
            p0.translationalAcceleration.setMagnitude((p1.translationalVelocity.magnitude < p0.translationalVelocity.magnitude ? -1 : 1) * maxAcceleration);
        }

        spline.planningPoints.get(spline.planningPoints.size() - 1).translationalAcceleration.setMagnitude(0);
    }

    // Generate piecewise acceleration profile
    public void generateAccelerationProfile() {
        translationalAccelerationProfile = new Piecewise();
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            setTAPInterval(translationalAccelerationProfile, spline.planningPoints.get(i), spline.planningPoints.get(i + 1));
        }
        setTAPInterval(translationalAccelerationProfile, spline.planningPoints.get(spline.planningPoints.size() - 2), spline.planningPoints.get(spline.planningPoints.size() - 1));
    }

    // Set an interval in the translational acceleration profile
    public void setTAPInterval(Piecewise profile, RigidBody pp0, RigidBody pp1) {
        double slope = Utils.slope(pp0.distance, pp0.translationalAcceleration.magnitude, pp1.distance, pp1.translationalAcceleration.magnitude);
        profile.setInterval(pp0.distance, pp1.distance, "(" + slope + ")*d + (" + -(slope * pp0.distance) + ") + (" + pp0.translationalAcceleration.magnitude + ")");
    }

    // Generate piecewise velocity profile
    public void generateVelocityProfile() {
        translationalVelocityProfile = new Piecewise();
        for (int i = 0; i < spline.planningPoints.size() - 1; i++) {
            setTVPInterval(translationalVelocityProfile, spline.planningPoints.get(i), spline.planningPoints.get(i + 1));
        }
        setTVPInterval(translationalVelocityProfile, spline.planningPoints.get(spline.planningPoints.size() - 2), spline.planningPoints.get(spline.planningPoints.size() - 1));
    }

    // Set an interval in the translational velocity profile
    public void setTVPInterval(Piecewise profile, RigidBody pp0, RigidBody pp1) {
        double slope = Utils.slope(pp0.distance, pp0.translationalVelocity.magnitude, pp1.distance, pp1.translationalVelocity.magnitude);
        profile.setInterval(pp0.distance, pp1.distance, "(" + slope + ")*d + (" + -(slope * pp0.distance) + ") + (" + pp0.translationalVelocity.magnitude + ")");
    }

    public Vector2D getTranslationalVelocity(double distance) {
        double dX = spline.distanceX.evaluate(distance, 1);
        double dY = spline.distanceY.evaluate(distance, 1);
        return new Vector2D(translationalVelocityProfile.evaluate(distance, 0), Utils.normalizeTheta(Math.atan2(dY, dX), 0, 2 * Math.PI), false);
    }

    public Vector2D getTranslationalAcceleration(double distance) {
        double dX = spline.distanceX.evaluate(distance, 2);
        double dY = spline.distanceY.evaluate(distance, 2);
        return new Vector2D(translationalAccelerationProfile.evaluate(distance, 0), Utils.normalizeTheta(Math.atan2(dY, dX), 0, 2 * Math.PI), false);
    }

    public double getAngularVelocity(double distance) {
        return 0;
    }

    public double getAngularAcceleration(double distance) {
        return 0;
    }

    public RigidBody getPlanningPoint(double distance) {
        RigidBody toReturn = new RigidBody(distance);
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
                translationalAccelerationProfile.setInterval(intervalObj.getDouble("a"), intervalObj.getDouble("b"), intervalObj.getString("exp"));
            }

            translationalVelocityProfile = new Piecewise();
            for (int i = 0; i < translationalVelocityProfileArr.length(); i++) {
                JSONObject intervalObj = translationalVelocityProfileArr.getJSONObject(i);
                translationalVelocityProfile.setInterval(intervalObj.getDouble("a"), intervalObj.getDouble("b"), intervalObj.getString("exp"));
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
