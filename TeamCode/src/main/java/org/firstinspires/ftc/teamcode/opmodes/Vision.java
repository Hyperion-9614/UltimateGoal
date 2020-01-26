package org.firstinspires.ftc.teamcode.opmodes;

import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.Hardware;

@TeleOp
public class Vision extends LinearOpMode {
    private Hardware hardware;

    @Override
    public void runOpMode() {
        hardware = new Hardware(this);
        hardware.initCV();
        waitForStart();

//        Log.i("Skystone Locations: ", hardware.skystonePositions.toString());
        while (opModeIsActive()) {
            sleep(100);
            Log.i("Some Random Shit", "gay");
            }
        }
    }