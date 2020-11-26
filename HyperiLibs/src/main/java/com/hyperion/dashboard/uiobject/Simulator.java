package com.hyperion.dashboard.uiobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.Dashboard;
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
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Simulator {

    public Thread simulationThread;
    public State state;

    public double errorMag = 0.5;
    public double errorProb = 0.35;

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

            Arrow mPVelArrow = new Arrow(Color.WHITE, 15);
            Arrow errorAccArrow = new Arrow(Color.RED, 15);
            Arrow pidAccArrow = new Arrow(Color.BLUE, 15);
            Arrow finalVelArrow = new Arrow(Color.BLACK, 15);
            Robot simRob = new Robot(new ID("robot.simulation"), spline.getDPose(0));

            double[] toDisp = Dashboard.fieldPane.poseToDisplay(simRob.rB, 0);
            Circle setPointCircle = new Circle(toDisp[0], toDisp[1], 1.25 * Constants.getDouble("dashboard.gui.sizes.planningPoint"));
            setPointCircle.setFill(Color.WHITE);
            setPointCircle.setStroke(Color.BLACK);

            Platform.runLater(() -> {
                Dashboard.leftPane.simulate.setText("Stop\nSim");
                Dashboard.leftPane.simText.setText("Simulating " + selectionStr());

                for (Node node : new Node[]{ simRob.displayGroup, mPVelArrow.displayGroup,
                                             errorAccArrow.displayGroup, pidAccArrow.displayGroup,
                                             finalVelArrow.displayGroup, setPointCircle }) {
                    Dashboard.fieldPane.getChildren().add(node);
                    node.toFront();
                }
            });

            double lastTheta = spline.waypoints.get(0).theta;
            double lastAngVel = 0;
            double distance = 0;

            simRob.rB = spline.mP.getRigidBody(0);
            Pose last = new Pose(simRob.rB);
            Pose goal = new Pose(spline.waypoints.get(spline.waypoints.size() - 1));

            Vector2D mPVel = new Vector2D();
            Vector2D errorAcc = new Vector2D();
            Vector2D pidCorrAcc = new Vector2D();
            double pidCorrRot;

            errorMag = Dashboard.leftPane.errorMagSpinner.getValue();
            errorProb = Dashboard.leftPane.errorProbSpinner.getValue() / 100.0;

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
                    double kProb = (Math.random() <= errorProb) ? 0 : 1;
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
            while (state != State.INACTIVE && (System.currentTimeMillis() - startTime) <= 10000
                   && (simRob.rB.distanceTo(goal) > Constants.getDouble("pathing.endErrorThresholds.translation")
                   || Math.abs(MathUtils.optThetaDiff(simRob.rB.theta, goal.theta)) > Math.toRadians(Constants.getDouble("pathing.endErrorThresholds.rotation")))
                   && distance <= spline.totalArcLength()) {
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
                    pidCorrAcc.setVec((Vector2D) pidCorr[0]);
                    System.out.println("PID Corr: " + (Vector2D) pidCorr[0] + " " + (double) pidCorr[1]);
                    pidCorrRot = (double) pidCorr[1];
                    simRob.rB.tVel.add(pidCorrAcc);
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

                    double[] spDisp = Dashboard.fieldPane.poseToDisplay(setPoint, 0);
                    setPointCircle.setCenterX(spDisp[0]);
                    setPointCircle.setCenterY(spDisp[1]);

                    mPVelArrow.set(simRob.rB, mPVel);
                    errorAccArrow.set(simRob.rB, errorAcc);
                    pidAccArrow.set(simRob.rB, pidCorrAcc);
                    finalVelArrow.set(simRob.rB, simRob.rB.tVel);
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
                Dashboard.fieldPane.getChildren().remove(mPVelArrow.displayGroup);
                Dashboard.fieldPane.getChildren().remove(errorAccArrow.displayGroup);
                Dashboard.fieldPane.getChildren().remove(pidAccArrow.displayGroup);
                Dashboard.fieldPane.getChildren().remove(finalVelArrow.displayGroup);
                Dashboard.fieldPane.getChildren().remove(simRob.displayGroup);
                Dashboard.fieldPane.getChildren().remove(setPointCircle);
                if (displaySpline.id.get(-1).equals("simulation")) {
                    displaySpline.removeDisplayGroup();
                }
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
