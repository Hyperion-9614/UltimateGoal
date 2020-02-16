package org.firstinspires.ftc.teamcode.modules;

import com.hyperion.common.Constants;
import com.hyperion.common.Utils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;

import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;

import java.util.ArrayList;

/**
 * PID Controller for homogeneous translation & rotation along spline trajectories
 * References:
 * (1) https://en.wikipedia.org/wiki/PID_controller
 * (2) https://www.ni.com/en-us/innovations/white-papers/06/pid-theory-explained.html
 */

public class HomogeneousPID {

    public Hardware hardware;
    private long lastTime;
    private double time;
    private Pose goal;

    private ArrayList<double[]> xEt;
    public ArrayList<double[]> xUt;
    private ArrayList<double[]> yEt;
    public ArrayList<double[]> yUt;
    private ArrayList<double[]> thetaEt;
    public ArrayList<double[]> thetaUt;

    public HomogeneousPID(Hardware hardware) {
        this.hardware = hardware;
    }

    public void reset(Pose goal) {
        lastTime = -1;
        time = 0;
        this.goal = new Pose(goal);
        xEt = new ArrayList<>();
        xUt = new ArrayList<>();
        yEt = new ArrayList<>();
        yUt = new ArrayList<>();
        thetaEt = new ArrayList<>();
        thetaUt = new ArrayList<>();
    }

    public double[] pidWheelCorrections(Pose robot) {
        if (lastTime == -1) lastTime = System.currentTimeMillis();
        double dT = (System.currentTimeMillis() - lastTime) / 1000.0;

        Vector2D relTransErrVec = new Vector2D(robot, goal);
        relTransErrVec.setTheta(-robot.theta + relTransErrVec.theta);
        double thetaError = Utils.optThetaDiff(robot.theta, goal.theta);

        time += dT;
        xEt.add(new double[]{ time, relTransErrVec.x });
        yEt.add(new double[]{ time, relTransErrVec.y });
        thetaEt.add(new double[]{ time, thetaError });

        double uX = pOut(xEt, Constants.X_K_P) + iOut(xEt, Constants.X_K_I) + dOut(xEt, Constants.X_K_D);
        double uY = pOut(yEt, Constants.Y_K_P) + iOut(yEt, Constants.Y_K_I) + dOut(yEt, Constants.Y_K_D);
        double uTheta = pOut(thetaEt, Constants.THETA_K_P) + iOut(thetaEt, Constants.THETA_K_I) + dOut(thetaEt, Constants.THETA_K_D);

        xUt.add(new double[]{ time, uX });
        yUt.add(new double[]{ time, uY });
        thetaUt.add(new double[]{ time, uTheta });
        return wheelCorrections(uX, uY, uTheta);
    }

    private double[] wheelCorrections(double uX, double uY, double uTheta) {
        Vector2D relMoveVec = new Vector2D(uX, uY, true).rotated(Math.PI / 2);
        relMoveVec.setMagnitude(Math.min(0.75, relMoveVec.magnitude));
        double w = -Utils.clip(uTheta, -0.75, 0.75);
        return Motion.toMotorPowers(relMoveVec, w);
    }

    private double pOut(ArrayList<double[]> eT, double kP) {
        return kP * eT.get(eT.size() - 1)[1];
    }

    private double iOut(ArrayList<double[]> eT, double kI) {
        double integral = 0;
        for (int i = 0; i < eT.size() - 1; i++) {
            double baseSum = eT.get(i)[1] + eT.get(i + 1)[1];
            double height = eT.get(i + 1)[0] - eT.get(i)[0];
            integral += 0.5 * height * baseSum;
        }
        return kI * integral;
    }

    private double dOut(ArrayList<double[]> eT, double kD) {
        double derivative = 0;
        if (eT.size() >= 2) {
            derivative = (eT.get(eT.size() - 1)[1] - eT.get(eT.size() - 2)[1]) / (eT.get(eT.size() - 1)[0] - eT.get(eT.size() - 2)[0]);
        }
        return kD * derivative;
    }

}
