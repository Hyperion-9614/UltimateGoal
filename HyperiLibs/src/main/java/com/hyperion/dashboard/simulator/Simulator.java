package com.hyperion.dashboard.simulator;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.dashboard.uiobject.fieldobject.Robot;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Simulator {

    public ArrayList<Simulation> simulations;
    public Thread simulationThread;

    public RigidBody robot;

    public Simulator() {
        robot = new RigidBody(new Pose(0, 0, 0));

        simulations = new ArrayList<>();
        simulations.add(new Test());
    }

    public void simulate(String name) {
        Simulation sim = simulations.stream()
                .filter(s -> s.id.sub(2).equals(name))
                .collect(Collectors.toList()).get(0);
        simulationThread = new Thread(() -> {
            sim.init(robot);
            sim.run();
        });
        simulationThread.start();

        Robot simRob = new Robot(new ID("robot.sim"), robot);
        simRob.addDisplayGroup();

        Vector2D lastTvel = new Vector2D();
        double lastAVel = 0;

        long startTime = System.currentTimeMillis();
        long lastTime = startTime;
        while (sim.state == Simulation.State.RUNNING) {
            double dTime = (System.currentTimeMillis() - lastTime) / 1000.0;
            lastTime = System.currentTimeMillis();

            setEmpiricalMotionVectors(sim, dTime);
            robot.addXYT(robot.tVel.x * dTime, robot.tVel.y * dTime, robot.aVel * dTime);
            robot.theta = MathUtils.norm(robot.theta, 0, 2 * Math.PI);

            robot.tAcc = new Vector2D(Math.abs(robot.tVel.magnitude - lastTvel.magnitude),
                    MathUtils.norm(robot.tVel.theta + (robot.tVel.magnitude < lastTvel.magnitude ? Math.PI : 0), 0, 2 * Math.PI),
                    false).scaled(1.0 / dTime);
            lastTvel = new Vector2D(robot.tVel);
            robot.aVel = (robot.aVel - lastAVel) / dTime;
            lastAVel = robot.aVel;
        }
        simRob.removeDisplayGroup();
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

        Vector2D finalVec = x.added(y).scaled(kReal);
        double rot = (sim.fLPow - sim.bRPow) / 2;

        robot.tVel.setVec(finalVec.scaled(Constants.getDouble("motionProfile.maxes.tVel")));
        robot.aVel = -rot * Constants.getDouble("motionProfile.maxes.aVel");
    }

    private double getDir(double pow, double nForth, double nBack) {
        return MathUtils.sign(pow) == 1 ? nForth * Math.PI : nBack * Math.PI;
    }

}
