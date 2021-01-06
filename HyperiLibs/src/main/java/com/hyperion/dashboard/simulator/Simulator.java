package com.hyperion.dashboard.simulator;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.uiobject.fieldobject.Arrow;
import com.hyperion.dashboard.uiobject.fieldobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.fieldobject.FieldObject;
import com.hyperion.dashboard.uiobject.fieldobject.Robot;
import com.hyperion.dashboard.uiobject.fieldobject.Waypoint;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.pathplanning.DStarLite;
import com.hyperion.motion.trajectory.PIDCtrl;
import com.hyperion.motion.trajectory.SplineTrajectory;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Simulator {

    public Thread simulationThread;
    public State state;

    public boolean isSimPID = true;
    public boolean isSimDynPath;

    public FieldObject[] simulants;
    public boolean isSelectingFirst = true;
    public DStarLite pathPlanner;

    public Simulator() {
        this.state = State.INACTIVE;
        simulants = new FieldObject[2];
    }

    public void simulate() {
        simulationThread = new Thread(() -> {
            state = State.SIMULATING;
            pathPlanner = new DStarLite(Dashboard.fieldPane.fixedObstacles);

            DisplaySpline displaySpline;
            SplineTrajectory spline;
            if (simulants[0] instanceof DisplaySpline) {
                displaySpline = (DisplaySpline) simulants[0];
                spline = displaySpline.spline;
            } else {
                Pose start = ((Waypoint) simulants[0]).pose;
                Pose goal = ((Waypoint) simulants[1]).pose;
                if (isSimDynPath) {
                    pathPlanner.init(start, goal);
                    spline = new SplineTrajectory(pathPlanner.getPath());
                } else {
                    spline = new SplineTrajectory(start, goal);
                }
                displaySpline = new DisplaySpline(new ID(Dashboard.opModeID, "spline", "simulation"), spline);
                displaySpline.addDisplayGroup();
            }

            Arrow errorAccArrow = new Arrow(Color.RED, 15);
            Robot simRob = new Robot(new ID("robot.simulation"), spline.getDPose(0));
            Robot setPointRob = new Robot(new ID("robot.setPoint"), spline.getDPose(0));

            Platform.runLater(() -> {
                Dashboard.leftPane.simulate.setText("Stop\nSim");
                Dashboard.leftPane.simText.setText("Simulating " + selectionStr());
                Dashboard.fieldPane.getChildren().add(simRob.displayGroup);
                Dashboard.fieldPane.getChildren().add(setPointRob.displayGroup);
                Dashboard.fieldPane.getChildren().add(errorAccArrow.displayGroup);
            });

            double lastTheta = spline.waypoints.get(0).theta;
            double lastAngVel = 0;
            double distance = 0;

            simRob.rB = spline.mP.getRigidBody(0);
            Pose last = new Pose(simRob.rB);
            Pose goal = new Pose(spline.waypoints.get(spline.waypoints.size() - 1));

            Vector2D mPVel = new Vector2D();
            Vector2D errorAcc = new Vector2D();
            Vector2D pidCorrVel = new Vector2D();
            double pidCorrRot;

            double errorMag = Constants.getDouble("dashboard.simulator.errorMag");
            double errorProb = Constants.getInt("dashboard.simulator.errorProb");

            PIDCtrl.reset();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Timer collisionTimer = new Timer();
            class CollisionTask extends TimerTask {
                @Override
                public void run() {
                    double kProb = (Math.random() <= (errorProb / 100.0)) ? 1 : 0;
                    errorAcc.setVec(new Vector2D(kProb * errorMag * MathUtils.randInRange(new Random(), 0, 75),
                                                 kProb * MathUtils.randInRange(new Random(), 0, 2 * Math.PI), false));
                    try {
                        Thread.sleep((int) MathUtils.randInRange(new Random(), 0, 1500));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    collisionTimer.schedule(new CollisionTask(), (int) MathUtils.randInRange(new Random(), 0, 1500));
                }
            }
            CollisionTask randomError = new CollisionTask();
            randomError.run();

            long startTime = System.currentTimeMillis();
            long lastTime = startTime;
            while (state != State.INACTIVE && (System.currentTimeMillis() - startTime) <= 15000
                   && (simRob.rB.distanceTo(goal) > Constants.getDouble("pathing.endErrorThresholds.translation")
                   || Math.abs(MathUtils.optThetaDiff(simRob.rB.theta, goal.theta)) > Math.toRadians(Constants.getDouble("pathing.endErrorThresholds.rotation")))) {
                double dTime = (System.currentTimeMillis() - lastTime) / 1000.0;
                lastTime = System.currentTimeMillis();
                distance += last.distanceTo(simRob.rB);
                last = new Pose(simRob.rB);

                RigidBody setPoint = spline.mP.getRigidBody(Math.min(distance + 1, spline.totalArcLength()));
                PIDCtrl.setGoal(setPoint);

                mPVel.setVec(setPoint.tVel);
                simRob.rB.tVel.setVec(setPoint.tVel);

                simRob.rB.tVel.add(errorAcc);

                if (isSimPID) {
                    Object[] pidCorr = PIDCtrl.correction(simRob.rB);
                    pidCorrVel.setVec((Vector2D) pidCorr[0]);
                    System.out.println("PID Corr: " + (Vector2D) pidCorr[0] + " " + (double) pidCorr[1]);
                    pidCorrRot = (double) pidCorr[1];
                    simRob.rB.tVel.add(pidCorrVel);
                    simRob.rB.addXYT(0, 0, pidCorrRot / 360.0);
                }

                Vector2D dPos = simRob.rB.tVel.scaled(dTime);
                simRob.rB.setPose(simRob.rB.addVector(dPos));

                simRob.rB.aVel = (simRob.rB.theta - lastTheta) / dTime;
                lastTheta = simRob.rB.theta;
                simRob.rB.aAcc = (simRob.rB.aVel - lastAngVel) / dTime;
                lastAngVel = simRob.rB.aVel;

                Platform.runLater(() -> {
                    simRob.refreshDisplayGroup();
                    setPointRob.rB = setPoint;
                    setPointRob.refreshDisplayGroup();

                    errorAccArrow.set(simRob.rB, errorAcc);
                    simRob.setPIDandMPvels(mPVel, pidCorrVel);
                });

                if (isSimDynPath) {
                    pathPlanner.robotMoved(simRob.rB);

                    if (pathPlanner.updateDynamicObstacles(Dashboard.fieldPane.dynamicObstacles)) {
                        pathPlanner.recompute();

                        spline = new SplineTrajectory(pathPlanner.getPath());
                        displaySpline.refreshDisplayGroup();
                        distance = 0;
                        PIDCtrl.reset();
                    }
                }
            }

            randomError.cancel();

            if (System.currentTimeMillis() - startTime >= 10000) {
                System.out.println(selectionStr() + " simulation timed out");
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                Dashboard.fieldPane.getChildren().remove(errorAccArrow.displayGroup);
                Dashboard.fieldPane.getChildren().remove(simRob.displayGroup);
                Dashboard.fieldPane.getChildren().remove(setPointRob.displayGroup);
                Dashboard.leftPane.simulate.setText("Select\nSimulants");
                Dashboard.leftPane.simText.setText("");
            });

            simulants = new FieldObject[2];
            state = State.INACTIVE;
        });
        simulationThread.start();
    }

    public String selectionStr() {
        if (simulants[0] instanceof DisplaySpline) {
            return simulants[0].id.toString();
        } else {
            return (simulants[0] == null ? "" : simulants[0].id.get(-1)) + " -> " + (simulants[1] == null ? "" : simulants[1].id.get(-1));
        }
    }

    public enum State {
        INACTIVE, SELECTING, SIMULATING
    }

}
