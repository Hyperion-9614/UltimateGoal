package org.firstinspires.ftc.teamcode.opmodes;

import com.hyperion.motion.math.Vector2D;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

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

    private Hardware hw;
    private Motion motion;
    private Appendages appendages;

    private boolean isHtStarted;
    private double preHtTheta;

    private boolean resetVertSlidesToggle;
    private boolean intakeToggle;
    private boolean outtakeToggle;
    private boolean foundationMoverToggle;
    private boolean chainBarToggle;
    private boolean clawToggle;
    private boolean capstoneToggle;

    private String[] opModeIDs = new String[]{ "tele.blue", "tele.red" };
    private int opModeSelectorIndex = -1;

    @Override
    public void runOpMode() {
        hw = new Hardware(this);
        motion = hw.motion;
        appendages = hw.appendages;

        while (!isStopRequested() && (!isStarted() || (opModeIsActive() && !hw.isRunning))) {
            if (gamepad1.dpad_up) {
                opModeSelectorIndex++;
                if (opModeSelectorIndex >= opModeIDs.length)
                    opModeSelectorIndex = 0;
                hw.initOpMode(opModeIDs[opModeSelectorIndex]);
                sleep(250);
            } else if (gamepad1.dpad_down) {
                opModeSelectorIndex--;
                if (opModeSelectorIndex < 0)
                    opModeSelectorIndex = opModeIDs.length - 1;
                hw.initOpMode(opModeIDs[opModeSelectorIndex]);
                sleep(250);
            }
        }
        hw.initOpMode(opModeIDs[opModeSelectorIndex]);

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
                preHtTheta = motion.robot.pose.theta;
                isHtStarted = true;
            }
            if (isHtStarted) {
                vel.rotate(preHtTheta - motion.robot.pose.theta);
            }
            motion.setDrive(vel, rot);

            /*
             * GAMEPAD 1
             * RIGHT BUMPER : Intake
             * LEFT BUMPER : Outtake
             */
            if (gamepad1.right_bumper && !intakeToggle) {
                appendages.setCompWheelsStatus(appendages.compWheelsStatus.equals("in") ? "off" : "in");
                intakeToggle = true;
            } else if (!gamepad1.right_bumper) {
                intakeToggle = false;
            }
            if (gamepad1.left_bumper && !outtakeToggle) {
                appendages.setCompWheelsStatus(appendages.compWheelsStatus.equals("out") ? "off" : "out");
                outtakeToggle = true;
            } else if (!gamepad1.left_bumper) {
                outtakeToggle = false;
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
             * GAMEPAD 2
             * LEFT TRIGGER : Vertical slides down
             * RIGHT TRIGGER : Vertical slides up
             */
            double vertSlidePower = Math.pow(gamepad2.right_trigger - gamepad2.left_trigger, 3);
            appendages.setVerticalSlidePower(vertSlidePower);

            /*
             * GAMEPAD 2
             * DPAD UP : Go to save position/increment slides ticks
             * DPAD DOWN : Go to save position/decrement slides ticks
             */
            if (gamepad2.dpad_up) {
                appendages.setVerticalSlidePosition(Math.max(hw.vertSlideL.getCurrentPosition(), appendages.slidesSaveTicks));
                appendages.slidesSaveTicks += hw.constants.SLIDES_PRESET_INCREMENT_TICKS;
            }
            if (gamepad2.dpad_down) {
                appendages.setVerticalSlidePosition(Math.max(hw.vertSlideL.getCurrentPosition(), appendages.slidesSaveTicks));
                appendages.slidesSaveTicks -= hw.constants.SLIDES_PRESET_INCREMENT_TICKS;
            }
            
            /*
             * GAMEPAD 2
             * B : Claw toggle
             */
            if (gamepad2.b && !clawToggle) {
                appendages.setClawStatus(appendages.clawStatus.equals("open") ? "closed" : "open");
                clawToggle = true;
            } else if (!gamepad2.b) {
                clawToggle = false;
            }

            /*
             * GAMEPAD 2
             * X : Chain bar toggle
             */
            if (gamepad2.x && !chainBarToggle) {
                appendages.cycleChainBar();
                chainBarToggle = true;
            } else if (!gamepad2.x) {
                chainBarToggle = false;
            }

            /*
             * GAMEPAD 2
             * RIGHT STICK BUTTON : Reset vertical slide encoders
             */
            if (gamepad2.right_stick_button && !resetVertSlidesToggle) {
                appendages.resetVerticalSlideEncoders();
                resetVertSlidesToggle = true;
            } else if (!gamepad2.right_stick_button) {
                resetVertSlidesToggle = false;
            }

            /*
             * GAMEPAD 2
             * Y : Capstone toggle
             */
            if (gamepad2.y && !capstoneToggle) {
                appendages.setCapstoneStatus(appendages.capstoneStatus.equals("down") ? "up" : "down");
                capstoneToggle = true;
            } else if (!gamepad2.y) {
                capstoneToggle = false;
            }
        }
    }

}