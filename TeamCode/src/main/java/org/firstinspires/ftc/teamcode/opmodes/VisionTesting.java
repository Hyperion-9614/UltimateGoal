package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.core.Gerald;

/*
OpMode to test the vision pipeline
 */
@Autonomous
public class VisionTesting extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        Gerald hw = new Gerald(this);
        while (opModeIsActive() && (!isStopRequested())) {
            telemetry.addData("Number of rings detected", hw.getStackHeight()); //TODO: Needs to be tested
        }
    }
}
