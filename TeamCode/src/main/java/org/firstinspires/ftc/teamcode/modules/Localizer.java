package org.firstinspires.ftc.teamcode.modules;

import com.hyperion.common.Utils;
import com.hyperion.motion.math.Vector2D;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.firstinspires.ftc.teamcode.core.Hardware;
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
    public Vector2D maxAccel = new Vector2D();
    public Vector2D maxDecel = new Vector2D();
    public Vector2D maxVel = new Vector2D();

    public RealMatrix AInv;

    public Localizer(Hardware hw) {
        this.hw = hw;
        this.hw.xLOdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        this.hw.xROdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        this.hw.yOdo.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        this.hw.xLOdo.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.hw.xROdo.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.hw.yOdo.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        RealMatrix A = new Array2DRowRealMatrix(new double[][]{
                new double[]{ Math.cos(hw.constants.XL_REL.theta), Math.sin(hw.constants.XL_REL.theta), hw.constants.XL_REL.x * Math.sin(hw.constants.XL_REL.theta) - hw.constants.XL_REL.y * Math.cos(hw.constants.XL_REL.theta) },
                new double[]{ Math.cos(hw.constants.XR_REL.theta), Math.sin(hw.constants.XR_REL.theta), hw.constants.XR_REL.x * Math.sin(hw.constants.XR_REL.theta) - hw.constants.XR_REL.y * Math.cos(hw.constants.XR_REL.theta) },
                new double[]{ Math.cos(hw.constants.Y_REL.theta), Math.sin(hw.constants.Y_REL.theta), hw.constants.Y_REL.x * Math.sin(hw.constants.Y_REL.theta) - hw.constants.Y_REL.y * Math.cos(hw.constants.Y_REL.theta) }
        });
        AInv = MatrixUtils.inverse(A.scalarMultiply(1.0 / hw.constants.ODO_WHEEL_RADIUS));
    }

    public synchronized void update() {
        bulkDataL = hw.expansionHubL.getBulkInputData();
        bulkDataR = hw.expansionHubR.getBulkInputData();

        if (hw != null && bulkDataL != null && bulkDataR != null) {
            double xLNew = bulkDataL.getMotorCurrentPosition(hw.xLOdo);
            double xRNew = bulkDataR.getMotorCurrentPosition(hw.xROdo);
            double yNew = bulkDataL.getMotorCurrentPosition(hw.yOdo);
            double tNew = System.currentTimeMillis();
            double dxL = hw.constants.countsToM(xLNew - oldxlCounts);
            double dxR = hw.constants.countsToM(xRNew - oldxrCounts);
            double dy = hw.constants.countsToM(yNew - oldyCounts);
            double dT = (tNew - oldT) / 1000.0;
            oldxlCounts = xLNew;
            oldxrCounts = xRNew;
            oldyCounts = yNew;
            oldT = tNew;

            double omegaXl = dxL / hw.constants.ODO_WHEEL_RADIUS;
            double omegaXr = dxR / hw.constants.ODO_WHEEL_RADIUS;
            double omegaY = dy / hw.constants.ODO_WHEEL_RADIUS;

            RealMatrix omega = new Array2DRowRealMatrix(new double[][]{ new double[]{ omegaXl, omegaXr, omegaY } }).transpose();
            double[] dR = AInv.multiply(omega).getColumn(0);

            hw.motion.robot.tVel = new Vector2D(dR[0], dR[1], true).scaled(1.0 / dT);
            hw.motion.robot.tAcc = new Vector2D(Math.abs(hw.motion.robot.tVel.magnitude - lastTvel.magnitude),
                                                     Utils.normalizeTheta(hw.motion.robot.tVel.theta + (hw.motion.robot.tVel.magnitude < lastTvel.magnitude ? Math.PI : 0), 0, 2 * Math.PI),
                                                     false).scaled(1.0 / dT);

            if (hw.motion.robot.tVel.magnitude > maxVel.magnitude) {
                maxVel = new Vector2D(hw.motion.robot.tVel);
            }
            if (hw.motion.robot.tAcc.theta == hw.motion.robot.tVel.theta + Math.PI && hw.motion.robot.tAcc.magnitude >= maxDecel.magnitude) {
                maxDecel = new Vector2D(hw.motion.robot.tAcc);
            } else if (hw.motion.robot.tAcc.theta == hw.motion.robot.tVel.theta && hw.motion.robot.tAcc.magnitude >= maxAccel.magnitude) {
                maxAccel = new Vector2D(hw.motion.robot.tAcc);
            }

            lastTvel = new Vector2D(hw.motion.robot.tVel);
            hw.motion.robot.pose.addXYT(hw.constants.mToCoords(dR[0]), hw.constants.mToCoords(dR[1]), dR[2]);
            hw.motion.robot.pose.theta = Utils.normalizeTheta(hw.motion.robot.pose.theta, 0, 2 * Math.PI);
        }
    }

}
