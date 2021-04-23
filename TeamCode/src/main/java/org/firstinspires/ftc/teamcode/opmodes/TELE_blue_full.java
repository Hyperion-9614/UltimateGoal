package org.firstinspires.ftc.teamcode.opmodes;

import com.hyperion.motion.math.Vector2D;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.Apndg;
import org.firstinspires.ftc.teamcode.core.Apndg.State;
import org.firstinspires.ftc.teamcode.core.Gerald;
import org.firstinspires.ftc.teamcode.core.Motion;

/**
 * Main TeleOp
 * Select op mode then start
 * Controller map: http://team358.org/files/programming/ControlSystem2009-/XBoxControlMapping.jpg
 */

@TeleOp
public class TELE_blue_full extends LinearOpMode {

    private Gerald gerald;
    private boolean isHtStarted;
    private double preHtTheta;

    // Toggles, switches, & counters
    private State intakeDir = State.IN;
    private boolean t_right_bump;
    private boolean t_left_bump;
    private boolean wasIntakeRunning;

    @Override
    public void runOpMode() {
        gerald = new Gerald(this, "tele.blue.full");
        waitForStart();
        gerald.status = "Running OpMode " + gerald.opModeID.toString();
        while (opModeIsActive() && !isStopRequested() && gerald.isRunning) {
            executeLoop();
        }
        gerald.end();
    }

    public void executeLoop() {
        /*
         * GAMEPAD 1
         * LEFT JOYSTICK : Translation in direction of joystick, relative to robot
         * RIGHT JOYSTICK : Pivot in direction of joystick, relative to robot
         */
        Vector2D vel = new Vector2D(gamepad1.left_stick_x, -gamepad1.left_stick_y, true).rotated(-Math.PI / 2);
        double rot = -gamepad1.right_stick_x;
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

        /*
         * GAMEPAD 1
         * RIGHT BUMPER: Toggle intake ON/OFF
         * LEFT BUMPER: Switch intake direction
         */
        if (gamepad1.right_bumper && !t_right_bump) {
            t_right_bump = true;
            Apndg.setIntake((Apndg.States.intake != State.OFF) ? State.OFF : intakeDir);
        } else if (!gamepad1.right_bumper && t_right_bump) {
            t_right_bump = false;
        }
        if (gamepad1.left_bumper && !t_left_bump) {
            t_left_bump = true;
            intakeDir = (intakeDir == State.IN) ? State.OUT : State.IN;
        } else if (!gamepad1.left_bumper && t_left_bump) {
            t_left_bump = false;
        }

        /*
         * GAMEPAD 1
         * RIGHT TRIGGER: Shoot indefinitely
         */
        if (gamepad1.right_trigger > 0) {
            double[] conf = Apndg.getOptimalShooterConfig();
            Apndg.setFlap(conf[0]);
            Apndg.setAngler(conf[1]);
            Motion.rotate(conf[2]);
            if (Apndg.States.intake != State.OFF) {
                Apndg.setIntake(State.OFF);
                wasIntakeRunning = true;
            }
            if (Apndg.States.shooter != State.ON)
                Apndg.setShooter(State.ON);
            Apndg.loadRing();
        } else {
            Apndg.setShooter(State.OFF);
            if (wasIntakeRunning) {
                Apndg.setIntake(intakeDir);
                wasIntakeRunning = false;
            }
        }

        /*
         * GAMEPAD 1
         * Y: Toggle mustache
         */
        if (gamepad1.y) {
            Apndg.setMustache(Apndg.States.mustache == State.UP ? State.DOWN : State.UP);
        }
    }

}