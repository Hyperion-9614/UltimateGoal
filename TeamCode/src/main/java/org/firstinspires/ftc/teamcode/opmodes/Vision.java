package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.Hardware;

@TeleOp
public class Vision extends LinearOpMode {
    private Hardware hardware;

    //TODO: Run this opmode and scroll down to see Unimetry Readings, should work
    @Override
    public void runOpMode() {
        hardware = new Hardware(this);
        hardware.initCV();
        waitForStart();
        while (opModeIsActive()) {
            sleep(100);
            }
        }
    }