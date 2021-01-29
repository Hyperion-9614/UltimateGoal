package org.firstinspires.ftc.teamcode.modules;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.firstinspires.ftc.teamcode.core.Gerald;
import org.firstinspires.ftc.teamcode.core.Motion;
import org.openftc.revextensions2.RevBulkData;

/**
 * Makes all calculations and position updates for robot field-centric coordinate grid movement
 * References
 *  (1) https://github.com/acmerobotics/road-runner/blob/master/doc/pdf/Mobile_Robot_Kinematics_for_FTC.pdf
 *  (2) https://www.sparknotes.com/math/precalc/conicsections/section5/#:~:text=A%20rotation%20of%20the%20coordinate,x%27%20and%20y%27%20axes.
 */

public class Localizer {

    private Gerald gerald;

    public RevBulkData bulkDataL;
    public RevBulkData bulkDataR;

    public double oldxlCounts;
    public double oldxrCounts;
    public double oldyCounts;
    public double oldT;
    public Vector2D lastTvel = new Vector2D();

    public RealMatrix AInv;

    /**
     * Resets & initializes odometry modules
     *
     * @param  gerald     the root hardware instance
     */
    public Localizer(Gerald gerald) {
        this.gerald = gerald;
    }

    /**
     * Called by odometry thread every 10 ms
     * <p>
     * -> Computes how much and in which direction the robot has moved
     * -> Adds the deltas onto the current pose
     */
    public synchronized void update() {
        bulkDataL = gerald.expansionHubL.getBulkInputData();
//        bulkDataR = gerald.expansionHubR.getBulkInputData();

        if (gerald != null && bulkDataL != null) {
            // Read in the change in encoder counts of each odometry tracker
            double xLNew = -bulkDataL.getMotorCurrentPosition(Motion.xLOdo);
            double xRNew = -bulkDataL.getMotorCurrentPosition(Motion.xROdo);
            double yNew = bulkDataL.getMotorCurrentPosition(Motion.yOdo);
            double tNew = System.currentTimeMillis();

            // Convert count deltas to meters
            double dxL = Constants.countsToM(xLNew - oldxlCounts);
            double dxR = Constants.countsToM(xRNew - oldxrCounts);
            double dy = Constants.countsToM(yNew - oldyCounts);
            double dT = (tNew - oldT) / 1000.0;
            oldxlCounts = xLNew;
            oldxrCounts = xRNew;
            oldyCounts = yNew;
            oldT = tNew;

            /*
             * Calculate ratios of change in odometry positions (meters) to radius of odometry wheels
             * Create 3x1 matrix with the omega values (transposed from 1x3)
             */
            double odoWheelRadius = Constants.getDouble("localization.odometryWheelRadius");
            double omegaXl = dxL / odoWheelRadius;
            double omegaXr = dxR / odoWheelRadius;
            double omegaY = dy / odoWheelRadius;
            RealMatrix omega = new Array2DRowRealMatrix(new double[][]{ new double[]{ omegaXl, omegaXr, omegaY } }).transpose();

            /*
             * Build A matrix (constant) if not already built
             * Essentially just a matrix containing relative positions of odometry modules to center of robot
             * Literally just plagiarized from
             * Multiply omega matrix with A to get a delta pose matrix
             */
            if (AInv == null) {
                Pose xL = Constants.getPose("localization.odometryPoses.xL");
                Pose xR = Constants.getPose("localization.odometryPoses.xR");
                Pose y = Constants.getPose("localization.odometryPoses.y");
                RealMatrix A = new Array2DRowRealMatrix(new double[][]{
                        new double[]{ Math.cos(xL.theta), Math.sin(xL.theta), xL.x * Math.sin(xL.theta) - xL.y * Math.cos(xL.theta) },
                        new double[]{ Math.cos(xR.theta), Math.sin(xR.theta), xR.x * Math.sin(xR.theta) - xR.y * Math.cos(xR.theta) },
                        new double[]{ Math.cos(y.theta), Math.sin(y.theta), y.x * Math.sin(y.theta) - y.y * Math.cos(y.theta) }
                });
                AInv = MatrixUtils.inverse(A.scalarMultiply(1.0 / odoWheelRadius));
            }
            double[] dR = AInv.multiply(omega).getColumn(0);

            /*
             * Calculate instantaneous translational velocity & acceleration based on change in pose & change in time
             */
            Motion.robot.tVel = new Vector2D(dR[0], dR[1], true).scaled(1.0 / dT);
            Motion.robot.tAcc = new Vector2D(Math.abs(Motion.robot.tVel.magnitude - lastTvel.magnitude),
                                                      MathUtils.norm(Motion.robot.tVel.theta + (Motion.robot.tVel.magnitude < lastTvel.magnitude ? Math.PI : 0), 0, 2 * Math.PI),
                                                 false).scaled(1.0 / dT);
            lastTvel = new Vector2D(Motion.robot.tVel);

            /*
             * Add to robot position object
             * Since dR is in the robot's coordinate frame, we essentially rotate the axes by -heading (+heading is the amount the axes are rotated by)
             * so that it aligns with the global coordinate grid
             * Refer to rotating coordinate grid axes - there are two formulas to convert an x, y point in one grid to an x', y' point in a rotated grid
             * Those two formulas are present in the addXYT call
             */
            double theta = -Motion.robot.theta;
            Motion.robot.addXYT(dR[0] * Math.cos(theta) + dR[1] * Math.sin(theta),
                                dR[1] * Math.cos(theta) - dR[0] * Math.sin(theta), dR[2]);
            Motion.robot.theta = MathUtils.norm(Motion.robot.theta, 0, 2 * Math.PI);
        }
    }

}
