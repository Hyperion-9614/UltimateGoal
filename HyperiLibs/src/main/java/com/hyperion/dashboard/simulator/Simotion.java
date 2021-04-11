package com.hyperion.dashboard.simulator;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Vector2D;
import com.hyperion.motion.trajectory.PIDCtrl;

public class Simotion {

    public static Simulation sim;
    public static RigidBody robot;
    public static RigidBody start;

    public static void init(Simulation sim) {
        Simotion.sim = sim;
        Simotion.start = new RigidBody(sim.start);
        Simotion.robot = new RigidBody(start);
    }

    public static void clear() {
        sim = null;
        robot = null;
        start = null;
    }

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
        return worldVec.thetaed(-robot.theta + worldVec.theta);
    }
    public static double[] toMotorPowers(Vector2D relVec, double rot) {
        return new double[] {
                -relVec.x + relVec.y - rot,
                relVec.x + relVec.y + rot,
                relVec.x + relVec.y - rot,
                -relVec.x + relVec.y + rot
        };
    }

    public static void pidMove(Pose target) {
        PIDCtrl.reset();
        PIDCtrl.setGoal(target);

        long start = System.currentTimeMillis();
        while (sim.state != Simulation.State.INACTIVE && System.currentTimeMillis() - start <= 3000
                && (robot.distanceTo(target) > Constants.getDouble("pathing.endErrorThresholds.translation")
                || Math.abs(MathUtils.optThetaDiff(robot.theta, target.theta)) > Math.toRadians(Constants.getDouble("pathing.endErrorThresholds.rotation")))) {
            Object[] pidCorr = PIDCtrl.correction(robot);
            double[] wheelPowers = toMotorPowers(toRelVec((Vector2D) pidCorr[0]), (double) pidCorr[1]);
            setDrive(wheelPowers);
        }

        setDrive(0);
    }

}
