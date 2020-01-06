package org.firstinspires.ftc.teamcode.opmodes;

import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.firstinspires.ftc.teamcode.core.Appendages;
import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;

/**
 * Main TeleOp
 * Select op mode then start
 * Controller map: http://team358.org/files/programming/ControlSystem2009-/XBoxControlMapping.jpg
 */

@TeleOp
public class TELE_Main extends LinearOpMode {

    private Hardware hardware;
    private Motion motion;
    private Appendages appendages;

    private boolean isHtStarted;
    private double preHtTheta;

    private boolean presetPlaceStoneToggle;
    private boolean resetVertSlidesToggle;
    private boolean intakeToggle;
    private boolean outtakeToggle;
    private boolean foundationMoverToggle;
    private boolean chainBarToggle;
    private boolean clawToggle;

    @Override
    public void runOpMode() {
        hardware = new Hardware(this);
        motion = hardware.motion;
        appendages = hardware.appendages;

        while (!isStopRequested() && (!isStarted() || (opModeIsActive() && !hardware.isRunning))) {
            if (gamepad1.b) {
                hardware.initOpMode("tele.red");
            } else if (gamepad1.x) {
                hardware.initOpMode("tele.blue");
            }
        }

        executeLoop();

        hardware.end();
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
                preHtTheta = motion.robot.pose.theta;
                isHtStarted = true;
            }
            if (isHtStarted) {
                vel.rotate(preHtTheta - motion.robot.pose.theta);
            }
            motion.setDrive(vel, rot);

            /*
             * GAMEPAD 1
             * LEFT TRIGGER : Vertical slides down
             * RIGHT TRIGGER : Vertical slides up
             */
            double vertSlidePower = Math.pow(gamepad1.right_trigger - gamepad1.left_trigger, 3);
            appendages.setVerticalSlidePower(vertSlidePower);

            /*
             * GAMEPAD 1
             * RIGHT STICK BUTTON : Reset vertical slide encoders
             */
            if (gamepad1.right_stick_button && !resetVertSlidesToggle) {
                appendages.resetVerticalSlideEncoders();
                resetVertSlidesToggle = true;
            } else if (!gamepad1.right_stick_button) {
                resetVertSlidesToggle = false;
            }

            /*
             * GAMEPAD 1
             * RIGHT BUMPER : Intake
             * LEFT BUMPER : Outtake
             */
            if (gamepad1.right_bumper && !intakeToggle) {
                appendages.setCompWheelsStatus(appendages.compWheelsStatus.equals("in") ? "stop" : "in");
                intakeToggle = true;
            } else if (!gamepad1.right_bumper) {
                intakeToggle = false;
            }
            if (gamepad1.left_bumper && !outtakeToggle) {
                appendages.setCompWheelsStatus(appendages.compWheelsStatus.equals("out") ? "stop" : "out");
                outtakeToggle = true;
            } else if (!gamepad1.left_bumper) {
                outtakeToggle = false;
            }

            /*
             * GAMEPAD 1
             * Y : Place stone preset
             */
            if (gamepad1.y && !presetPlaceStoneToggle) {
                hardware.preset_placeStone(true);
                presetPlaceStoneToggle = true;
            } else if (!gamepad1.y) {
                presetPlaceStoneToggle = false;
            }

            /*
             * GAMEPAD 1
             * A : Foundation mover toggle
             */
            if (gamepad1.a && !foundationMoverToggle) {
                appendages.setFoundationMoverStatus(appendages.foundationMoverStatus.equals("down") ? "up" : "down");
                foundationMoverToggle = true;
            } else if (!gamepad1.a) {
                foundationMoverToggle = false;
            }

            /*
             * GAMEPAD 1
             * B : Claw toggle
             */
            if (gamepad1.b && !clawToggle) {
                appendages.setClawStatus(appendages.clawStatus.equals("open") ? "closed" : "open");
//                appendages.setVerticalSlidePosition(1000);
                clawToggle = true;
            } else if (!gamepad1.b) {
                clawToggle = false;
            }

            /*
             * GAMEPAD 1
             * X : Chain bar toggle
             */
            if (gamepad1.x && !chainBarToggle) {
                appendages.cycleChainBar();
                chainBarToggle = true;
            } else if (!gamepad1.x) {
                chainBarToggle = false;
            }
        }
    }

}