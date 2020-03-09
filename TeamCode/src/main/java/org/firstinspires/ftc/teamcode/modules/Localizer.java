package org.firstinspires.ftc.teamcode.modules;

import com.hyperion.common.Constants;
import com.hyperion.common.Utils;
import com.hyperion.motion.math.Vector2D;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;
import org.openftc.revextensions2.RevBulkData;

/**
 * Makes all calculations and position updates for robot field-centric coordinate grid movement
 */

public class Localizer {

    private Hardware hw;

    public RevBulkData bulkDataL;
    public RevBulkData bulkDataR;

    public double oldxlCounts;
    public double oldxrCounts;
    public double oldyCounts;
    public double oldT;
    public Vector2D lastTvel = new Vector2D();

    public RealMatrix AInv;

    public Localizer(Hardware hw) {
        this.hw = hw;
        this.hw.xLOdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        this.hw.xROdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        this.hw.yOdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        this.hw.xLOdo.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.hw.xROdo.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.hw.yOdo.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    public synchronized void update() {
        bulkDataL = hw.expansionHubL.getBulkInputData();
        bulkDataR = hw.expansionHubR.getBulkInputData();

        if (hw != null && bulkDataL != null && bulkDataR != null) {
            double xLNew = -bulkDataL.getMotorCurrentPosition(hw.xLOdo);
            double xRNew = -bulkDataR.getMotorCurrentPosition(hw.xROdo);
            double yNew = bulkDataL.getMotorCurrentPosition(hw.yOdo);
            double tNew = System.currentTimeMillis();
            double dxL = Constants.countsToM(xLNew - oldxlCounts);
            double dxR = Constants.countsToM(xRNew - oldxrCounts);
            double dy = Constants.countsToM(yNew - oldyCounts);
            double dT = (tNew - oldT) / 1000.0;
            oldxlCounts = xLNew;
            oldxrCounts = xRNew;
            oldyCounts = yNew;
            oldT = tNew;

            double omegaXl = dxL / Constants.ODO_WHEEL_RADIUS;
            double omegaXr = dxR / Constants.ODO_WHEEL_RADIUS;
            double omegaY = dy / Constants.ODO_WHEEL_RADIUS;

            RealMatrix omega = new Array2DRowRealMatrix(new double[][]{ new double[]{ omegaXl, omegaXr, omegaY } }).transpose();
            if (AInv == null) {
                RealMatrix A = new Array2DRowRealMatrix(new double[][]{
                        new double[]{ Math.cos(Constants.XL_REL.theta), Math.sin(Constants.XL_REL.theta), Constants.XL_REL.x * Math.sin(Constants.XL_REL.theta) - Constants.XL_REL.y * Math.cos(Constants.XL_REL.theta) },
                        new double[]{ Math.cos(Constants.XR_REL.theta), Math.sin(Constants.XR_REL.theta), Constants.XR_REL.x * Math.sin(Constants.XR_REL.theta) - Constants.XR_REL.y * Math.cos(Constants.XR_REL.theta) },
                        new double[]{ Math.cos(Constants.Y_REL.theta), Math.sin(Constants.Y_REL.theta), Constants.Y_REL.x * Math.sin(Constants.Y_REL.theta) - Constants.Y_REL.y * Math.cos(Constants.Y_REL.theta) }
                });
                AInv = MatrixUtils.inverse(A.scalarMultiply(1.0 / Constants.ODO_WHEEL_RADIUS));
            }
            double[] dR = AInv.multiply(omega).getColumn(0);

            Motion.robot.tVel = new Vector2D(dR[0], dR[1], true).scaled(1.0 / dT);
            Motion.robot.tAcc = new Vector2D(Math.abs(Motion.robot.tVel.magnitude - lastTvel.magnitude),
                                                      Utils.norm(Motion.robot.tVel.theta + (Motion.robot.tVel.magnitude < lastTvel.magnitude ? Math.PI : 0), 0, 2 * Math.PI),
                                                false).scaled(1.0 / dT);
            lastTvel = new Vector2D(Motion.robot.tVel);

            double dXr = Constants.mToCoords(dR[0]);
            double dYr = Constants.mToCoords(dR[1]);
            double theta = -Motion.robot.theta;
            Motion.robot.addXYT(dXr * Math.cos(theta) + dYr * Math.sin(theta),
                                     dYr * Math.cos(theta) - dXr * Math.sin(theta), dR[2]);
            Motion.robot.theta = Utils.norm(Motion.robot.theta, 0, 2 * Math.PI);
        }
    }

}
