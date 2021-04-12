package com.hyperion.dashboard.simulator;

import com.hyperion.common.ArrayUtils;
import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.pathplanning.DStarLite;
import com.hyperion.motion.pathplanning.Obstacle;
import com.hyperion.motion.trajectory.PIDCtrl;
import com.hyperion.motion.trajectory.SplineTrajectory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Simotion {

    public static Simulation sim;
    public static RigidBody robot;
    public static RigidBody start;

    public static Map<String, Pose> waypoints = new HashMap<>();
    public static Map<String, SplineTrajectory> splines = new HashMap<>();

    public static DStarLite pathPlanner;
    public static ArrayList<Obstacle> fixedObstacles = new ArrayList<>();
    public static ArrayList<Obstacle> dynamicObstacles = new ArrayList<>();

    public static void init(Simulation sim) {
        Simotion.sim = sim;
        Simotion.start = new RigidBody(sim.start);
        Simotion.robot = new RigidBody(start);

        try {
            JSONObject fieldRoot = new JSONObject(IOUtils.readFile(Constants.getFile("data", "field.json")));
            readWaypoints(fieldRoot);
            readSplines(fieldRoot);
            readFixedObstacles(fieldRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }

        pathPlanner = new DStarLite(fixedObstacles);
    }

    // Reset Simotion
    public static void clear() {
        sim = null;
        robot = null;
        start = null;
    }

    // Read waypoints from field.json file
    public static void readWaypoints(JSONObject root) {
        JSONObject wpObj = root.getJSONObject("waypoints");
        waypoints.clear();

        Iterator<String> keys = wpObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONArray waypointArray = wpObj.getJSONArray(key);
            Pose waypoint = new Pose(waypointArray.getDouble(0), waypointArray.getDouble(1), waypointArray.getDouble(2));
            waypoints.put(key, waypoint);
        }
    }

    // Read splines from field.json file
    public static void readSplines(JSONObject root) {
        JSONObject splinesObj = root.getJSONObject("splines");
        splines.clear();

        Iterator<String> keys = splinesObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            SplineTrajectory spline = new SplineTrajectory(splinesObj.getJSONObject(key));
            splines.put(key, spline);
        }
    }

    // Read fixed obstacles from field.json file
    public static void readFixedObstacles(JSONObject root) {
        JSONObject obstaclesObj = root.getJSONObject("obstacles");
        fixedObstacles.clear();

        Iterator<String> keys = obstaclesObj.keys();
        while (keys.hasNext()) {
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
    public static void setDrive(double... powers) {
        if (sim != null) {
            if (powers.length == 4) {
                sim.fLPow = MathUtils.clip(powers[0], -1, 1);
                sim.fRPow = MathUtils.clip(powers[1], -1, 1);
                sim.bLPow = MathUtils.clip(powers[2], -1, 1);
                sim.bRPow = MathUtils.clip(powers[3], -1, 1);
            } else if (powers.length == 2) {
                sim.fLPow = MathUtils.clip(powers[0], -1, 1);
                sim.fRPow = MathUtils.clip(powers[1], -1, 1);
                sim.bLPow = MathUtils.clip(powers[0], -1, 1);
                sim.bRPow = MathUtils.clip(powers[1], -1, 1);
            }
        }
    }
    public static void setDrive(double power) {
        setDrive(power, power);
    }
    public static void setDrive(double fLPower, double fRPower, double bLPower, double bRPower, long time) {
        setDrive(fLPower, fRPower, bLPower, bRPower);
        sim.sleep(time);
        setDrive(0);
    }
    public static void setDrive(double power, long time) {
        setDrive(power, power, power, power, time);
    }
    public static void setDrive(Vector2D relVec, double rot) {
        setDrive(toMotorPowers(relVec, rot));
    }

    public static Vector2D toRelVec(Vector2D worldVec) {
        Vector2D relVec = worldVec.thetaed(-robot.theta + worldVec.theta);
        if (relVec.mag > 1) relVec.setMag(1);
        return relVec;
    }
    public static double[] toMotorPowers(Vector2D relVec, double rot) {
        return new double[] {
            relVec.x - relVec.y - rot,
            relVec.x + relVec.y + rot,
            relVec.x + relVec.y - rot,
            relVec.x - relVec.y + rot
        };
    }

    // Getters
    public static Pose getWaypoint(String name) {
        return waypoints.get(new ID(Dashboard.opModeID, "waypoint", name).toString());
    }
    public static SplineTrajectory getSpline(String name) {
        return splines.get(new ID(Dashboard.opModeID, "spline", name).toString());
    }

    ///////////////////////// ADVANCED MOTION /////////////////////////

    // Main {
    public static void pidMove(Pose target) {
        PIDCtrl.reset();
        PIDCtrl.setGoal(target);

        long start = System.currentTimeMillis();
        while (sim != null && sim.state == Simulation.State.ACTIVE && robot != null
                && (System.currentTimeMillis() - start) <= 6000
                && (robot.distanceTo(target) > Constants.getDouble("pathing.endErrorThresholds.translation")
                || Math.abs(MathUtils.optThetaDiff(robot.theta, target.theta)) > Math.toRadians(Constants.getDouble("pathing.endErrorThresholds.rotation")))) {
            Object[] pidCorr = PIDCtrl.correction(robot);
            double[] wheelPowers = toMotorPowers(toRelVec((Vector2D) pidCorr[0]), (double) pidCorr[1]);
            setDrive(wheelPowers);
        }

        setDrive(0);
    }
    // }
    public static void pidMove(String waypoint) {
        sim.status = "PID moving to waypoint " + waypoint;
        pidMove(getWaypoint(waypoint));
    }
    public static void pidMove(Vector2D addVec) {
        sim.status = "PIDMoving on " + addVec;
        pidMove(addVec, robot.theta);
    }
    public static void pidMove(double coords, double dir) {
        sim.status = "PIDMoving " + coords + " cm toward " + Math.toDegrees(dir) + "\u00B0";
        pidMove(new Vector2D(coords, dir, false));
    }
    public static void pidMove(Vector2D addVec, double targetHeading) {
        sim.status = "PIDMoving on " + addVec + " to heading " + Math.toDegrees(targetHeading) + "\u00B0";
        Pose target = robot.addVector(addVec);
        target.setTheta(targetHeading);
        pidMove(target);
    }

    public static void translate(String waypoint) {
        sim.status = "Translating to waypoint" + waypoint;
        translate(getWaypoint(waypoint));
    }
    public static void translate(Pose target) {
        sim.status = "Translating to pose" + target;
        pidMove(new Pose(target.x, target.y, robot.theta));
    }

    public static void rotate(String waypoint) {
        sim.status = "Rotating to waypoint " + waypoint;
        rotate(getWaypoint(waypoint).theta);
    }
    public static void rotate(double targetTheta) {
        sim.status = "Rotating to heading " + Math.toDegrees(targetTheta) + "\u00B0";
        pidMove(new Pose(robot.x, robot.y, targetTheta));
    }

    // Main {
    public static boolean followSpline(SplineTrajectory spline, boolean isDynamic) {
        if (!spline.waypoints.get(0).equals(robot))
            pidMove(spline.waypoints.get(0));

        double distance = 0;
        double L = spline.totalArcLength();
        Pose last = new Pose(robot);
        Pose goal = new Pose(spline.waypoints.get(spline.waypoints.size() - 1));

        PIDCtrl.reset();

        long start = System.currentTimeMillis();
        while (sim != null && sim.state == Simulation.State.ACTIVE && robot != null && (System.currentTimeMillis() - start) <= Constants.getLong("spline.timeoutMS")
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
                worldVelVec.setMag(Math.max(worldVelVec.mag, 0.1));
            double[] wheelPowers = toMotorPowers(toRelVec(worldVelVec), rot / (2 * Math.PI));
            setDrive(wheelPowers);

            if (isDynamic) {
                pathPlanner.robotMoved(robot);

                // TODO: Pass in empirical obstacle list
                if (pathPlanner.updateDynamicObstacles(new ArrayList<>())) {
                    pathPlanner.recompute();

                    sim.status = "Recomputing spline with " + pathPlanner.getPath().size() + " waypoints";
                    spline = new SplineTrajectory(pathPlanner.getPath());
                    distance = 0;
                    PIDCtrl.reset();
                }
            }
        }

        setDrive(0);
        return (robot.distanceTo(spline.waypoints.get(spline.waypoints.size() - 1)) <= Constants.getDouble("pathing.endErrorThresholds.translation"));
    }
    // }
    public static boolean followSpline(String spline, boolean isDynamic) {
        sim.status = "Following spline " + spline;
        return followSpline(getSpline(spline), isDynamic);
    }

    public static boolean splineThroughPoses(Pose... poses) {
        poses = ArrayUtils.combineArrs(new Pose[]{ new Pose(robot) }, poses);
        sim.status = "Computing spline with " + poses.length + " waypoints";
        SplineTrajectory spline = new SplineTrajectory(poses);
        StringBuilder status = new StringBuilder("Splining through poses ");
        for (Pose p : poses)
            status.append(p).append(", ");
        sim.status = status.toString();
        return followSpline(spline, false);
    }
    public static boolean straightSplineToPose(String waypoint) {
        sim.status = "Splining to waypoint " + waypoint;
        return straightSplineToPose(getWaypoint(waypoint));
    }
    public static boolean straightSplineToPose(Pose waypoint) {
        sim.status = "Computing spline with 2 waypoints";
        SplineTrajectory spline = new SplineTrajectory(robot, waypoint);
        sim.status = "Splining straight to pose " + waypoint;
        return followSpline(spline, false);
    }

    public static boolean dynamicSplineToPose(String waypoint) {
        sim.status = "Pathfinding to waypoint " + waypoint;
        return dynamicSplineToPose(getWaypoint(waypoint));
    }
    public static boolean dynamicSplineToPose(Pose pose) {
        pathPlanner.init(robot, pose);
        sim.status = "Computing spline with 2 waypoints";
        SplineTrajectory spline = new SplineTrajectory(pathPlanner.getPath());
        sim.status = "Pathfinding to pose " + pose;
        return followSpline(spline, true);
    }

}
