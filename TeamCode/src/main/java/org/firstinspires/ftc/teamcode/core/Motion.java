package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Utils;
import com.hyperion.motion.math.RigidBody;
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

    public Hardware hw;

    public Localizer localizer;
    public TrajectoryPID trajectoryPID;
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
        trajectoryPID = new TrajectoryPID(hw.constants);

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
    public void setDrive(Vector2D moveVec, double rot) {
        setDrive(new double[]{
            moveVec.x + moveVec.y + rot,
            -moveVec.x + moveVec.y - rot,
            -moveVec.x + moveVec.y + rot,
            moveVec.x + moveVec.y - rot
        });
    }

    ///////////////////////// GENERAL MOTION /////////////////////////

    // Strafe coords at velocity
    public void strafe(Vector2D velocity, double coords) {
        coords = Math.abs(coords);
        Pose target = robot.pose.addVector(new Vector2D(coords, velocity.theta, false));
        followPath(new SplineTrajectory(hw.constants, new RigidBody(robot), new RigidBody(target)));
    }

    // Strafe at velocity
    public void strafe(Vector2D a, double b, boolean isVw) {
        if (isVw) {
            a = a.rotated(-robot.pose.theta + Math.PI / 2);
            double[] velocities = new double[]{
                    a.x + a.y - b,
                    -a.x + a.y + b,
                    -a.x + a.y - b,
                    a.x + a.y + b
            };
            setDrive(velocities);
        } else {
            b = Math.abs(b);
            Pose target = robot.pose.addVector(new Vector2D(b, a.theta, false));
            followPath(new SplineTrajectory(hw.constants, new RigidBody(robot), new RigidBody(target)));
        }
    }

    // Rotate in place to target theta in specified direction
    public void rotate(double targetHeading) {
        targetHeading = Utils.normalizeTheta(targetHeading, 0, 2 * Math.PI);
        Pose target = new Pose(robot.pose.x, robot.pose.y, targetHeading);
        followPath(new SplineTrajectory(hw.constants, new RigidBody(robot), new RigidBody(target)));
    }

    ///////////////////////// PATHING /////////////////////////

    // Calculate and follow optimal SplineTrajectory to a waypoint from current pose
    public void goToWaypoint(Pose goal) {
        SplineTrajectory path = new SplineTrajectory(dStarLite.optimalPath(robot.pose, goal), hw.constants);
        if (hw.options.debug) {
            hw.rcClient.emit("pathFound", path.writeJSON());
            Utils.printSocketLog("RC", "SERVER", "pathFound", hw.options);
        }
        followPath(path);
        if (hw.options.debug) {
            hw.rcClient.emit("pathCompleted", "{}");
            Utils.printSocketLog("RC", "SERVER", "pathCompleted", hw.options);
        }
    }

    public void goToWaypoint(String waypointName) {
        Pose goal = waypoints.get(hw.opModeID + ".waypoint." + waypointName);
        hw.status = "Going to " + hw.opModeID + ".waypoint." + waypointName;
        goToWaypoint(goal);
        hw.status = "Arrived at " + hw.opModeID + ".waypoint." + waypointName;
    }

    // Uses feed forward motion profiling and a trajectory PID controller to follow a SplineTrajectory
    public void followPath(SplineTrajectory splineTrajectory) {
        double distance = 0;
        Pose lastPose = new Pose(robot.pose);
        trajectoryPID.reset(robot, splineTrajectory);
        ElapsedTime timer = new ElapsedTime();
        while (hw.context.opModeIsActive() && !hw.context.isStopRequested() && timer.milliseconds() <= 7500
               && (hw.context.gamepad1.right_stick_x == 0 && hw.context.gamepad1.left_stick_y == 0 && hw.context.gamepad1.left_stick_x == 0)
               && Utils.distance(robot.pose, splineTrajectory.waypoints.get(splineTrajectory.waypoints.size() - 1).pose) >= hw.constants.END_TRANSLATION_ERROR_THRESHOLD
               || Math.abs(Utils.optimalThetaDifference(robot.pose.theta, splineTrajectory.waypoints.get(splineTrajectory.waypoints.size() - 1).pose.theta)) >= hw.constants.END_ROTATION_ERROR_THRESHOLD) {
            distance += lastPose.distanceTo(robot.pose);
            lastPose = new Pose(robot.pose);

            if (distance > splineTrajectory.length) {
                break;
            }

            Vector2D translationalVelocity = splineTrajectory.motionProfile.getTranslationalVelocity(distance).scaled(hw.constants.MP_K_TV);
            Vector2D translationalAcceleration = splineTrajectory.motionProfile.getTranslationalAcceleration(distance).scaled(hw.constants.MP_K_TA);

            double angularVelocity = Math.pow((1.0 / (2 * Math.PI)) * Utils.optimalThetaDifference(robot.pose.theta, splineTrajectory.getDPose(distance).theta), 3);
            double angularAcceleration = 0;

            strafe(translationalVelocity.added(translationalAcceleration), angularVelocity + angularAcceleration, true);
//            setDrive(trajectoryPID.controlLoopIteration(distance, robot));
        }
        setDrive(0);
    }

    public void followPath(String pathName) {
        SplineTrajectory path = splines.get(hw.opModeID + ".spline." + pathName);
        if (path != null) {
            hw.status = "Following " + hw.opModeID + ".spline." + pathName;
            followPath(path);
            hw.status = "Completed " + hw.opModeID + ".spline." + pathName;
        }
    }

}
