package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Manages all appendages (servos, arms, etc.) on robot
 */

public class Appendages {

    public static Hardware hw;
    public static int slidesSaveTicks;

    public static String compWheelsStatus = "";
    public static String foundationMoverStatus = "";
    public static String chainBarStatus = "";
    public static String clawStatus = "";
    public static String capstoneStatus = "";

    public static void init(Hardware hardware) {
        hw = hardware;
        slidesSaveTicks = Constants.SLIDES_PRESET_START_TICKS;

        resetVerticalSlideEncoders();

        hw.compWheelsL.setDirection(DcMotorSimple.Direction.REVERSE);
        hw.foundationMoverL.setDirection(Servo.Direction.REVERSE);
        hw.chainBarL.setDirection(Servo.Direction.REVERSE);

        setCompWheelsStatus("stop");
        setFoundationMoverStatus("up");
        setClawStatus("open");
        setChainBarStatus("up");
        setCapstoneStatus("up");
    }

    //////////////////////// APPENDAGE CONTROL //////////////////////////

    public static void setVerticalSlidePower(double power) {
        hw.vertSlideL.setPower(power);
        hw.vertSlideR.setPower(power);
    }

    public static void setVerticalSlideMode(DcMotor.RunMode mode) {
        hw.vertSlideL.setMode(mode);
        hw.vertSlideR.setMode(mode);
    }

    public static void setVerticalSlideTarget(int target) {
        hw.vertSlideL.setTargetPosition(target);
        hw.vertSlideR.setTargetPosition(target);
    }

    public static void setVerticalSlidePosition(int target) {
        setVerticalSlideTarget(target);
        setVerticalSlideMode(DcMotor.RunMode.RUN_TO_POSITION);

        setVerticalSlidePower(1);
        ElapsedTime timer = new ElapsedTime();
        while ((hw.vertSlideL.isBusy() || hw.vertSlideR.isBusy()) && timer.milliseconds() <= 4000
                && (hw.context.gamepad1.left_stick_x == 0 && hw.context.gamepad1.left_stick_y == 0 && hw.context.gamepad1.right_stick_x == 0)) {

        }

        setVerticalSlidePower(0);
        setVerticalSlideMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public static void resetVerticalSlideEncoders() {
        setVerticalSlideMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        setVerticalSlideMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public static void setCompWheelsPower(double power) {
        hw.compWheelsL.setPower(power);
        hw.compWheelsR.setPower(power);
    }

    public static void setCompWheelsStatus(String inOffOut) {
        compWheelsStatus = inOffOut.toLowerCase();
        double power = 0;
        switch (compWheelsStatus) {
            case "in":
                power = -0.73;
                setChainBarStatus("up");
                break;
            case "out":
                power = 0.73;
                break;
        }
        setCompWheelsPower(power);
    }

    public static void setFoundationMoverStatus(String downUp) {
        foundationMoverStatus = downUp.toLowerCase();
        if (foundationMoverStatus.equals("down")) {
            hw.foundationMoverL.setPosition(0.9);
            hw.foundationMoverR.setPosition(0.9);
        } else {
            hw.foundationMoverL.setPosition(0.3);
            hw.foundationMoverR.setPosition(0.3);
        }
        hw.context.sleep(500);
    }

    public static void setChainBarPosition(double position) {
        hw.chainBarL.setPosition(position);
        hw.chainBarR.setPosition(position);
        hw.context.sleep(500);
    }

    public static void setChainBarStatus(String upInOut) {
        chainBarStatus = upInOut.toLowerCase();
        if (chainBarStatus.equals("up")) {
            setClawStatus("open");
            setChainBarPosition(0.75);
        } else if (chainBarStatus.equals("in")) {
            setChainBarPosition(0.9);
        } else if (chainBarStatus.equals("out")) {
            setChainBarPosition(0.45);
        }
        hw.context.sleep(500);
    }

    public static void cycleChainBar() {
        switch (chainBarStatus) {
            case "up":
                setChainBarStatus("in");
                break;
            case "in":
                setChainBarStatus("out");
                break;
            case "out":
                setChainBarStatus("up");
                break;
        }
    }

    public static void setClawStatus(String openClosed) {
        clawStatus = openClosed.toLowerCase();
        if (clawStatus.equals("open")) {
            hw.claw.setPower(-1.0);
        } else {
            hw.claw.setPower(1.0);
        }
        hw.context.sleep(500);
    }

    public static void setCapstoneStatus(String upDown) {
        capstoneStatus = upDown.toLowerCase();
        if (capstoneStatus.equals("up")) {
            hw.capstone.setPosition(0.34);
        } else {
            hw.capstone.setPosition(0.5);
        }
    }

}
