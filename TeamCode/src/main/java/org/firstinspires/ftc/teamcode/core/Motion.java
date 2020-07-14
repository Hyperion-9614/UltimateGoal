package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.ArrayUtils;
import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.common.MathUtils;
import com.hyperion.common.MiscUtils;
import com.hyperion.motion.dstarlite.DStarLite;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.trajectory.SplineTrajectory;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

import com.hyperion.motion.trajectory.PIDCtrl;
import org.firstinspires.ftc.teamcode.modules.Localizer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages all 2D robot translation & rotation
 */

public class Motion {

    public static Hardware hw;

    public static Localizer localizer;
    public static DStarLite dStarLite;

    public static RigidBody start = new RigidBody(new Pose(0, 0, 0));
    public static RigidBody robot = new RigidBody(start);
    public static Map<ID, Pose> waypoints = new HashMap<>();
    public static Map<ID, SplineTrajectory> splines = new HashMap<>();

    public static void init(Hardware hardware) {
        hw = hardware;
        try {
            JSONObject jsonObject = new JSONObject(IOUtils.readFile(hw.fieldJSON));
            readWaypoints(jsonObject);
            readSplines(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }

        localizer = new Localizer(hw);
        dStarLite = new DStarLite();

        for (DcMotor motor : hw.hwmp.dcMotor) {
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        hw.fLDrive.setDirection(DcMotorSimple.Direction.REVERSE);
        hw.bLDrive.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    //////////////////////// INIT ////////////////////////////

    // Read waypoints from field.json file
    public static void readWaypoints(JSONObject root) throws Exception {
        JSONObject wpObj = root.getJSONObject("waypoints");
        waypoints.clear();

        Iterator<String> keys = wpObj.keys();
        while (!hw.ctx.isStarted() && !hw.ctx.isStopRequested() && keys.hasNext()) {
            String key = keys.next();
            JSONArray waypointArray = wpObj.getJSONArray(key);
            Pose waypoint = new Pose(waypointArray.getDouble(0), waypointArray.getDouble(1), waypointArray.getDouble(2));
            waypoints.put(new ID(key), waypoint);
        }
    }

    // Read splines from field.json file
    public static void readSplines(JSONObject root) throws Exception {
        JSONObject splinesObj = root.getJSONObject("splines");
        splines.clear();

        Iterator<String> keys = splinesObj.keys();
        while (!hw.ctx.isStarted() && !hw.ctx.isStopRequested() && keys.hasNext()) {
            String key = keys.next();
            SplineTrajectory spline = new SplineTrajectory(splinesObj.getJSONObject(key));
            splines.put(new ID(key), spline);
        }
    }

    ///////////////////////// RAW MOTION & HELPERS /////////////////////////

    // Drive power setters
    public static void setDrive(double power) {
        setDrive(power, power);
    }
    public static void setDrive(double... powers) {
        if (powers.length == 4) {
            hw.fLDrive.setPower(powers[0]);
            hw.fRDrive.setPower(powers[1]);
            hw.bLDrive.setPower(powers[2]);
            hw.bRDrive.setPower(powers[3]);
        } else if (powers.length == 2) {
            hw.fLDrive.setPower(powers[0]);
            hw.bLDrive.setPower(powers[0]);
            hw.fRDrive.setPower(powers[1]);
            hw.bRDrive.setPower(powers[1]);
        }
    }
    public static void setDrive(double fLPower, double fRPower, double bLPower, double bRPower, long time) {
        setDrive(fLPower, fRPower, bLPower, bRPower);
        hw.ctx.sleep(time);
        setDrive(0);
    }
    public static void setDrive(Vector2D relVec, double rot) {
        setDrive(toMotorPowers(relVec, rot));
    }

    public static Vector2D toRelVec(Vector2D worldVec) {
        return worldVec.thetaed(-robot.theta + worldVec.theta + Math.PI / 2);
    }
    public static double[] toMotorPowers(Vector2D relVec, double rot) {
        return new double[]{
                relVec.x + relVec.y + rot,
                -relVec.x + relVec.y - rot,
                -relVec.x + relVec.y + rot,
                relVec.x + relVec.y - rot
        };
    }

    // Getters
    public static Pose getWaypoint(String name) {
        return waypoints.get(new ID(hw.opModeID, "waypoint", name));
    }
    public static SplineTrajectory getSpline(String name) {
        return splines.get(new ID(hw.opModeID, "spline", name));
    }

    ///////////////////////// ADVANCED MOTION /////////////////////////

    public static void pidMove(String waypoint) {
        hw.status = "Moving to waypoint " + waypoint;
        pidMove(getWaypoint(waypoint));
    }
    public static void pidMove(Pose target) {
        PIDCtrl.reset();
        PIDCtrl.init(target);

        ElapsedTime timer = new ElapsedTime();
        while (hw.ctx.opModeIsActive() && !hw.ctx.isStopRequested() && timer.milliseconds() <= 3000
               && (robot.distanceTo(target) > Constants.getDouble("motionProfile.endErrorThresholds.translation")
               || Math.abs(MathUtils.optThetaDiff(robot.theta, target.theta)) > Math.toRadians(Constants.getDouble("motionProfile.endErrorThresholds.rotation")))) {
            Object[] pidCorr = PIDCtrl.correction(robot);
            double[] wheelPowers = toMotorPowers(toRelVec((Vector2D) pidCorr[0]), (double) pidCorr[1]);
            setDrive(wheelPowers);
        }

        setDrive(0);
    }
    public static void pidMove(Vector2D addVec) {
        pidMove(addVec, robot.theta);
    }
    public static void pidMove(double coords, double blueDir, double redDir) {
        pidMove(new Vector2D(coords, hw.choose(blueDir, redDir), false));
    }
    public static void pidMove(double coords, double dir) {
        pidMove(coords, dir, dir);
    }
    public static void pidMove(Vector2D addVec, double targetHeading) {
        Pose target = robot.addVector(addVec);
        target.setT(targetHeading);
        pidMove(target);
    }

    public static void translate(String waypoint) {
        hw.status = "Translating to waypoint" + waypoint;
        translate(getWaypoint(waypoint));
    }
    public static void translate(Pose target) {
        pidMove(new Pose(target.x, target.y, robot.theta));
    }

    public static void rotate(String waypoint) {
        rotate(getWaypoint(waypoint).theta);
    }
    public static void rotate(double targetTheta) {
        hw.status = "Rotating to " + Math.toDegrees(targetTheta) + "\u00B0";
        pidMove(new Pose(robot.x, robot.y, targetTheta));
    }

    public static void followSpline(String spline) {
        followSpline(getSpline(spline));
    }
    public static void followSpline(SplineTrajectory spline) {
        double distance = 0;
        Pose last = new Pose(robot);
        Pose goal = new Pose(spline.waypoints.get(spline.waypoints.size() - 1));

        PIDCtrl.reset();

        ElapsedTime timer = new ElapsedTime();
        while (hw.ctx.opModeIsActive() && !hw.ctx.isStopRequested() && timer.milliseconds() <= 5000
                && (robot.distanceTo(goal) > Constants.getDouble("motionProfile.endErrorThresholds.translation")
                || Math.abs(MathUtils.optThetaDiff(robot.theta, goal.theta)) > Math.toRadians(Constants.getDouble("motionProfile.endErrorThresholds.rotation")))
                && distance <= spline.totalArcLength()) {
            distance += last.distanceTo(robot);
            distance = Math.min(distance, spline.totalArcLength());
            last = new Pose(robot);

            RigidBody setPoint = spline.mP.getRigidBody(distance + 1);
            PIDCtrl.init(setPoint);

            Object[] pidCorr = PIDCtrl.correction(robot);
            Vector2D worldVec = setPoint.tVel.added((Vector2D) pidCorr[0]);

            double[] wheelPowers = toMotorPowers(toRelVec(worldVec), (double) pidCorr[1]);
            setDrive(wheelPowers);
        }

        setDrive(0);
    }
    public static void splineToWaypoint(String waypoint) {
        hw.status = "Going to waypoint" + waypoint;
        splineToWaypoint(getWaypoint(waypoint));
    }
    public static void splineToWaypoint(Pose waypoint) {
        SplineTrajectory spline = new SplineTrajectory(robot, new RigidBody(waypoint));
        followSpline(spline);
    }
    public static void splineThroughWaypoints(Pose... waypoints) {
        waypoints = ArrayUtils.combineArrs(new Pose[]{ new Pose(robot) }, waypoints);
        SplineTrajectory spline = new SplineTrajectory(waypoints);
        followSpline(spline);
    }

}
