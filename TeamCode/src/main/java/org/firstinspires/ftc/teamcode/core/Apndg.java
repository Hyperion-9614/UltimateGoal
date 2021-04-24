package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Pose;
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
    public static Servo angler;

    // Intake & Transfer
    public static Servo loader;
    public static DcMotorEx intake;
    public static Servo mustacheL;
    public static Servo mustacheR;

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
        angler = gerald.hwmp.servo.get("ShootAngle");

        shooterL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        setShooterPIDF();

        // Intake
        intake = gerald.hwmp.get(DcMotorEx.class, "Intake");
        loader = gerald.hwmp.servo.get("shootServo");
        mustacheL = gerald.hwmp.servo.get("blockArmL");
        mustacheR = gerald.hwmp.servo.get("blockArmR");

        intake.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // Wobble
        arm = gerald.hwmp.get(DcMotorEx.class, "WobbleClaw");
        claw = gerald.hwmp.servo.get("WBGriper");

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
        setFlap(Constants.getDouble("apndg.flap.origin"));
        setAngler(Constants.getDouble("apndg.angler.origin"));
        setLoader(State.OUT);
        setMustache(State.UP);
        setClaw(State.CLOSED);
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
                shooterL.setVelocity(-MathUtils.rpmToRad(rpm), AngleUnit.RADIANS);
                shooterR.setVelocity(-MathUtils.rpmToRad(rpm), AngleUnit.RADIANS);
                States.shooterLrpm = rpm;
                gerald.ctx.sleep(500);
                break;
            case OFF:
                shooterL.setVelocity(0);
                shooterR.setVelocity(0);
                States.shooterLrpm = 0;
                break;
        }
    }

    /**
     * Sets the shooter flap to the given position
     *
     * @param  pos  the flap servo position
     */
    public static void setFlap(double pos) {
        States.flap = pos;
        flap.setPosition(pos);
    }

    /**
     * Sets the shooter angler to the given position
     *
     * @param  pos  the angler servo position
     */
    public static void setAngler(double pos) {
        States.flap = pos;
        angler.setPosition(pos);
    }

    /**
     * Returns the optimal flap, angler, and robot theta to
     * accurately score in the high goal
     */
    public static double[] getOptimalShooterConfig() {
        Pose shoot = Motion.getWaypoint("shoot");
        double dX = Motion.robot.x - shoot.x;
        double dY = Motion.robot.y - shoot.y;
        double dTheta = Motion.robot.theta - shoot.theta;

        double flapMin = Constants.getDouble("apndg.flap.min");
        double flapO = Constants.getDouble("apndg.flap.origin");
        double flapMax = Constants.getDouble("apndg.flap.max");

        double anglerMin = Constants.getDouble("apndg.angler.min");
        double anglerO = Constants.getDouble("apndg.angler.origin");
        double anglerMax = Constants.getDouble("apndg.angler.max");

        double[] conf = { flapO, anglerO, Motion.robot.theta };
        double dYs = MathUtils.clip(dY / 91.44, -0.3, 1);
        double dXs = MathUtils.clip(dX / 91.44, -1, 1);

        // Assuming larger flap pos = greater angle
        // && larger angler pos = further right
        if (dY > 0)
            conf[0] += dYs * (flapMax - flapO);
        else
            conf[0] += dYs * (flapO - flapMin);

        if (dX > 0)
            conf[1] -= dXs * (anglerO - anglerMin);
        else
            conf[1] -= dXs * (anglerMax - anglerO);

        return conf;
    }

    /**
     * Sets the loader to the given state
     *
     * @param  state  OUT | IN
     */
    public static void setLoader(State state) {
        States.loader = state;
        loader.setPosition(Constants.getDouble(new ID("apndg", "loader", state.toString().toLowerCase()).toString()));
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
     * Sets the mustache
     *
     * @param  state  DOWN | UP
     */
    public static void setMustache(State state) {
        States.mustache = state;
        mustacheL.setPosition(Constants.getDouble(new ID("apndg", "mustache", state.toString().toLowerCase(), "L").toString()));
        mustacheR.setPosition(Constants.getDouble(new ID("apndg", "mustache", state.toString().toLowerCase(), "R").toString()));
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
        claw.setPosition(Constants.getDouble(new ID("apndg", "claw", state.toString().toLowerCase()).toString()));
    }

    /**
     * Loads a ring into the shooter
     */
    public static void loadRing() {
        if (States.loader != State.OUT)
            setLoader(State.OUT);
        gerald.ctx.sleep(100);
        setLoader(State.IN);
        gerald.ctx.sleep(275);
        setLoader(State.OUT);
        gerald.ctx.sleep(50);
    }

    /**
     * Loads and shoots a ring at a given angle
     *
     * @param  flapDeg  the angle to set the flap to (0 - 45 deg)
     * @param  anglerDeg  the angle to set the angler to (0 - 30 deg)
     * @param  numTimes  number of times to load/shoot
     *                   input -1 if shooter should stay on indefinitely
     * @param  turnOff   should the shooter turn off after firing all rings
     */
    public static void shoot(double flapDeg, double anglerDeg, int numTimes, boolean turnOff) {
        gerald.status = "Shooting at flap " + flapDeg + "\u00B0 " + "angler " + anglerDeg + "\u00B0 " + numTimes + " times";
        setFlap(flapDeg);
        setAngler(anglerDeg);
        if (States.intake != State.OFF)
            setIntake(State.OFF);
        if (States.shooter != State.ON)
            setShooter(State.ON);
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
        ON, OFF, IN, OUT, OPEN, CLOSED, DOWN, UP
    }

    public static class States {

        public static State shooter = State.OFF;
        public static double shooterLrpm;
        public static double flap;
        public static double anglerDeg;
        public static State loader = State.OUT;
        public static State intake = State.OFF;
        public static State mustache = State.UP;
        public static String arm = "init";
        public static State claw = State.CLOSED;

    }

}
