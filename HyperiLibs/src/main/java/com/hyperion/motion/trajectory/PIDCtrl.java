package com.hyperion.motion.trajectory;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.common.MiscUtils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;

import java.util.ArrayList;

/**
 * PID Controller for homogeneous translation & rotation along spline trajectories
 * References:
 * (1) https://en.wikipedia.org/wiki/PID_controller
 * (2) https://www.ni.com/en-us/innovations/white-papers/06/pid-theory-explained.html
 */

public class PIDCtrl {

    private static long lastTime;
    private static double time;
    private static Pose goal;

    private static ArrayList<double[]> xEt;
    public static ArrayList<double[]> xUt;
    private static ArrayList<double[]> yEt;
    public static ArrayList<double[]> yUt;
    private static ArrayList<double[]> thetaEt;
    public static ArrayList<double[]> thetaUt;

    public static void reset() {
        lastTime = -1;
        time = 0;
        xEt = new ArrayList<>();
        xUt = new ArrayList<>();
        yEt = new ArrayList<>();
        yUt = new ArrayList<>();
        thetaEt = new ArrayList<>();
        thetaUt = new ArrayList<>();
    }

    public static void setGoal(Pose goal) {
        PIDCtrl.goal = new Pose(goal);
    }

    public static Object[] correction(Pose robot) {
        if (lastTime == -1) lastTime = System.currentTimeMillis();
        double dT = (System.currentTimeMillis() - lastTime) / 1000.0;

        Vector2D relTransErrVec = new Vector2D(robot, goal);
        relTransErrVec.setTheta(-robot.theta + relTransErrVec.theta);
        double thetaError = MathUtils.optThetaDiff(robot.theta, goal.theta);

        time += dT;
        xEt.add(new double[]{ time, relTransErrVec.x });
        yEt.add(new double[]{ time, relTransErrVec.y });
        thetaEt.add(new double[]{ time, thetaError });

        double uX = pOut(xEt, Constants.getDouble("pid.x.kP")) + iOut(xEt, Constants.getDouble("pid.x.kI")) + dOut(xEt, Constants.getDouble("pid.x.kD"));
        double uY = pOut(yEt, Constants.getDouble("pid.y.kP")) + iOut(yEt, Constants.getDouble("pid.y.kI")) + dOut(yEt, Constants.getDouble("pid.y.kD"));
        double uTheta = pOut(thetaEt, Constants.getDouble("pid.theta.kP")) + iOut(thetaEt, Constants.getDouble("pid.theta.kI")) + dOut(thetaEt, Constants.getDouble("pid.theta.kD"));

        xUt.add(new double[]{ time, uX });
        yUt.add(new double[]{ time, uY });
        thetaUt.add(new double[]{ time, uTheta });

        return correctionValues(uX, uY, uTheta);
    }

    private static Object[] correctionValues(double uX, double uY, double uTheta) {
        Vector2D worldCorrectionVec = new Vector2D(uX, uY, true);
        return new Object[]{ worldCorrectionVec, uTheta };
    }

    private static double pOut(ArrayList<double[]> eT, double kP) {
        double proportional = eT.get(eT.size() - 1)[1];
        return kP * proportional;
    }

    private static double iOut(ArrayList<double[]> eT, double kI) {
        double integral = 0;
        for (int i = 0; i < eT.size() - 1; i++) {
            double baseSum = eT.get(i)[1] + eT.get(i + 1)[1];
            double height = eT.get(i + 1)[0] - eT.get(i)[0];
            integral += 0.5 * height * baseSum;
        }
        return kI * integral;
    }

    private static double dOut(ArrayList<double[]> eT, double kD) {
        double derivative = 0;
        if (eT.size() >= 2) {
            derivative = (eT.get(eT.size() - 1)[1] - eT.get(eT.size() - 2)[1]) / (eT.get(eT.size() - 1)[0] - eT.get(eT.size() - 2)[0]);
        }
        return kD * derivative;
    }

}
