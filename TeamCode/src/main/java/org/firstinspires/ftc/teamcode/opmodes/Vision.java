package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.Hardware;

@TeleOp
public class Vision extends LinearOpMode {

    private Hardware hw;

    @Override
    public void runOpMode() {
        hw = new Hardware(this);

        waitForStart();

        while (opModeIsActive()) {

        }
    }

}