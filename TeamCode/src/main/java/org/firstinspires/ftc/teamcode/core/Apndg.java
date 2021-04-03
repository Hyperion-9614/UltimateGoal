package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * Manages all appendages (servos, arms, etc.) on robot
 */

public class Apndg {

    public static Gerald gerald;

    // Shooter
    public static DcMotorEx shooterL;
    public static DcMotorEx shooterR;
    public static Servo flap;

    // Transfer
    public static Servo loader;
    public static Servo hopper;

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
        hopper = gerald.hwmp.servo.get("elevator");

        // Intake
//        intakeL = Motion.bLDrive;
//        intakeR = Motion.bRDrive;
//
//        intakeL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        intakeR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        intakeL.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
//        intakeR.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // Wobble

    }

    /**
     * Sets positions of appendages to init state
     */
    public static void initPositions() {
        setHopper(State.DOWN);
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
        double rpm = Constants.getDouble("apndg.shooter.rpm");
        switch (state) {
            case ON:
                shooterL.setVelocity(MathUtils.rpmToRad(rpm), AngleUnit.RADIANS);
                shooterR.setVelocity(-MathUtils.rpmToRad(rpm), AngleUnit.RADIANS);
                States.shooterLrpm = rpm;
                break;
            case OFF:
                shooterL.setVelocity(0);
                shooterR.setVelocity(0);
                States.shooterLrpm = 0;
                break;
        }
    }

    /**
     * Sets the elevator to the given angle with the horizontal
     *
     * @param  angleDeg  the angle with the horizontal to angle the shooter flap
     */
    public static void setFlap(double angleDeg) {
        States.flapDeg = angleDeg;
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
    public static void setHopper(State state) {
        States.hopper = state;
        switch (state) {
            case DOWN:
                hopper.setPosition(Constants.getDouble("apndg.hopper.down"));
                break;
            case UP:
                hopper.setPosition(Constants.getDouble("apndg.hopper.up"));
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
        gerald.ctx.sleep(200);
        setLoader(State.IN);
        gerald.ctx.sleep(100);
        setLoader(State.OUT);
        gerald.ctx.sleep(200);
    }

    /**
     * Loads and shoots a ring at a given angle
     *
     * @param  angleDeg  the angle to shoot at (0 - 90 deg)
     * @param  numTimes  number of times to load/shoot
     *                   input -1 if shooter should stay on indefinitely
     * @param  turnOff   should the shooter turn off after firing all rings
     */
    public static void shoot(double angleDeg, int numTimes, boolean turnOff) {
        gerald.status = "Shooting at angle " + angleDeg + "\u00B0 " + numTimes + " times";
        setFlap(angleDeg);
//        if (States.intake != State.OFF)
//            setIntake(State.OFF);
        if (States.hopper != State.UP)
            setHopper(State.UP);
        if (States.shooter != State.ON)
            setShooter(State.ON);
        gerald.ctx.sleep(500);
        for (int i = 0; i < numTimes; i++)
            loadRing();
        if (numTimes > 0 && turnOff) {
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
        public static double shooterLrpm;
        public static double flapDeg;
        public static State loader;
        public static State hopper;
        public static State intake;

    }

}
