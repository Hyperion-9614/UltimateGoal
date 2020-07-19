package com.hyperion.dashboard.uiobject;

import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.trajectory.SplineTrajectory;

import javafx.application.Platform;
import javafx.scene.paint.Color;

public class Simulator {

    public Thread simulationThread;
    public State state;

    public boolean isSimErrorPID;
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

            Arrow velocityVec = new Arrow(Color.BLACK, 20);
            Robot simulationRobot = new Robot(new ID("robot.simulation"), spline.getDPose(0));

            Platform.runLater(() -> {
                Dashboard.leftPane.simulate.setText("Stop\nSim");
                Dashboard.leftPane.simText.setText("Simulating " + selectionStr());
                Dashboard.fieldPane.getChildren().add(simulationRobot.displayGroup);
                simulationRobot.displayGroup.toFront();
                Dashboard.fieldPane.getChildren().add(velocityVec.displayGroup);
                velocityVec.displayGroup.toFront();
            });

            double distanceIncrement = 1;
            long lastTime = System.currentTimeMillis();
            double lastTheta = spline.waypoints.get(0).theta;
            double lastAngVel = 0;
            for (double d = 0; d <= spline.totalArcLength(); d += distanceIncrement) {
                if (state == State.INACTIVE) break;
                double dt = (System.currentTimeMillis() - lastTime) / 1000.0;
                lastTime = System.currentTimeMillis();

                RigidBody robot = spline.mP.getRigidBody(d);
                robot.tVel.setMagnitude(Math.max(robot.tVel.magnitude, 1));
                Pose dest = robot.addVector(robot.tVel);
                robot.aVel = (robot.theta - lastTheta) / dt;
                lastTheta = robot.theta;
                robot.aAcc = (robot.aVel - lastAngVel) / dt;
                lastAngVel = robot.aVel;

                simulationRobot.rigidBody = robot;

                Platform.runLater(() -> {
                    simulationRobot.refreshDisplayGroup();
                    velocityVec.set(Dashboard.fieldPane.poseToDisplay(robot, 0),
                                    Dashboard.fieldPane.poseToDisplay(dest, 0));
                });
                try {
                    Thread.sleep((long) MathUtils.round((distanceIncrement / robot.tVel.magnitude) * 1000, 0));
                } catch (InterruptedException e) {

                }
            }

            Platform.runLater(() -> {
                Dashboard.fieldPane.getChildren().remove(velocityVec.displayGroup);
                Dashboard.fieldPane.getChildren().remove(simulationRobot.displayGroup);
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
