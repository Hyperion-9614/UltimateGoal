package com.hyperion.dashboard.simulator;

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

import javafx.scene.shape.Rectangle;

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
        activeSim = simulations.stream()
                .filter(s -> s.id.equals(id))
                .collect(Collectors.toList()).get(0);
        robot = new RigidBody(activeSim.start);

        opModeThread = new Thread(() -> {
            activeSim.init(robot);
            activeSim.run();
            if (activeSim != null)
                activeSim.end();
        });
        opModeThread.start();

        engineThread = new Thread(() -> {
            Robot simRob = new Robot(new ID("robot", "sim"), robot);
            simRob.addDisplayGroup();

            Vector2D lastTvel = new Vector2D();
            double lastAVel = 0;
            long startTime = System.currentTimeMillis();
            long lastTime = startTime;

            while (activeSim != null && activeSim.state == Simulation.State.RUNNING) {
                if (System.currentTimeMillis() -  lastTime >= Constants.getDouble("simulator.renderDelay")) {
                    double dTime = (System.currentTimeMillis() - lastTime) / 1000.0;
                    lastTime = System.currentTimeMillis();

                    setEmpiricalMotionVectors(activeSim, dTime);
                    robot.addXYT(robot.tVel.x * dTime, robot.tVel.y * dTime, robot.aVel * dTime);
                    robot.theta = MathUtils.norm(robot.theta, 0, 2 * Math.PI);

                    double aX = (robot.tVel.x - lastTvel.x) / dTime;
                    double aY = (robot.tVel.y - lastTvel.y) / dTime;
                    robot.tAcc = new Vector2D(aX, aY, true);
                    lastTvel = new Vector2D(robot.tVel);
                    robot.aAcc = (robot.aVel - lastAVel) / dTime;
                    lastAVel = robot.aVel;

                    simRob.refreshDisplayGroup();
                }
            }
            simRob.removeDisplayGroup();
        });
        engineThread.start();
    }

    /**
     * Sets the robot's translational/angular acceleration and velocity based on mechanum powers and realistic physics
     *
     * @param  sim    the simultion instance
     * @param  dTime  the change in time
     */
    private void setEmpiricalMotionVectors(Simulation sim, double dTime) {
        /* fL | turn: 0, pi | roll: pi/4, 5pi/4
         * fR | turn: 0, pi | roll: 7pi/4, 3pi/4
         * bL | turn: 0, pi | roll: 7pi/4, 3pi/4
         * bR | turn: 0, pi | roll: pi/4, 5pi/4
         */
        double kReal = 1 - Constants.getDouble("simulator.kSlip");
        double kStrafe = Constants.getDouble("simulator.kStrafe") * (sim.fLPow - sim.bLPow) / 2;

        Vector2D fLturn = new Vector2D(sim.fLPow, getDir(sim.fLPow, 0, 1), false);
        Vector2D fLroll = new Vector2D(kStrafe * sim.fLPow, getDir(sim.fLPow, 1.0/4, 5.0/4), false);

        Vector2D fRturn = new Vector2D(sim.fRPow, getDir(sim.fRPow, 0, 1), false);
        Vector2D fRroll = new Vector2D(kStrafe * sim.fRPow, getDir(sim.fRPow, 7.0/4, 3.0/4), false);

        Vector2D bLturn = new Vector2D(sim.bLPow, getDir(sim.bLPow, 0, 1), false);
        Vector2D bLroll = new Vector2D(kStrafe * sim.bLPow, getDir(sim.bLPow, 7.0/4, 3.0/4), false);

        Vector2D bRturn = new Vector2D(sim.bRPow, getDir(sim.bRPow, 0, 1), false);
        Vector2D bRroll = new Vector2D(kStrafe * sim.bRPow, getDir(sim.bRPow, 1.0/4, 5.0/4), false);

        Vector2D x = fLturn.added(bLturn).added(fRturn).added(bRturn);
        Vector2D y = fLroll.added(bLroll).added(fRroll).added(bRroll);

        Vector2D finalVelRel = x.added(y).scaled(kReal * Constants.getDouble("motionProfile.maxes.tVel") / 4);
        Vector2D worldVel = finalVelRel.thetaed(robot.theta + finalVelRel.theta);
        double rot = (sim.fLPow - sim.bRPow) / 2;

        robot.tVel.setVec(worldVel);
        robot.aVel = -rot * Constants.getDouble("motionProfile.maxes.aVel");

        double[] display = Dashboard.fieldPane.poseToDisplay(robot, Dashboard.fieldPane.robotSize);
        Rectangle rect = new Rectangle(display[0], display[1], Dashboard.fieldPane.robotSize, Dashboard.fieldPane.robotSize);
        boolean[] intersects = Dashboard.fieldPane.getWBBIntersects(rect);
        double xComp = robot.tVel.x, yComp = robot.tVel.y;
        if (!intersects[0]) xComp = 0;
        if (!intersects[1]) yComp = 0;
        robot.tVel.setVec(new Vector2D(xComp, yComp, true));
    }

    private double getDir(double pow, double nForth, double nBack) {
        return MathUtils.sign(pow) == 1 ? nForth * Math.PI : nBack * Math.PI;
    }

}
