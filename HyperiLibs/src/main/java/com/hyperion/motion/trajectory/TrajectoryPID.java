package com.hyperion.motion.trajectory;

import com.hyperion.common.Constants;
import com.hyperion.common.Utils;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;

import java.util.ArrayList;

/**
 * PID Controller for homogeneous translation & rotation along spline trajectories
 * References:
 * (1) https://en.wikipedia.org/wiki/PID_controller
 * (2) https://www.ni.com/en-us/innovations/white-papers/06/pid-theory-explained.html
 */

public class TrajectoryPID {

    public Constants constants;
    public long currTime;
    public double lastTime;
    public RigidBody initial;
    public double preHtTheta;
    public SplineTrajectory splineTrajectory;
    public ArrayList<double[]> xEt;
    public ArrayList<double[]> xUt;
    public ArrayList<double[]> yEt;
    public ArrayList<double[]> yUt;
    public ArrayList<double[]> thetaEt;
    public ArrayList<double[]> thetaUt;

    public TrajectoryPID(Constants constants) {
        this.constants = constants;
    }

    public void reset(RigidBody initial, SplineTrajectory splineTrajectory) {
        currTime = -1;
        lastTime = 0;
        this.splineTrajectory = splineTrajectory;
        this.initial = new RigidBody(initial);
        preHtTheta = initial.pose.theta;
        xEt = new ArrayList<>();
        xUt = new ArrayList<>();
        yEt = new ArrayList<>();
        yUt = new ArrayList<>();
        thetaEt = new ArrayList<>();
        thetaUt = new ArrayList<>();
    }

    public double[] controlLoopIteration(double distance, RigidBody robot) {
        if (currTime == -1) currTime = System.currentTimeMillis();
        double dT = (System.currentTimeMillis() - currTime) / 1000.0;
        RigidBody setPoint = splineTrajectory.motionProfile.getRigidBody(distance);
        double xError = setPoint.pose.x - robot.pose.x;
        double yError = setPoint.pose.y - robot.pose.y;
        double thetaError = Utils.optimalThetaDifference(robot.pose.theta, setPoint.pose.theta);

        double time = lastTime + dT;
        xEt.add(new double[]{ time, xError });
        yEt.add(new double[]{ time, yError });
        thetaEt.add(new double[]{ time, thetaError });

        double uX = pOut(xEt, constants.X_K_P) + iOut(xEt, constants.X_K_I) + dOut(xEt, constants.X_K_D);
        double uY = pOut(yEt, constants.Y_K_P) + iOut(yEt, constants.Y_K_I) + dOut(yEt, constants.Y_K_D);
        double uTheta = pOut(thetaEt, constants.THETA_K_P) + iOut(thetaEt, constants.THETA_K_I) + dOut(thetaEt, constants.THETA_K_D);

        xUt.add(new double[]{ time, uX });
        yUt.add(new double[]{ time, uY });
        thetaUt.add(new double[]{ time, uTheta });

        lastTime = time;
        return toMotorPowers(robot.pose, uX, uY, uTheta);
    }

    public double pOut(ArrayList<double[]> eT, double kP) {
        return kP * eT.get(eT.size() - 1)[1];
    }

    public double iOut(ArrayList<double[]> eT, double kI) {
        double integral = 0;
        double lastT = 0;
        for (int i = 1; i < eT.size(); i++) {
            double dT = eT.get(i)[0] - lastT;
            integral += dT * eT.get(i - 1)[1];
            lastT += dT;
        }
        return kI * integral;
    }

    public double dOut(ArrayList<double[]> eT, double kD) {
        double derivative = 0;
        if (eT.size() >= 2) {
            derivative = (eT.get(eT.size() - 1)[1] - eT.get(eT.size() - 2)[1]) / (eT.get(eT.size() - 1)[0] - eT.get(eT.size() - 2)[0]);
        }
        return kD * derivative;
    }

    public double[] toMotorPowers(Pose robot, double uX, double uY, double uTheta) {
        double vX = Utils.clip(uX, -1, 1);
        double vY = Utils.clip(uY, -1, 1);
        double w = Utils.clip(uTheta, -1, 1);

        Vector2D velocity = new Vector2D(vX, vY, true).rotated(preHtTheta - robot.theta);
        if (velocity.magnitude > 1) velocity = velocity.unit();
        return new double[]{
            velocity.x + velocity.y + w,
            -velocity.x + velocity.y - w,
            -velocity.x + velocity.y + w,
            velocity.x + velocity.y - w
        };
    }

}
