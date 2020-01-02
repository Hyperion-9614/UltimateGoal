package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Utils;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Manages all appendages (servos, arms, etc.) on robot
 */

public class Appendages {

    public Hardware hardware;

    public String compWheelsStatus = "";
    public String foundationMoverStatus = "";
    public String chainBarStatus = "";
    public String clawStatus = "";

    public Appendages(Hardware hardware) {
        this.hardware = hardware;

        resetVerticalSlideEncoders();

        hardware.vertSlideL.setDirection(DcMotorSimple.Direction.REVERSE);
        hardware.compWheelsL.setDirection(DcMotorSimple.Direction.REVERSE);
        hardware.foundationMoverL.setDirection(Servo.Direction.REVERSE);
        hardware.chainBarL.setDirection(Servo.Direction.REVERSE);

        setCompWheelsStatus("stop");
        setFoundationMoverStatus("up");
        setChainBarStatus("in");
        setClawStatus("open");

        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.mode = BNO055IMU.SensorMode.IMU;
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled = false;
        hardware.imu.initialize(parameters);
    }

    //////////////////////// APPENDAGE CONTROL //////////////////////////

    public void setVerticalSlidePower(double power) {
        if ((hardware.vertSlideL.getCurrentPosition() > 0 && hardware.vertSlideR.getCurrentPosition() > 0)
            || (hardware.vertSlideL.getCurrentPosition() <= 0 && hardware.vertSlideR.getCurrentPosition() <= 0 && power > 0)) {
            hardware.vertSlideL.setPower(power);
            hardware.vertSlideR.setPower(power);
        } else {
            hardware.vertSlideL.setPower(0);
            hardware.vertSlideR.setPower(0);
        }
    }

    public void setVerticalSlideMode(DcMotor.RunMode mode) {
        hardware.vertSlideL.setMode(mode);
        hardware.vertSlideR.setMode(mode);
    }

    public void setVerticalSlideTarget(int target) {
        hardware.vertSlideL.setTargetPosition(target);
        hardware.vertSlideR.setTargetPosition(target);
    }

    public void setVerticalSlidePosition(int target) {
        setVerticalSlideTarget(target);
        setVerticalSlideMode(DcMotor.RunMode.RUN_TO_POSITION);

        setVerticalSlidePower(1);
        ElapsedTime timer = new ElapsedTime();
        while ((hardware.vertSlideL.isBusy() || hardware.vertSlideR.isBusy()) && timer.milliseconds() <= 4000
                && (hardware.context.gamepad1.left_stick_x == 0 && hardware.context.gamepad1.left_stick_y == 0 && hardware.context.gamepad1.right_stick_x == 0)) {
            hardware.motion.localizer.update();
            hardware.unimetry.update();
        }

        setVerticalSlidePower(0);
        setVerticalSlideMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void resetVerticalSlideEncoders() {
        setVerticalSlideMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        setVerticalSlideMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void setCompWheelsPower(double power) {
        hardware.compWheelsL.setPower(power);
        hardware.compWheelsR.setPower(power);
    }

    public void setCompWheelsStatus(String inStopOut) {
        compWheelsStatus = inStopOut.toLowerCase();
        double power = 0;
        switch (compWheelsStatus) {
            case "in":
                power = -1;
                break;
            case "out":
                power = 1;
                break;
        }
        setCompWheelsPower(power);
    }

    public void setFoundationMoverStatus(String downUp) {
        foundationMoverStatus = downUp.toLowerCase();
        if (foundationMoverStatus.equals("down")) {
            hardware.foundationMoverL.setPosition(1.0);
            hardware.foundationMoverR.setPosition(1.0);
        } else {
            hardware.foundationMoverL.setPosition(0);
            hardware.foundationMoverR.setPosition(0);
        }
    }

    public void setChainBarStatus(String inOut) {
        chainBarStatus = inOut.toLowerCase();
        if (chainBarStatus.equals("in")) {
            hardware.chainBarL.setPosition(0.9);
            hardware.chainBarR.setPosition(0.9);
        } else if (chainBarStatus.equals("noStone")){
            hardware.chainBarL.setPosition(0.8);
            hardware.chainBarR.setPosition(0.8);
        } else {
            hardware.chainBarL.setPosition(0.5);
            hardware.chainBarR.setPosition(0.5);

        }
    }

    public void setClawStatus(String openClosed) {
        clawStatus = openClosed.toLowerCase();
        if (clawStatus.equals("open")) {
            hardware.claw.setPower(1.0);
        } else {
            hardware.claw.setPower(-1.0);
        }
    }

}
