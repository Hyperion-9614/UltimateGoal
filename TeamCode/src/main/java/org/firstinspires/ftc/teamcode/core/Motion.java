package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.ArrayUtils;
import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.pathplanning.DStarLite;
import com.hyperion.motion.pathplanning.Obstacle;
import com.hyperion.motion.trajectory.PIDCtrl;
import com.hyperion.motion.trajectory.SplineTrajectory;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.modules.Localizer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages all 2D robot translation & rotation
 */

public class Motion {

    public static Hardware hw;

    public static Localizer localizer;
    public static DStarLite pathPlanner;
    public static ArrayList<Obstacle> fixedObstacles = new ArrayList<>();
    public static ArrayList<Obstacle> dynamicObstacles = new ArrayList<>();

    public static RigidBody start;
    public static RigidBody robot;
    public static Map<ID, Pose> waypoints = new HashMap<>();
    public static Map<ID, SplineTrajectory> splines = new HashMap<>();

    public static void init(Hardware hardware) {
        hw = hardware;
        try {
            JSONObject fieldRoot = new JSONObject(IOUtils.readFile(hw.fieldJSON));
            readWaypoints(fieldRoot);
            readSplines(fieldRoot);
            readFixedObstacles(fieldRoot);

            start = new RigidBody(getWaypoint("start"));
            robot = new RigidBody(start);
        } catch (Exception e) {
            e.printStackTrace();
        }

        localizer = new Localizer(hw);
        pathPlanner = new DStarLite(fixedObstacles);

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

    // Read fixed obstacles from field.json file
    public static void readFixedObstacles(JSONObject root) throws Exception {
        JSONObject obstaclesObj = root.getJSONObject("obstacles");
        fixedObstacles.clear();

        Iterator<String> keys = obstaclesObj.keys();
        while (!hw.ctx.isStarted() && !hw.ctx.isStopRequested() && keys.hasNext()) {
            String key = keys.next();
            if (key.contains("fixed")) {
                Obstacle fixedObstacle;
                if (key.contains("rect"))
                    fixedObstacle = new Obstacle.Rect(obstaclesObj.getJSONObject(key));
                else
                    fixedObstacle = new Obstacle.Circle(obstaclesObj.getJSONObject(key));
                fixedObstacles.add(fixedObstacle);
            }
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
        hw.status = "PID moving to waypoint " + waypoint;
        pidMove(getWaypoint(waypoint));
    }
    public static void pidMove(Pose target) {
        PIDCtrl.reset();
        PIDCtrl.setGoal(target);

        ElapsedTime timer = new ElapsedTime();
        while (hw.ctx.opModeIsActive() && !hw.ctx.isStopRequested() && timer.milliseconds() <= 3000
               && (robot.distanceTo(target) > Constants.getDouble("pathing.endErrorThresholds.translation")
               || Math.abs(MathUtils.optThetaDiff(robot.theta, target.theta)) > Math.toRadians(Constants.getDouble("pathing.endErrorThresholds.rotation")))) {
            Object[] pidCorr = PIDCtrl.correction(robot);
            double[] wheelPowers = toMotorPowers(toRelVec((Vector2D) pidCorr[0]), (double) pidCorr[1]);
            setDrive(wheelPowers);
        }

        setDrive(0);
    }
    public static void pidMove(Vector2D addVec) {
        pidMove(addVec, robot.theta);
    }
    public static void pidMove(double coords, double dir) {
        pidMove(new Vector2D(coords, dir, false));
    }
    public static void pidMove(Vector2D addVec, double targetHeading) {
        Pose target = robot.addVector(addVec);
        target.setTheta(targetHeading);
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
        hw.status = "Rotating to waypoint " + waypoint;
        rotate(getWaypoint(waypoint).theta);
    }
    public static void rotate(double targetTheta) {
        hw.status = "Rotating to " + Math.toDegrees(targetTheta) + "\u00B0";
        pidMove(new Pose(robot.x, robot.y, targetTheta));
    }

    public static boolean followSpline(String spline) {
        hw.status = "Following spline " + spline;
        return followSpline(getSpline(spline), false);
    }
    public static boolean followSpline(SplineTrajectory spline, boolean isDynamic) {
        if (!spline.waypoints.get(0).equals(robot))
            pidMove(spline.waypoints.get(0));

        double distance = 0;
        Pose last = new Pose(robot);
        Pose goal = new Pose(spline.waypoints.get(spline.waypoints.size() - 1));

        PIDCtrl.reset();

        ElapsedTime timer = new ElapsedTime();
        while (hw.ctx.opModeIsActive() && !hw.ctx.isStopRequested() && timer.milliseconds() <= 8000
               && (robot.distanceTo(goal) > Constants.getDouble("pathing.endErrorThresholds.translation")
               || Math.abs(MathUtils.optThetaDiff(robot.theta, goal.theta)) > Math.toRadians(Constants.getDouble("pathing.endErrorThresholds.rotation")))
               && distance <= spline.totalArcLength()) {
            distance += last.distanceTo(robot);
            last = new Pose(robot);

            RigidBody setPoint = spline.mP.getRigidBody(Math.min(distance + 1, spline.totalArcLength()));
            PIDCtrl.setGoal(setPoint);

            Object[] pidCorr = PIDCtrl.correction(robot);
            Vector2D pidCorrAcc = (Vector2D) pidCorr[0];
            double pidCorrRot = (double) pidCorr[1];
            Vector2D worldVelVec = setPoint.tVel.added(pidCorrAcc).scaled(1 / Constants.getDouble("motionProfile.maxes.tVel"));

            double[] wheelPowers = toMotorPowers(toRelVec(worldVelVec), pidCorrRot * Constants.getDouble("pid.kRot"));
            setDrive(wheelPowers);

            if (isDynamic) {
                pathPlanner.robotMoved(robot);

                // NEED TO DO: Pass in empirical obstacle list
                if (pathPlanner.updateDynamicObstacles(new ArrayList<>())) {
                    pathPlanner.recompute();

                    spline = new SplineTrajectory(pathPlanner.getPath());
                    distance = 0;
                    PIDCtrl.reset();
                }
            }
        }

        setDrive(0);
        return (robot.distanceTo(spline.waypoints.get(spline.waypoints.size() - 1)) <= Constants.getDouble("pathing.endErrorThresholds.translation"));
    }

    public static boolean splineThroughPoses(Pose... waypoints) {
        waypoints = ArrayUtils.combineArrs(new Pose[]{ new Pose(robot) }, waypoints);
        SplineTrajectory spline = new SplineTrajectory(waypoints);
        return followSpline(spline, false);
    }
    public static boolean straightSplineToPose(String waypoint) {
        hw.status = "Splining to waypoint" + waypoint;
        return straightSplineToPose(getWaypoint(waypoint));
    }
    public static boolean straightSplineToPose(Pose waypoint) {
        SplineTrajectory spline = new SplineTrajectory(robot, new RigidBody(waypoint));
        return followSpline(spline, false);
    }

    public static boolean dynamicSplineToPose(String waypoint) {
        hw.status = "Pathfinding to waypoint" + waypoint;
        return dynamicSplineToPose(getWaypoint(waypoint));
    }
    public static boolean dynamicSplineToPose(Pose waypoint) {
        pathPlanner.init(robot, waypoint);
        ArrayList<Pose> path = pathPlanner.getPath();
        SplineTrajectory spline = new SplineTrajectory(path.toArray(new Pose[]{}));
        return followSpline(spline, true);
    }

}
