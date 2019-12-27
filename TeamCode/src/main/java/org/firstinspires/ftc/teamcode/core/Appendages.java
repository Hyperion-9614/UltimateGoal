package org.firstinspires.ftc.teamcode.core;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * Manages all appendages (servos, arms, etc.) on robot
 */

public class Appendages {

    public Hardware hardware;

    public String compWheelsStatus = "";
    public String foundationMoverStatus = "";
    public String clawStatus = "";

    public Appendages(Hardware hardware) {
        this.hardware = hardware;

        resetVerticalSlideEncoders();

        hardware.compWheelsL.setDirection(DcMotorSimple.Direction.REVERSE);

        setFoundationMoverStatus("up");
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
        hardware.vertSlideL.setPower(power);
        hardware.vertSlideR.setPower(power);
    }

    public void setVerticalSlideMode(DcMotor.RunMode mode) {
        hardware.vertSlideL.setMode(mode);
        hardware.vertSlideR.setMode(mode);
    }

    public void setVerticalSlideTarget(int target) {
        hardware.vertSlideL.setTargetPosition(target);
        hardware.vertSlideR.setTargetPosition(target);
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
            hardware.foundationMoverL.setPower(1.0);
            hardware.foundationMoverR.setPower(1.0);
        } else {
            hardware.foundationMoverL.setPower(0);
            hardware.foundationMoverR.setPower(0);
        }
    }

    public void setClawStatus(String openClosed) {
        clawStatus = openClosed.toLowerCase();
        if (clawStatus.equals("open")) {
            hardware.claw.setPower(1.0);
        } else {
            hardware.claw.setPower(0);
        }
    }

}
