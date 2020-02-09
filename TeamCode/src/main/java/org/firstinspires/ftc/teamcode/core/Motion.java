package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.Utils;
import com.hyperion.motion.math.RigidBody;
import org.firstinspires.ftc.teamcode.modules.HomogeneousPID;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.dstarlite.DStarLite;
import com.hyperion.motion.trajectory.SplineTrajectory;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.modules.Localizer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Manages all 2D robot translation & rotation
 */

public class Motion {

    public static Hardware hw;

    public static Localizer localizer;
    public static HomogeneousPID homogeneousPID;
    public static DStarLite dStarLite;

    public static RigidBody start = new RigidBody(new Pose(0, 0, 0));
    public static RigidBody robot = new RigidBody(start);
    public static Map<String, Pose> waypoints = new HashMap<>();
    public static Map<String, SplineTrajectory> splines = new HashMap<>();

    public static void init(Hardware hardware) {
        hw = hardware;
        try {
            JSONObject jsonObject = new JSONObject(Utils.readFile(hw.fieldJSON));
            readWaypoints(jsonObject);
            readSplines(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }

        localizer = new Localizer(hw);
        dStarLite = new DStarLite();
        homogeneousPID = new HomogeneousPID(hw);

        for (DcMotor motor : hw.hwmp.dcMotor) {
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        hw.fRDrive.setDirection(DcMotorSimple.Direction.REVERSE);
        hw.bRDrive.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    //////////////////////// INIT ////////////////////////////

    // Read waypoints from field.json file
    public static void readWaypoints(JSONObject root) throws Exception {
        JSONObject wpObj = root.getJSONObject("waypoints");
        waypoints.clear();

        Iterator keys = wpObj.keys();
        while (!hw.ctx.isStarted() && !hw.ctx.isStopRequested() && keys.hasNext()) {
            String key = keys.next().toString();
            JSONArray waypointArray = wpObj.getJSONArray(key);
            Pose waypoint = new Pose(waypointArray.getDouble(0), waypointArray.getDouble(1), waypointArray.getDouble(2));
            waypoints.put(key, waypoint);
        }
    }

    // Read splines from field.json file
    public static void readSplines(JSONObject root) throws Exception {
        JSONObject splinesObj = root.getJSONObject("splines");
        splines.clear();

        Iterator keys = splinesObj.keys();
        while (!hw.ctx.isStarted() && !hw.ctx.isStopRequested() && keys.hasNext()) {
            String key = keys.next().toString();
            SplineTrajectory spline = new SplineTrajectory(splinesObj.getJSONObject(key));
            splines.put(key, spline);
        }
    }

    ///////////////////////// RAW MOTION & HELPERS /////////////////////////

    // Drive power setters
    public static void setDrive(double fLPower, double fRPower, double bLPower, double bRPower) {
        double[] powers = new double[]{ fLPower, fRPower, bLPower, bRPower };
        setDrive(powers);
    }
    public static void setDrive(double leftPower, double rightPower) {
        setDrive(leftPower, rightPower, leftPower, rightPower);
    }
    public static void setDrive(double power) {
        setDrive(power, power);
    }
    public static void setDrive(double[] powers) {
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
    public static void setDrive(Vector2D relMoveVec, double rot) {
        setDrive(new double[]{
            relMoveVec.x + relMoveVec.y + rot,
            -relMoveVec.x + relMoveVec.y - rot,
            -relMoveVec.x + relMoveVec.y + rot,
            relMoveVec.x + relMoveVec.y - rot
        });
    }

    // Getters
    public static Pose getWaypoint(String name) {
        return waypoints.get(hw.opModeID + ".waypoint." + name);
    }
    public static SplineTrajectory getSpline(String name) {
        return splines.get(hw.opModeID + ".spline." + name);
    }

    ///////////////////////// ADVANCED MOTION /////////////////////////

    public static void pidMove(String waypoint) {
        hw.status = "Moving to waypoint " + waypoint;
        pidMove(getWaypoint(waypoint));
    }
    public static void pidMove(Pose target) {
        homogeneousPID.reset(target);
        ElapsedTime timer = new ElapsedTime();
        while (hw.ctx.opModeIsActive() && !hw.ctx.isStopRequested() && timer.milliseconds() <= 2250
               && (robot.pose.distanceTo(target) > Constants.END_TRANSLATION_ERROR_THRESHOLD
               || Math.abs(Utils.optThetaDiff(robot.pose.theta, target.theta)) > Constants.END_ROTATION_ERROR_THRESHOLD)) {
            homogeneousPID.controlLoopIteration(robot.pose);
        }
        setDrive(0);
    }
    public static void pidMove(Vector2D addVec) {
        pidMove(addVec, robot.pose.theta);
    }
    public static void pidMove(double coords, double blueDir, double redDir) {
        pidMove(new Vector2D(coords, hw.choose(blueDir, redDir), false));
    }
    public static void pidMove(double coords, double dir) {
        pidMove(coords, dir, dir);
    }
    public static void pidMove(Vector2D addVec, double targetHeading) {
        Pose target = robot.pose.addVector(addVec);
        target.setT(targetHeading);
        pidMove(target);
    }

    public static void translate(String waypoint) {
        hw.status = "Translating to waypoint" + waypoint;
        translate(getWaypoint(waypoint));
    }
    public static void translate(Pose target) {
        pidMove(new Pose(target.x, target.y, robot.pose.theta));
    }

    public static void rotate(String waypoint) {
        rotate(getWaypoint(waypoint).theta);
    }
    public static void rotate(double targetTheta) {
        hw.status = "Rotating to " + Math.toDegrees(targetTheta) + "\u00B0";
        pidMove(new Pose(robot.pose.x, robot.pose.y, targetTheta));
    }

}
