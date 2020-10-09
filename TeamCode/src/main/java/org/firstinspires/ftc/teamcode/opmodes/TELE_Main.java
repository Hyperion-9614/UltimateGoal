package org.firstinspires.ftc.teamcode.opmodes;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Vector2D;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;

/**
 * Main TeleOp
 * Select op mode then start
 * Controller map: http://team358.org/files/programming/ControlSystem2009-/XBoxControlMapping.jpg
 */

@TeleOp
public class TELE_Main extends LinearOpMode {

    private Hardware hw;
    private boolean isHtStarted;
    private double preHtTheta;

    @Override
    public void runOpMode() {
        hw = new Hardware(this);
        while (!isStopRequested() && (!isStarted() || (opModeIsActive() && !hw.isRunning))) {
            hw.initLoop(false);
        }
        executeLoop();
        hw.end();
    }

    public void executeLoop() {
        while (opModeIsActive() && !isStopRequested()) {
            /*
             * GAMEPAD 1
             * LEFT JOYSTICK : Translation in direction of joystick, relative to robot
             * RIGHT JOYSTICK : Pivot in direction of joystick, relative to robot
             */
            Vector2D vel = new Vector2D(gamepad1.left_stick_x, -gamepad1.left_stick_y, true);
            double rot = gamepad1.right_stick_x;
            if (rot == 0) {
                isHtStarted = false;
            } else if (rot != 0 && !isHtStarted) {
                preHtTheta = Motion.robot.theta;
                isHtStarted = true;
            }
            if (isHtStarted) {
                vel.rotate(preHtTheta - Motion.robot.theta);
            }
            Motion.setDrive(vel, rot);

            if (hw.opModeID.contains("test")) {
                if (gamepad1.right_stick_button) {
                    double maxTransVel = 0;
                    double maxTransAcc = 0;
                    double maxAngVel = 0;
                    double maxAngAcc = 0;

                    Motion.setDrive(1, 1);
                    ElapsedTime timer = new ElapsedTime();
                    while (timer.milliseconds() <= 1000) {
                        maxTransVel = Math.max(maxTransVel, Motion.robot.tVel.magnitude);
                        maxTransAcc = Math.max(maxTransAcc, Motion.robot.tAcc.magnitude);
                        maxAngVel = Math.max(maxAngVel, Motion.robot.aVel);
                        maxAngAcc = Math.max(maxAngAcc, Motion.robot.aAcc);
                    }

                    System.out.println("MAX MOTION VALUES");
                    System.out.println("tVel: " + MathUtils.round(maxTransVel, 3));
                    System.out.println("tAcc: " + MathUtils.round(maxTransAcc, 3));
                    System.out.println("aVel: " + MathUtils.round(maxAngVel, 3));
                    System.out.println("aAcc: " + MathUtils.round(maxAngAcc, 3));
                }
            }
        }
    }

}