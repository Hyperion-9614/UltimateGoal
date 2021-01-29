package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * Manages all appendages (servos, arms, etc.) on robot
 */

public class Appendages {

    public static Gerald gerald;

    // Shooter
    public static DcMotor shooterL;
    public static DcMotor shooterR;
    public static Servo shooterLoader;
    public static Servo shooterFlap;

    // Wobble
    public static DcMotor wobbleArm;
    public static Servo wobbleClaw;

    // Intake
    public static DcMotor intake;

    public static void init(Gerald gerald) {
        Appendages.gerald = gerald;
        initHW();
        initPositions();
    }

    //////////////////////// INIT //////////////////////////

    // Init appendage hardware
    public static void initHW() {
        // Shooter
//        shooterL = gerald.hwmp.dcMotor.get("ShootR");
//        shooterR = gerald.hwmp.dcMotor.get("ShootL");
//        shooterLoader = gerald.hwmp.servo.get("shootServo");
//        shooterFlap = gerald.hwmp.servo.get("ShootLevel");
//
//        shooterL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        shooterR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        shooterL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
//        shooterR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Wobble
//        wobbleArm = gerald.hwmp.dcMotor.get("WobbleClaw");
//        wobbleClaw = gerald.hwmp.servo.get("WBGriper");
//
//        wobbleArm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//        wobbleArm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Intake
//        intake = gerald.hwmp.dcMotor.get("Intake");
//
//        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    // Set start positions of appendages
    public static void initPositions() {
//        shooterLoader.setPosition(Constants.getDouble("apndg.loader.park"));
//        shooterFlap.setPosition(Constants.getDouble("apndg.shooter.park"));
//        wobbleClaw.setPosition(Constants.getDouble("apndg.wobble.claw.park"));
    }

    public static void shoot(double angleDeg) {

    }

}
