package com.hyperion.dashboard.simulator;

import com.hyperion.common.ArrayUtils;
import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.uiobject.fieldobject.Robot;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;

import java.util.ArrayList;
import java.util.stream.Collectors;

import javafx.application.Platform;

public class Simulator {

    public ArrayList<Simulation> simulations;
    public Thread opModeThread;
    public Thread engineThread;
    public Simulation activeSim;
    
    public RigidBody robot;

    public Simulator() {
        robot = new RigidBody(new Pose(0, 0, 0));

        simulations = new ArrayList<>();
        simulations.add(new Test());
    }

    public void simulate(String id) {
        Simulation sim = simulations.stream()
                .filter(s -> s.id.equals(id))
                .collect(Collectors.toList()).get(0);
        try {
            activeSim = sim.getClass().newInstance();
            activeSim.init();
            robot = Simotion.robot;

            opModeThread = new Thread(() -> {
                activeSim.run();
                stop();
            });
            opModeThread.start();

            engineThread = new Thread(() -> {
                Robot simRob = new Robot(new ID("robot", "sim"), robot);
                simRob.addDisplayGroup();

                long lastTime = System.currentTimeMillis();
                while (activeSim != null && activeSim.state == Simulation.State.ACTIVE && opModeThread != null && !opModeThread.isInterrupted()) {
                    if (System.currentTimeMillis() - lastTime >= Constants.getDouble("simulator.renderDelay")) {
                        double dTime = (System.currentTimeMillis() - lastTime) / 1000.0;
                        lastTime = System.currentTimeMillis();

                        setEmpiricalMotionVectors(activeSim, dTime);
                        robot.addXYT(robot.tVel.x * dTime, robot.tVel.y * dTime, robot.aVel * dTime);
                        robot.theta = MathUtils.norm(robot.theta, 0, 2 * Math.PI);

                        simRob.refreshDisplayGroup();
                    }
                }
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                simRob.removeDisplayGroup();
            });
            engineThread.start();

            Platform.runLater(() -> Dashboard.leftPane.setSimDisables(true, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the current active sim
     */
    public void stop() {
        try {
            if (opModeThread != null) {
                opModeThread.interrupt();
                opModeThread = null;
            }
            Simotion.clear();
            if (activeSim != null) {
                Simotion.setDrive(0);
                activeSim.state = Simulation.State.INACTIVE;
                activeSim = null;
            }
            Platform.runLater(() -> Dashboard.leftPane.setSimDisables(false, true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the robot's translational/angular acceleration and velocity based on mechanum powers and realistic physics
     *
     * @param  sim    the simultion instance
     * @param  dTime  the change in time
     */
    private void setEmpiricalMotionVectors(Simulation sim, double dTime) {
        /* Calculate tAcc and aAcc using mecanum forces & kinematics */

        if (Constants.getInt("simulator.verbosity") == 2) {
            ArrayUtils.printArray(new double[]{ sim.fLPow, sim.fRPow, sim.bLPow, sim.bRPow });
        }

        // Constants
        double wRadius = Constants.getDouble("simulator.wheelRadius");
        double trackWidth = Constants.getDouble("localization.trackWidth");
        double driveBase = Constants.getDouble("localization.driveBase");
        double l2 = Math.pow(trackWidth, 2) + Math.pow(driveBase, 2);
        double rRadius = Math.sqrt(l2) / 2;
        double rMass = Constants.getDouble("simulator.robotMass");
        double kFrF = Constants.getDouble("simulator.kFrF");

        // Per-wheel forces
        double fLTorque = getTorque(sim.fLPow);
        Vector2D fL_Ff = new Vector2D(fLTorque / wRadius,
                scalToDir(fLTorque, 0, Math.PI), false);
        Vector2D fL_Fc = new Vector2D(fL_Ff.mag / Math.cos(Math.PI / 4),
                scalToDir(fLTorque, 7 * Math.PI / 4, 3 * Math.PI / 4), false);
        fL_Ff = fL_Ff.scaled(kFrF);

        double fRTorque = getTorque(sim.fRPow);
        Vector2D fR_Ff = new Vector2D(fRTorque / wRadius,
                scalToDir(fRTorque, 0, Math.PI), false);
        Vector2D fR_Fc = new Vector2D(fR_Ff.mag / Math.cos(Math.PI / 4),
                scalToDir(fRTorque, Math.PI / 4, 5 * Math.PI / 4), false);
        fR_Ff = fR_Ff.scaled(kFrF);

        double bLTorque = getTorque(sim.bLPow);
        Vector2D bL_Ff = new Vector2D(bLTorque / wRadius,
                scalToDir(bLTorque, 0, Math.PI), false);
        Vector2D bL_Fc = new Vector2D(bL_Ff.mag / Math.cos(Math.PI / 4),
                scalToDir(bLTorque, Math.PI / 4, 5 * Math.PI / 4), false);
        bL_Ff = bL_Ff.scaled(kFrF);

        double bRTorque = getTorque(sim.bRPow);
        Vector2D bR_Ff = new Vector2D(bRTorque / wRadius,
                scalToDir(bRTorque, 0, Math.PI), false);
        Vector2D bR_Fc = new Vector2D(bR_Ff.mag / Math.cos(Math.PI / 4),
                scalToDir(bRTorque, 7 * Math.PI / 4, 3 * Math.PI / 4), false);
        bR_Ff = bR_Ff.scaled(kFrF);

        // Translational
        Vector2D Ff = fL_Ff.added(fR_Ff).added(bL_Ff).added(bR_Ff);
        Vector2D Fc = fL_Fc.added(fR_Fc).added(bL_Fc).added(bR_Fc);
        Vector2D Ft = Ff.added(Fc);
        robot.tAcc.setVec(Ft.scaled(1 / rMass));
        robot.tAcc.setTheta(robot.tAcc.theta + robot.theta);

        // Rotational
        Vector2D Fl = fL_Ff.added(bL_Ff);
        Vector2D Fr = fR_Ff.added(bR_Ff);
        double Tl = rRadius * Fl.mag * dirToScal(Fl.theta, Math.PI);
        double Tr = rRadius * Fr.mag * dirToScal(Fr.theta, 0);
        double T = Tl + Tr;
        double I = (rMass / 12) * l2;
        robot.aAcc = T / I;

        /* Vf = Vi + A(delta t), but only if you're not running into a wall */
        // TODO: wall collision
        robot.tVel.add(robot.tAcc.scaled(dTime));
        robot.aVel += robot.aAcc * dTime;
    }

    private double getTorque(double pow) {
        double driveT = Constants.getDouble("simulator.driveTorque");
        double stallT = Constants.getDouble("simulator.stallTorque");
        return (Math.abs(pow) * driveT - stallT);
    }

    private double scalToDir(double pow, double pos, double neg) {
        return MathUtils.sign(pow) == 1 ? pos : neg;
    }

    private double dirToScal(double dir, double pos) {
        return MathUtils.doubEquals(dir, pos) ? 1 : -1;
    }

}
