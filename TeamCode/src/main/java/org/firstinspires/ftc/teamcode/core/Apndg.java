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
    public static DcMotor shooterF;
    public static DcMotor shooterB;
    public static Servo loader;

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
//        shooterF = Motion.bLDrive;
//        shooterB = Motion.bRDrive;
//        loader = gerald.hwmp.servo.get("loader");
//
//        shooterF.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        shooterB.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        shooterF.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        shooterB.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

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
        setShooter(State.OFF);
        setLoader(State.BACK);
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
                shooterF.setPower(-power);
                shooterB.setPower(-power);
                break;
            case OFF:
                shooterF.setPower(0);
                shooterB.setPower(0);
                break;
        }
    }

    /**
     * Sets the loader to the given state
     *
     * @param  state  BACK | FORTH
     */
    public static void setLoader(State state) {
        States.loader = state;
        switch (state) {
            case BACK:
                loader.setPosition(Constants.getDouble("apndg.loader.back"));
                break;
            case FORTH:
                loader.setPosition(Constants.getDouble("apndg.loader.forth"));
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
        setLoader(State.FORTH);
        gerald.ctx.sleep(500);
        setLoader(State.BACK);
        gerald.ctx.sleep(500);
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
        // change flap
        if (States.intake != State.OFF)
            setIntake(State.OFF);
        setShooter(State.ON);
        for (int i = 0; i < numTimes; i++)
            loadRing();
        if (numTimes > 0)
            setShooter(State.OFF);
        gerald.clearStatus();
    }

    public enum State {
        ON, OFF, BACK, FORTH, IN, OUT
    }

    public static class States {

        public static State shooter;
        public static State loader;
        public static State intake;

    }

}
