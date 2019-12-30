package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Utils;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.trajectory.HomogeneousPID;
import com.hyperion.motion.trajectory.TrajectoryPID;
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

    public Hardware hardware;

    public Localizer localizer;
    public TrajectoryPID trajectoryPID;
    public HomogeneousPID homogeneousPID;
    public DStarLite dStarLite;

    public RigidBody start = new RigidBody(new Pose(0, 0, 0));
    public RigidBody robot = new RigidBody(start);
    public HashMap<String, Pose> waypoints = new HashMap<>();
    public HashMap<String, SplineTrajectory> splines = new HashMap<>();

    public Motion(Hardware hardware) {
        this.hardware = hardware;
        try {
            JSONObject jsonObject = new JSONObject(Utils.readFile(hardware.dashboardJson));
            readWaypoints(jsonObject);
            readSplines(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }

        localizer = new Localizer(hardware);
        dStarLite = new DStarLite();
        trajectoryPID = new TrajectoryPID(hardware.constants);
        homogeneousPID = new HomogeneousPID(hardware.constants);

        for (DcMotor motor : hardware.hwmp.dcMotor) {
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        hardware.fRDrive.setDirection(DcMotorSimple.Direction.REVERSE);
        hardware.bRDrive.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    //////////////////////// INIT ////////////////////////////

    // Read waypoints from dashboard.json file
    public void readWaypoints(JSONObject root) throws Exception {
        JSONObject wpObj = root.getJSONObject("waypoints");
        waypoints.clear();

        Iterator keys = wpObj.keys();
        while (keys.hasNext()) {
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
        while (keys.hasNext()) {
            String key = keys.next().toString();
            SplineTrajectory spline = new SplineTrajectory(splinesObj.getJSONObject(key).toString(), hardware.constants);
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
            hardware.fLDrive.setPower(powers[0]);
            hardware.fRDrive.setPower(powers[1]);
            hardware.bLDrive.setPower(powers[2]);
            hardware.bRDrive.setPower(powers[3]);
        } else if (powers.length == 2) {
            hardware.fLDrive.setPower(powers[0]);
            hardware.bLDrive.setPower(powers[0]);
            hardware.fRDrive.setPower(powers[1]);
            hardware.bRDrive.setPower(powers[1]);
        }
    }
    public void setDrive(Vector2D moveVec, double rot) {
        setDrive(new double[]{
            moveVec.x + moveVec.y + rot,
            -moveVec.x + moveVec.y - rot,
            -moveVec.x + moveVec.y + rot,
            moveVec.x + moveVec.y - rot
        });
    }

    ///////////////////////// GENERAL MOTION /////////////////////////

    // PID homogeneous transform to target
    public void pidMove(Pose target, boolean shouldStop) {
        homogeneousPID.reset(robot);
        while ((hardware.context.gamepad1.left_stick_x == 0 && hardware.context.gamepad1.left_stick_y == 0 && hardware.context.gamepad1.right_stick_x == 0)
               && (Utils.distance(robot.pose, target) >= hardware.constants.END_TRANSLATION_ERROR_THRESHOLD
               || Math.abs(Utils.optimalThetaDifference(robot.pose.theta, target.theta)) >= hardware.constants.END_ROTATION_ERROR_THRESHOLD)) {
            setDrive(homogeneousPID.controlLoopIteration(robot, new RigidBody(target)));
        }
        if (shouldStop) setDrive(0);
    }

    // Strafe coords at velocity
    public void strafe(Vector2D velocity, double coords) {
        coords = Math.abs(coords);
        Pose target = robot.pose.addVector(new Vector2D(coords, velocity.theta, false));
        pidMove(target, true);
    }

    // Strafe at velocity for specified amount of currTime
    public void strafe(Vector2D velocity, long msDuration) {
        ElapsedTime timer = new ElapsedTime();
        while (timer.milliseconds() < msDuration) {
            strafe(velocity);
        }
        setDrive(0);
    }

    // Strafe at velocity
    public void strafe(Vector2D velocity) {
        velocity = velocity.rotated(-robot.pose.theta + Math.PI / 2);
        double[] velocities = new double[]{
            velocity.x + velocity.y,
            -velocity.x + velocity.y,
            -velocity.x + velocity.y,
            velocity.x + velocity.y
        };
        setDrive(velocities);
    }

    // Rotate in place to target theta in specified direction
    public void rotate(double targetHeading) {
        targetHeading = Utils.normalizeTheta(targetHeading, 0, 2 * Math.PI);
        pidMove(new Pose(robot.pose.x, robot.pose.y, targetHeading), true);
    }

    // Kinda rotates in an arc, somewhat pivoting on one side, to target theta
    public void kindaPivot(double targetHeading, int pivotSide) {
        targetHeading = Utils.normalizeTheta(targetHeading, 0, 2 * Math.PI);
        if (pivotSide == 0) {
            pivotSide = -1;
            if (Utils.optimalThetaDifference(robot.pose.theta, targetHeading) > 0) {
                pivotSide = 1;
            }
        }
        double centriR = Utils.normalizeTheta(robot.pose.theta + Math.PI / 2, 0, 2 * Math.PI);
        double centriT = Utils.normalizeTheta(targetHeading + Math.PI / 2, 0, 2 * Math.PI);
        if (pivotSide == 1) {
            centriR = Utils.normalizeTheta(robot.pose.theta - Math.PI / 2, 0, 2 * Math.PI);
            centriT = Utils.normalizeTheta(targetHeading - Math.PI / 2, 0, 2 * Math.PI);
        }

        Pose center = robot.pose.addVector(new Vector2D(hardware.constants.TRACK_WIDTH / 2, centriR, false));
        Pose target = center.addVector(new Vector2D(hardware.constants.TRACK_WIDTH / 2, Utils.normalizeTheta(centriT + Math.PI, 0, 2 * Math.PI), false));
        pidMove(target, true);
    }

    ///////////////////////// PATHING /////////////////////////

    // Calculate and follow optimal SplineTrajectory to a waypoint from current pose
    public void goToWaypoint(Pose goal) {
        SplineTrajectory path = new SplineTrajectory(dStarLite.optimalPath(robot.pose, goal), hardware.constants);
        if (hardware.options.debug) {
            hardware.rcClient.emit("pathFound", path.writeJson());
            Utils.printSocketLog("RC", "SERVER", "pathFound", hardware.options);
        }
        followPath(path);
        if (hardware.options.debug) {
            hardware.rcClient.emit("pathCompleted", "{}");
            Utils.printSocketLog("RC", "SERVER", "pathCompleted", hardware.options);
        }
    }

    public void goToWaypoint(String waypointName) {
        Pose goal = waypoints.get(hardware.opModeID + ".waypoint." + waypointName);
        goToWaypoint(goal);
    }

    // Uses feed forward motion profiling and a trajectory PID controller to follow a SplineTrajectory
    public void followPath(SplineTrajectory splineTrajectory) {
        double distance = 0;
        Pose lastPose = robot.pose;
        trajectoryPID.reset(robot, splineTrajectory);
        while ((hardware.context.gamepad1.right_stick_x == 0 && hardware.context.gamepad1.left_stick_y == 0 && hardware.context.gamepad1.left_stick_x == 0)
               && Utils.distance(robot.pose, splineTrajectory.waypoints.get(splineTrajectory.waypoints.size() - 1).pose) >= hardware.constants.END_TRANSLATION_ERROR_THRESHOLD
               || Math.abs(Utils.optimalThetaDifference(robot.pose.theta, splineTrajectory.waypoints.get(splineTrajectory.waypoints.size() - 1).pose.theta)) >= hardware.constants.END_ROTATION_ERROR_THRESHOLD) {
            distance += lastPose.distanceTo(robot.pose);
            lastPose = robot.pose;

            Vector2D translationalVelocity = splineTrajectory.motionProfile.getTranslationalVelocity(distance).scaled(hardware.constants.MP_K_TA);
            Vector2D translationalAcceleration = splineTrajectory.motionProfile.getTranslationalAcceleration(distance).scaled(hardware.constants.MP_K_TV);
            double angularVelocity = -hardware.constants.MP_K_AV * splineTrajectory.motionProfile.getAngularVelocity(distance);
            double angularAcceleration = -hardware.constants.MP_K_AA * splineTrajectory.motionProfile.getAngularAcceleration(distance);
            setDrive(translationalVelocity.added(translationalAcceleration), angularVelocity + angularAcceleration);
            setDrive(trajectoryPID.controlLoopIteration(distance, robot));
        }
        setDrive(0);
    }

    public void followPath(String pathName) {
        SplineTrajectory path = splines.get(hardware.opModeID + ".spline." + pathName);
        if (path != null) {
            followPath(path);
        }
    }

}
