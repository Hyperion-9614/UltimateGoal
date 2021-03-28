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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manages all 2D robot translation & rotation
 */

public class Motion {

    public static Gerald gerald;

    // Drive motors
    public static DcMotor fLDrive;
    public static DcMotor fRDrive;
    public static DcMotor bLDrive;
    public static DcMotor bRDrive;
    public static DcMotor[] drives;

    // Odometry
    public static DcMotor xLOdo;
    public static DcMotor xROdo;
    public static DcMotor yOdo;
    public static DcMotor[] odos;

    // Motion control
    public static Localizer localizer;
    public static DStarLite pathPlanner;
    public static ArrayList<Obstacle> fixedObstacles = new ArrayList<>();
    public static ArrayList<Obstacle> dynamicObstacles = new ArrayList<>();

    public static RigidBody start;
    public static RigidBody robot;
    public static Map<String, Pose> waypoints = new HashMap<>();
    public static Map<String, SplineTrajectory> splines = new HashMap<>();

    public static void init(Gerald gerald) {
        Motion.gerald = gerald;
        initHW();
        initMotion();
    }

    //////////////////////// INIT //////////////////////////

    // Init motion-related hardware
    public static void initHW() {
        // Drive motors
        fLDrive = gerald.hwmp.dcMotor.get("fLDrive");
        fRDrive = gerald.hwmp.dcMotor.get("fRDrive");
        bLDrive = gerald.hwmp.dcMotor.get("bLDrive");
        bRDrive = gerald.hwmp.dcMotor.get("bRDrive");
        drives = new DcMotor[]{ fLDrive, fRDrive, bLDrive, bRDrive };

        for (DcMotor drive : drives) {
            drive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            drive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            drive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        fRDrive.setDirection(DcMotorSimple.Direction.REVERSE);
        bLDrive.setDirection(DcMotorSimple.Direction.REVERSE);

        // Odometry
        xLOdo = fRDrive;
        xROdo = fLDrive;
        yOdo = bRDrive;
        odos = new DcMotor[]{ xLOdo, xROdo, yOdo };
    }

    // Init motion control modules
    public static void initMotion() {
        try {
            JSONObject fieldRoot = new JSONObject(IOUtils.readFile(gerald.fieldJSON));
            readWaypoints(fieldRoot);
            readSplines(fieldRoot);
            readFixedObstacles(fieldRoot);
            setStartRB();
        } catch (Exception e) {
            e.printStackTrace();
        }

        localizer = new Localizer(gerald);
        pathPlanner = new DStarLite(fixedObstacles);
    }

    // Read waypoints from field.json file
    public static void readWaypoints(JSONObject root) throws Exception {
        JSONObject wpObj = root.getJSONObject("waypoints");
        waypoints.clear();

        Iterator<String> keys = wpObj.keys();
        while (!gerald.ctx.isStarted() && !gerald.ctx.isStopRequested() && keys.hasNext()) {
            String key = keys.next();
            JSONArray waypointArray = wpObj.getJSONArray(key);
            Pose waypoint = new Pose(waypointArray.getDouble(0), waypointArray.getDouble(1), waypointArray.getDouble(2));
            waypoints.put(key, waypoint);
        }
    }

    // Read splines from field.json file
    public static void readSplines(JSONObject root) throws Exception {
        JSONObject splinesObj = root.getJSONObject("splines");
        splines.clear();

        Iterator<String> keys = splinesObj.keys();
        while (!gerald.ctx.isStarted() && !gerald.ctx.isStopRequested() && keys.hasNext()) {
            String key = keys.next();
            SplineTrajectory spline = new SplineTrajectory(splinesObj.getJSONObject(key));
            splines.put(key, spline);
        }
    }

    // Read fixed obstacles from field.json file
    public static void readFixedObstacles(JSONObject root) throws Exception {
        JSONObject obstaclesObj = root.getJSONObject("obstacles");
        fixedObstacles.clear();

        Iterator<String> keys = obstaclesObj.keys();
        while (!gerald.ctx.isStarted() && !gerald.ctx.isStopRequested() && keys.hasNext()) {
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

    // Sets start and robot rigidbodies to a fixed pose
    public static void setStartRB() {
//        Pose startPose = getSpline("test").waypoints.get(0);
//        Pose startPose = getWaypoint("start");
//        if (startPose == null) startPose = new Pose();
        Pose startPose = new Pose(0, 0, 0);
        start = new RigidBody(startPose);
        robot = new RigidBody(startPose);
    }

    ///////////////////////// RAW MOTION & HELPERS /////////////////////////

    // Drive power setters
    public static void setDrive(double power) {
        setDrive(power, power);
    }
    public static void setDrive(double... powers) {
        if (powers.length == 4) {
            fLDrive.setPower(powers[0]);
            fRDrive.setPower(powers[1]);
            bLDrive.setPower(powers[2]);
            bRDrive.setPower(powers[3]);
        } else if (powers.length == 2) {
            fLDrive.setPower(powers[0]);
            bLDrive.setPower(powers[0]);
            fRDrive.setPower(powers[1]);
            bRDrive.setPower(powers[1]);
        }
    }
    public static void setDrive(double fLPower, double fRPower, double bLPower, double bRPower, long time) {
        setDrive(fLPower, fRPower, bLPower, bRPower);
        gerald.ctx.sleep(time);
        setDrive(0);
    }
    public static void setDrive(double power, long time) {
        setDrive(power, power, power, power, time);
    }
    public static void setDrive(Vector2D relVec, double rot) {
        setDrive(toMotorPowers(relVec, rot));
    }

    public static Vector2D toRelVec(Vector2D worldVec) {
        return worldVec.thetaed(-robot.theta + worldVec.theta + Math.PI / 2);
    }
    public static double[] toMotorPowers(Vector2D relVec, double rot) {
        return new double[] {
            relVec.x + relVec.y + rot,
            -relVec.x + relVec.y - rot,
            -relVec.x + relVec.y + rot,
            relVec.x + relVec.y - rot
        };
    }

    // Getters
    public static Pose getWaypoint(String name) {
        return waypoints.get(new ID(gerald.opModeID, "waypoint", name).toString());
    }
    public static SplineTrajectory getSpline(String name) {
        return splines.get(new ID(gerald.opModeID, "spline", name).toString());
    }

    ///////////////////////// ADVANCED MOTION /////////////////////////

    public static void pidMove(String waypoint) {
        gerald.status = "PID moving to waypoint " + waypoint;
        pidMove(getWaypoint(waypoint));
    }
    public static void pidMove(Pose target) {
        PIDCtrl.reset();
        PIDCtrl.setGoal(target);

        ElapsedTime timer = new ElapsedTime();
        while (gerald.ctx.opModeIsActive() && !gerald.ctx.isStopRequested() && gerald.isRunning && timer.milliseconds() <= 3000
               && (robot.distanceTo(target) > Constants.getDouble("pathing.endErrorThresholds.translation")
               || Math.abs(MathUtils.optThetaDiff(robot.theta, target.theta)) > Math.toRadians(Constants.getDouble("pathing.endErrorThresholds.rotation")))) {
            Object[] pidCorr = PIDCtrl.correction(robot);
            double[] wheelPowers = toMotorPowers(toRelVec((Vector2D) pidCorr[0]), (double) pidCorr[1]);
            setDrive(wheelPowers);
        }

        setDrive(0);
    }
    public static void pidMove(Vector2D addVec) {
        gerald.status = "PIDMoving on " + addVec;
        pidMove(addVec, robot.theta);
    }
    public static void pidMove(double coords, double dir) {
        gerald.status = "PIDMoving " + coords + " cm toward " + Math.toDegrees(dir) + "\u00B0";
        pidMove(new Vector2D(coords, dir, false));
    }
    public static void pidMove(Vector2D addVec, double targetHeading) {
        gerald.status = "PIDMoving on " + addVec + " to heading " + Math.toDegrees(targetHeading) + "\u00B0";
        Pose target = robot.addVector(addVec);
        target.setTheta(targetHeading);
        pidMove(target);
    }

    public static void translate(String waypoint) {
        gerald.status = "Translating to waypoint" + waypoint;
        translate(getWaypoint(waypoint));
    }
    public static void translate(Pose target) {
        gerald.status = "Translating to pose" + target;
        pidMove(new Pose(target.x, target.y, robot.theta));
    }

    public static void rotate(String waypoint) {
        gerald.status = "Rotating to waypoint " + waypoint;
        rotate(getWaypoint(waypoint).theta);
    }
    public static void rotate(double targetTheta) {
        gerald.status = "Rotating to heading " + Math.toDegrees(targetTheta) + "\u00B0";
        pidMove(new Pose(robot.x, robot.y, targetTheta));
    }

    public static boolean followSpline(String spline, boolean isDynamic) {
        gerald.status = "Following spline " + spline;
        return followSpline(getSpline(spline), isDynamic);
    }
    public static boolean followSpline(SplineTrajectory spline, boolean isDynamic) {
        if (!spline.waypoints.get(0).equals(robot))
            pidMove(spline.waypoints.get(0));

        double distance = 0;
        double L = spline.totalArcLength();
        Pose last = new Pose(robot);
        Pose goal = new Pose(spline.waypoints.get(spline.waypoints.size() - 1));

        PIDCtrl.reset();

        ElapsedTime timer = new ElapsedTime();
        while (gerald.ctx.opModeIsActive() && !gerald.ctx.isStopRequested() && gerald.isRunning && timer.milliseconds() <= Constants.getLong("spline.timeoutMS")
               && (robot.distanceTo(goal) > Constants.getDouble("pathing.endErrorThresholds.translation")
               || Math.abs(MathUtils.optThetaDiff(robot.theta, goal.theta)) > Math.toRadians(Constants.getDouble("pathing.endErrorThresholds.rotation")))) {
            distance += last.distanceTo(robot);
            last = new Pose(robot);

            RigidBody setPoint = spline.mP.getRigidBody(Math.min(distance + 1, spline.totalArcLength()));
            Vector2D worldVelVec = setPoint.tVel;
            double rot = 0;

            if (Constants.getBoolean("pathing.usePID")) {
                PIDCtrl.setGoal(setPoint);
                Object[] pidCorr = PIDCtrl.correction(robot);
                Vector2D pidCorrVel = (Vector2D) pidCorr[0];
                rot = (double) pidCorr[1];
                worldVelVec.add(pidCorrVel);
                if (Constants.getBoolean("spline.verbose"))
                    System.out.println("PID(Vel/Rot): " + pidCorrVel + " / " + rot);
            }

            if (Constants.getBoolean("spline.verbose"))
                System.out.println("D/wVel: " + MathUtils.round(distance, 3) + " / " + worldVelVec.toString());
            worldVelVec = worldVelVec.scaled(Constants.getDouble("pathing.powerScale") / Constants.getDouble("motionProfile.maxes.tVel"));
            if (distance <= 0.04 * L)
                worldVelVec.setMagnitude(Math.max(worldVelVec.magnitude, 0.1));
            double[] wheelPowers = toMotorPowers(toRelVec(worldVelVec), rot / (2 * Math.PI));
            setDrive(wheelPowers);

            if (isDynamic) {
                pathPlanner.robotMoved(robot);

                // TODO: Pass in empirical obstacle list
                if (pathPlanner.updateDynamicObstacles(new ArrayList<>())) {
                    pathPlanner.recompute();

                    gerald.status = "Recomputing spline with " + pathPlanner.getPath().size() + " waypoints";
                    spline = new SplineTrajectory(pathPlanner.getPath());
                    distance = 0;
                    PIDCtrl.reset();
                }
            }
        }

        setDrive(0);
        return (robot.distanceTo(spline.waypoints.get(spline.waypoints.size() - 1)) <= Constants.getDouble("pathing.endErrorThresholds.translation"));
    }

    public static boolean splineThroughPoses(Pose... poses) {
        poses = ArrayUtils.combineArrs(new Pose[]{ new Pose(robot) }, poses);
        gerald.status = "Computing spline with " + poses.length + " waypoints";
        SplineTrajectory spline = new SplineTrajectory(poses);
        gerald.status = "Splining through poses ";
        for (Pose p : poses)
            gerald.status += p + ", ";
        return followSpline(spline, false);
    }
    public static boolean straightSplineToPose(String waypoint) {
        gerald.status = "Splining to waypoint " + waypoint;
        return straightSplineToPose(getWaypoint(waypoint));
    }
    public static boolean straightSplineToPose(Pose waypoint) {
        gerald.status = "Computing spline with 2 waypoints";
        SplineTrajectory spline = new SplineTrajectory(robot, waypoint);
        gerald.status = "Splining straight to pose " + waypoint;
        return followSpline(spline, false);
    }

    public static boolean dynamicSplineToPose(String waypoint) {
        gerald.status = "Pathfinding to waypoint " + waypoint;
        return dynamicSplineToPose(getWaypoint(waypoint));
    }
    public static boolean dynamicSplineToPose(Pose pose) {
        pathPlanner.init(robot, pose);
        gerald.status = "Computing spline with 2 waypoints";
        SplineTrajectory spline = new SplineTrajectory(pathPlanner.getPath());
        gerald.status = "Pathfinding to pose " + pose;
        return followSpline(spline, true);
    }

}
