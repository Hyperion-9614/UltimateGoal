package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
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

    // Intake & Transfer
    public static Servo loader;
    public static DcMotor intake;

    // Wobble
    public static DcMotorEx arm;
    public static Servo claw;

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
        shooterL = gerald.hwmp.get(DcMotorEx.class, "ShootL");
        shooterR = gerald.hwmp.get(DcMotorEx.class, "ShootR");
        flap = gerald.hwmp.servo.get("ShootLevel");

        shooterL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        setShooterPIDF();

        // Intake
        intake = gerald.hwmp.dcMotor.get("Intake");
        loader = gerald.hwmp.servo.get("shootServo");

        intake.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // Wobble
        arm = gerald.hwmp.get(DcMotorEx.class, "WobbleClaw");
        claw = gerald.hwmp.servo.get("blockArmL");

        arm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        arm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    /**
     * Set shooter velocity PIDF from constants
     */
    public static void setShooterPIDF() {
        double kP = Constants.getDouble("pid.shooterL.kP");
        double kI = Constants.getDouble("pid.shooterL.kI");
        double kD = Constants.getDouble("pid.shooterL.kD");
        double kF = Constants.getDouble("pid.shooterL.kF");
//        shooterL.setVelocityPIDFCoefficients(kP, kI, kD, kF);
//        shooterR.setVelocityPIDFCoefficients(kP, kI, kD, kF);
    }

    /**
     * Sets positions of appendages to init state
     */
    public static void initPositions() {
        setFlap(0);
        setClaw(State.CLOSED);
        setLoader(State.OUT);
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
     * @param  angleDeg  the angle with the horizontal to
     *                   angle the shooter flap (0 - 30 deg)
     *
     */
    public static void setFlap(double angleDeg) {
        States.flapDeg = angleDeg;
        flap.setPosition(0.19 + angleDeg / 30);
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
     * Sets the intake to the given state
     *
     * @param  state  IN | OFF | OUT
     */
    public static void setIntake(State state) {
        double power = Constants.getDouble("apndg.intake.power");
        States.intake = state;
        switch (state) {
            case IN:
                intake.setPower(-power);
                break;
            case OFF:
                intake.setPower(0);
                break;
            case OUT:
                intake.setPower(power);
                break;
        }
    }

    /**
     * Sets the arm to the given state
     *
     * @param  state  init | horizon | endgame | autolift | pickup
     */
    public static void setArm(String state) {
        States.arm = state;
        arm.setTargetPosition(Constants.getInt(new ID("apndg.arm.states", state).toString()));
        arm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        arm.setPower(Constants.getDouble("apndg.arm.power"));
        long start = System.currentTimeMillis();
        while (gerald.ctx.opModeIsActive() && !gerald.ctx.isStopRequested() && arm.isBusy()
               && (System.currentTimeMillis() - start) <= Constants.getLong("apdng.arm.timeoutMS")){}
        arm.setPower(0);
        arm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    /**
     * Sets the claw to the given state
     *
     * @param  state  OPEN | CLOSED
     */
    public static void setClaw(State state) {
        States.claw = state;
        switch (state) {
            case OPEN:
                claw.setPosition(Constants.getDouble("apndg.claw.open"));
                break;
            case CLOSED:
                claw.setPosition(Constants.getDouble("apndg.claw.closed"));
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
     * @param  angleDeg  the angle to shoot at (0 - 30 deg)
     * @param  numTimes  number of times to load/shoot
     *                   input -1 if shooter should stay on indefinitely
     * @param  turnOff   should the shooter turn off after firing all rings
     */
    public static void shoot(double angleDeg, int numTimes, boolean turnOff) {
        gerald.status = "Shooting at angle " + angleDeg + "\u00B0 " + numTimes + " times";
        setFlap(angleDeg);
        if (States.intake != State.OFF)
            setIntake(State.OFF);
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
        ON, OFF, IN, OUT, OPEN, CLOSED
    }

    public static class States {

        public static State shooter;
        public static double shooterLrpm;
        public static double flapDeg;
        public static State loader;
        public static State intake;
        public static String arm;
        public static State claw;

    }

}
