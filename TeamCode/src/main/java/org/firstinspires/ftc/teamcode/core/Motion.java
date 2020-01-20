package org.firstinspires.ftc.teamcode.core;

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

import java.util.HashMap;
import java.util.Iterator;

/**
 * Manages all 2D robot translation & rotation
 */

public class Motion {

    public Hardware hw;

    public Localizer localizer;
    public HomogeneousPID homogeneousPID;
    public DStarLite dStarLite;

    public RigidBody start = new RigidBody(new Pose(0, 0, 0));
    public RigidBody robot = new RigidBody(start);
    public HashMap<String, Pose> waypoints = new HashMap<>();
    public HashMap<String, SplineTrajectory> splines = new HashMap<>();

    public Motion(Hardware hw) {
        this.hw = hw;
        try {
            JSONObject jsonObject = new JSONObject(Utils.readFile(hw.dashboardJson));
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

    // Read waypoints from dashboard.json file
    public void readWaypoints(JSONObject root) throws Exception {
        JSONObject wpObj = root.getJSONObject("waypoints");
        waypoints.clear();

        Iterator keys = wpObj.keys();
        while (!hw.context.isStarted() && !hw.context.isStopRequested() && keys.hasNext()) {
            String key = keys.next().toString();
            JSONArray waypointArray = wpObj.getJSONArray(key);
            Pose waypoint = new Pose(waypointArray.getDouble(0), waypointArray.getDouble(1), waypointArray.getDouble(2));
            waypoints.put(key, waypoint);
        }
    }

    // Read splines from dashboard.json file
    public void readSplines(JSONObject root) throws Exception {
        JSONObject splinesObj = root.getJSONObject("splines");
        splines.clear();

        Iterator keys = splinesObj.keys();
        while (!hw.context.isStarted() && !hw.context.isStopRequested() && keys.hasNext()) {
            String key = keys.next().toString();
            SplineTrajectory spline = new SplineTrajectory(splinesObj.getJSONObject(key).toString(), hw.constants);
            splines.put(key, spline);
        }
    }

    ///////////////////////// RAW MOTION & HELPERS /////////////////////////

    // Drive power setters
    public void setDrive(double fLPower, double fRPower, double bLPower, double bRPower) {
        double[] powers = new double[]{ fLPower, fRPower, bLPower, bRPower };
        setDrive(powers);
    }
    public void setDrive(double leftPower, double rightPower) {
        setDrive(leftPower, rightPower, leftPower, rightPower);
    }
    public void setDrive(double power) {
        setDrive(power, power);
    }
    public void setDrive(double[] powers) {
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
        hw.checkForcePark();
    }
    public void setDrive(Vector2D relMoveVec, double rot) {
        setDrive(new double[]{
            relMoveVec.x + relMoveVec.y + rot,
            -relMoveVec.x + relMoveVec.y - rot,
            -relMoveVec.x + relMoveVec.y + rot,
            relMoveVec.x + relMoveVec.y - rot
        });
    }

    ///////////////////////// ADVANCED MOTION /////////////////////////

    public void pidMove(String waypoint) {
        String id = hw.opModeID + ".waypoint." + waypoint;
        hw.status = "Moving to " + id;
        pidMove(waypoints.get(id));
    }
    public void pidMove(Pose target) {
        homogeneousPID.reset(robot.pose, target);
        ElapsedTime timer = new ElapsedTime();
        while (hw.context.opModeIsActive() && !hw.context.isStopRequested() && timer.milliseconds() <= 5500
               && robot.pose.distanceTo(target) > hw.constants.END_TRANSLATION_ERROR_THRESHOLD
               && Utils.optThetaDiff(robot.pose.theta, target.theta) > hw.constants.END_ROTATION_ERROR_THRESHOLD) {
            homogeneousPID.controlLoopIteration(robot.pose);
        }
    }

    public void rotate(double targetTheta) {
        hw.status = "Rotating to " + Math.toDegrees(targetTheta) + "\u00B0";
        pidMove(new Pose(robot.pose.x, robot.pose.y, targetTheta));
    }

}
