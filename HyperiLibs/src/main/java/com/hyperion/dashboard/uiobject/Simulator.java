package com.hyperion.dashboard.uiobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.trajectory.PIDCtrl;
import com.hyperion.motion.trajectory.SplineTrajectory;

import javafx.application.Platform;
import javafx.scene.paint.Color;

public class Simulator {

    public Thread simulationThread;
    public State state;

    public double errorMag = 0.5;
    public boolean isSimPID = true;
    public boolean isSimDynPath;

    public FieldObject[] simulants;
    public boolean isSelectingFirst = true;

    public Simulator() {
        this.state = State.INACTIVE;
        simulants = new FieldObject[2];
    }

    public void simulate() {
        simulationThread = new Thread(() -> {
            state = State.SIMULATING;

            DisplaySpline displaySpline;
            SplineTrajectory spline;
            if (simulants[0] instanceof DisplaySpline) {
                displaySpline = (DisplaySpline) simulants[0];
                spline = displaySpline.spline;
            } else {
                spline = new SplineTrajectory(((Waypoint) simulants[0]).pose, ((Waypoint) simulants[1]).pose);
                displaySpline = new DisplaySpline(new ID(Dashboard.opModeID, "spline", "simulation"), spline);
                displaySpline.addDisplayGroup();
            }

            Arrow mPVelArrow = new Arrow(Color.WHITE, 15);
            Arrow errorAccArrow = new Arrow(Color.RED, 15);
            Arrow pidAccArrow = new Arrow(Color.BLUE, 15);
            Arrow finalVelArrow = new Arrow(Color.BLACK, 15);
            Robot simRob = new Robot(new ID("robot.simulation"), spline.getDPose(0));

            Platform.runLater(() -> {
                Dashboard.leftPane.simulate.setText("Stop\nSim");
                Dashboard.leftPane.simText.setText("Simulating " + selectionStr());

                Dashboard.fieldPane.getChildren().add(simRob.displayGroup);
                simRob.displayGroup.toFront();
                Dashboard.fieldPane.getChildren().add(mPVelArrow.displayGroup);
                mPVelArrow.displayGroup.toFront();
                Dashboard.fieldPane.getChildren().add(errorAccArrow.displayGroup);
                errorAccArrow.displayGroup.toFront();
                Dashboard.fieldPane.getChildren().add(pidAccArrow.displayGroup);
                pidAccArrow.displayGroup.toFront();
                Dashboard.fieldPane.getChildren().add(finalVelArrow.displayGroup);
                finalVelArrow.displayGroup.toFront();
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
            errorMag = Dashboard.leftPane.errorSpinner.getValue();

            PIDCtrl.reset();

            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

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

                errorAcc.setVec(new Vector2D(errorMag * 30, errorMag * 30, true));
                simRob.rB.tVel.add(errorAcc);

                if (isSimPID) {
                    Object[] pidCorr = PIDCtrl.correction(simRob.rB);
                    pidCorrAcc.setVec((Vector2D) pidCorr[0]);
                    pidCorrRot = (double) pidCorr[1];
                    simRob.rB.tVel.add(pidCorrAcc);
                    simRob.rB.addXYT(0, 0, pidCorrRot);
                }

                Vector2D dPos = simRob.rB.tVel.scaled(dTime);
                simRob.rB.setPose(simRob.rB.addVector(dPos));

                simRob.rB.aVel = (simRob.rB.theta - lastTheta) / dTime;
                lastTheta = simRob.rB.theta;
                simRob.rB.aAcc = (simRob.rB.aVel - lastAngVel) / dTime;
                lastAngVel = simRob.rB.aVel;

//                if (isSimDynPath) {
//                    pathPlanner.robotMoved(robot);
//
//                    // TODO: Pass in empirical obstacle list
//                    if (pathPlanner.updateObstacles(new ArrayList<>())) {
//                        pathPlanner.recompute();
//
//                        ArrayList<Pose> path = pathPlanner.getPath();
//                        SplineTrajectory newSpline = new SplineTrajectory(path.toArray(new Pose[]{}));
//                        return followSpline(newSpline, true);
//                    }
//                }

                Platform.runLater(() -> {
                    simRob.refreshDisplayGroup();

                    mPVelArrow.set(simRob.rB, mPVel);
                    errorAccArrow.set(simRob.rB, errorAcc);
                    pidAccArrow.set(simRob.rB, pidCorrAcc);
                    finalVelArrow.set(simRob.rB, simRob.rB.tVel);
                });
            }

            if (System.currentTimeMillis() - startTime >= 10000) {
                System.out.println(selectionStr() + " simulation timed out");
            }

            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> {
                Dashboard.fieldPane.getChildren().remove(mPVelArrow.displayGroup);
                Dashboard.fieldPane.getChildren().remove(errorAccArrow.displayGroup);
                Dashboard.fieldPane.getChildren().remove(pidAccArrow.displayGroup);
                Dashboard.fieldPane.getChildren().remove(finalVelArrow.displayGroup);
                Dashboard.fieldPane.getChildren().remove(simRob.displayGroup);
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
