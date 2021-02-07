package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * Manages all appendages (servos, arms, etc.) on robot
 */

public class Apndg {

    public static Gerald gerald;

    // Shooter
    public static DcMotor shooterL;
    public static DcMotor shooterR;
    public static Servo loader;

    // Wobble


    // Intake


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
        shooterL = Motion.bLDrive;
        shooterR = Motion.bRDrive;
        loader = gerald.hwmp.servo.get("loader");

        shooterL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Wobble


        // Intake

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
        switch (state) {
            case ON:
                shooterL.setPower(Constants.getDouble("apndg.shooter.power"));
                shooterR.setPower(Constants.getDouble("apndg.shooter.power"));
                break;
            case OFF:
                shooterL.setPower(0);
                shooterR.setPower(0);
                break;
        }
    }

    /**
     * Sets the loader to the given state
     *
     * @param  state  BACK | FORTH
     */
    public static void setLoader(State state) {
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
     * Loads a ring into the shooter
     */
    public static void loadRing() {
        setLoader(State.FORTH);
        gerald.ctx.sleep(500);
        setLoader(State.BACK);
    }

    /**
     * Loads and shoots a ring at a given angle
     *
     * @param  angleDeg  the angle to shoot at (0 - 90 deg)
     */
    public static void shoot(double angleDeg) {

    }

    public enum State {
        ON, OFF, BACK, FORTH, IN, OUT
    }

}
