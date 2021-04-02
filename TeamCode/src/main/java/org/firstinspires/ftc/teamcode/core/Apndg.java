package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * Manages all appendages (servos, arms, etc.) on robot
 */

public class Apndg {

    public static Gerald gerald;

    // Shooter
    public static DcMotor shooterL;
    public static DcMotor shooterR;
    public static Servo flap;

    // Transfer
    public static Servo loader;
    public static Servo elevator;

    // Intake
    public static DcMotor intakeL;
    public static DcMotor intakeR;

    // Wobble


    public static void init(Gerald gerald) {
        Apndg.gerald = gerald;
        initHW();
        initPositions();
    }

    //////////////////////// INIT //////////////////////////

    /**
     * Init appendage hardware
     */
    public static void initHW() {
        // Shooter
        shooterL = Motion.fLDrive;
        shooterR = Motion.fRDrive;
        flap = gerald.hwmp.servo.get("flap");

        shooterL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Transfer
        loader = gerald.hwmp.servo.get("loader");
        elevator = gerald.hwmp.servo.get("elevator");

        // Intake
//        intakeL = Motion.bLDrive;
//        intakeR = Motion.bRDrive;
//
//        intakeL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        intakeR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        intakeL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        intakeR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        intakeR.setDirection(DcMotorSimple.Direction.REVERSE);

        // Wobble

    }

    /**
     * Sets positions of appendages to init state
     */
    public static void initPositions() {
        setElevator(State.DOWN);
        setLoader(State.OUT);
        setFlap(45);
    }

    /**
     * Sets the shooter to the given state
     *
     * @param  state  ON | OFF
     */
    public static void setShooter(State state) {
        States.shooter = state;
        double power = Constants.getDouble("apndg.shooter.power");
        switch (state) {
            case ON:
                shooterL.setPower(power);
                shooterR.setPower(-power);
                break;
            case OFF:
                shooterL.setPower(0);
                shooterR.setPower(0);
                break;
        }
    }

    /**
     * Sets the elevator to the given angle with the horizontal
     *
     * @param  angleDeg  the angle with the horizontal to angle the shooter flap
     */
    public static void setFlap(double angleDeg) {
        States.flap = angleDeg;
    }

    /**
     * Sets the loader to the given state
     *
     * @param  state  OUT | IN
     */
    public static void setLoader(State state) {
        States.loader = state;
        switch (state) {
            case OUT:
                loader.setPosition(Constants.getDouble("apndg.loader.out"));
                break;
            case IN:
                loader.setPosition(Constants.getDouble("apndg.loader.in"));
                break;
        }
    }

    /**
     * Sets the elevator to the given state
     *
     * @param  state  DOWN | UP
     */
    public static void setElevator(State state) {
        States.elevator = state;
        switch (state) {
            case DOWN:
                elevator.setPosition(Constants.getDouble("apndg.elevator.down"));
                break;
            case UP:
                elevator.setPosition(Constants.getDouble("apndg.elevator.up"));
                break;
        }
    }

    /**
     * Sets the intake to the given state
     *
     * @param  state  IN | OFF | OUT
     */
    public static void setIntake(State state) {
        double power = Constants.getDouble("apndg.intake.power");
        States.intake = state;
        switch (state) {
            case IN:
                intakeL.setPower(-power);
                intakeR.setPower(-power);
                break;
            case OFF:
                intakeL.setPower(0);
                intakeR.setPower(0);
                break;
            case OUT:
                intakeL.setPower(power);
                intakeR.setPower(power);
                break;
        }
    }

    /**
     * Loads a ring into the shooter
     */
    public static void loadRing() {
        if (States.loader != State.OUT)
            setLoader(State.OUT);
        gerald.ctx.sleep(500);
        setLoader(State.IN);
        gerald.ctx.sleep(500);
        setLoader(State.OUT);
    }

    /**
     * Loads and shoots a ring at a given angle
     *
     * @param  angleDeg  the angle to shoot at (0 - 90 deg)
     * @param  numTimes  number of times to load/shoot
     *                   input -1 if shooter should stay on indefinitely
     */
    public static void shoot(double angleDeg, int numTimes) {
        gerald.status = "Shooting at angle " + angleDeg + "\u00B0 " + numTimes + " times";
        setFlap(angleDeg);
//        if (States.intake != State.OFF)
//            setIntake(State.OFF);
        if (States.elevator != State.UP)
            setElevator(State.UP);
        setShooter(State.ON);
        gerald.ctx.sleep(500);
        for (int i = 0; i < numTimes; i++)
            loadRing();
        if (numTimes > 0) {
            setShooter(State.OFF);
            gerald.clearStatus();
        } else {
            gerald.status = "Shooter enabled indefinitely";
        }
    }

    public enum State {
        ON, OFF, IN, OUT, DOWN, UP
    }

    public static class States {

        public static State shooter;
        public static double flap;
        public static State loader;
        public static State elevator;
        public static State intake;

    }

}
