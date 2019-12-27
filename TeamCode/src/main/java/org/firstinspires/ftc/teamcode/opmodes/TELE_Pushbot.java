package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * Outreach pushbot teleop
 */

@TeleOp
public class TELE_Pushbot extends OpMode {

    private DcMotor leftDrive;
    private DcMotor rightDrive;

    @Override
    public void init() {
        leftDrive = hardwareMap.dcMotor.get("leftDrive");
        rightDrive = hardwareMap.dcMotor.get("rightDrive");

        leftDrive.setDirection(DcMotorSimple.Direction.REVERSE);
        leftDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightDrive.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    @Override
    public void loop() {
        setDrive(-gamepad1.left_stick_y, -gamepad1.right_stick_y);
    }

    @Override
    public void stop() {
        setDrive(0, 0);
    }

    public void setDrive(double lPow, double rPow) {
        leftDrive.setPower(lPow);
        rightDrive.setPower(rPow);
    }

}
