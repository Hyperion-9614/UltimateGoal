package com.hyperion.dashboard.simulator;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.trajectory.PIDCtrl;

public abstract class Simulation {

    public ID id;
    public State state;
    public RigidBody robot;
    public RigidBody start;

    public double fLPow;
    public double fRPow;
    public double bLPow;
    public double bRPow;

    public Simulation(String type, String name, Pose start) {
        this.id = new ID(type, "simulation", name);
        this.state = State.INACTIVE;
        this.start = new RigidBody(start);
    }

    public void init(RigidBody robot) {
        this.robot = robot;
        this.state = State.ACTIVE;
        setDrive(0, 0, 0, 0);
    }

    public abstract void run();

    protected void setDrive(double... powers) {
        if (powers.length == 4) {
            this.fLPow = MathUtils.clip(powers[0], -1, 1);
            this.fRPow = MathUtils.clip(powers[1], -1, 1);
            this.bLPow = MathUtils.clip(powers[2], -1, 1);
            this.bRPow = MathUtils.clip(powers[3], -1, 1);
        } else if (powers.length == 2) {
            this.fLPow = MathUtils.clip(powers[0], -1, 1);
            this.fRPow = MathUtils.clip(powers[1], -1, 1);
            this.bLPow = MathUtils.clip(powers[0], -1, 1);
            this.bRPow = MathUtils.clip(powers[1], -1, 1);
        }
    }
    protected void setDrive(double power) {
        setDrive(power, power);
    }
    protected void setDrive(double fLPower, double fRPower, double bLPower, double bRPower, long time) {
        setDrive(fLPower, fRPower, bLPower, bRPower);
        sleep(time);
        setDrive(0);
    }
    protected void setDrive(double power, long time) {
        setDrive(power, power, power, power, time);
    }
    protected void setDrive(Vector2D relVec, double rot) {
        setDrive(toMotorPowers(relVec, rot));
    }

    protected Vector2D toRelVec(Vector2D worldVec) {
        return worldVec.thetaed(-robot.theta + worldVec.theta + Math.PI / 2);
    }
    protected double[] toMotorPowers(Vector2D relVec, double rot) {
        return new double[] {
                relVec.x + relVec.y + rot,
                -relVec.x + relVec.y - rot,
                -relVec.x + relVec.y + rot,
                relVec.x + relVec.y - rot
        };
    }

    protected void sleep(long ms) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < ms && state == State.ACTIVE) {

        }
    }

    protected void pidMove(Pose target) {
        PIDCtrl.reset();
        PIDCtrl.setGoal(target);

        long start = System.currentTimeMillis();
        while (state != State.INACTIVE && System.currentTimeMillis() - start <= 3000
                && (robot.distanceTo(target) > Constants.getDouble("pathing.endErrorThresholds.translation")
                || Math.abs(MathUtils.optThetaDiff(robot.theta, target.theta)) > Math.toRadians(Constants.getDouble("pathing.endErrorThresholds.rotation")))) {
            Object[] pidCorr = PIDCtrl.correction(robot);
            double[] wheelPowers = toMotorPowers(toRelVec((Vector2D) pidCorr[0]), (double) pidCorr[1]);
            setDrive(wheelPowers);
        }

        setDrive(0);
    }

    public enum State {
        ACTIVE, INACTIVE
    }

}
