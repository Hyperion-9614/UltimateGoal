package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.Hardware;
@TeleOp
public class Vision extends LinearOpMode {
    private Hardware hardware;
    public static int[] skystonePositions;

    @Override
    public void runOpMode() {
        hardware = new Hardware(this);
        hardware.initCV();
        waitForStart();
        while (opModeIsActive()){
            if (hardware.cvPipeline.getPipelineActive()) {
                skystonePositions = hardware.cvPipeline.getSkystonePositions(0);
//                if (hardware.opModeID.contains("blue")) {
//                    skystonePositions = hardware.cvPipeline.getSkystonePositions(5);
//                } else if (hardware.opModeID.contains("red")) {
//                    skystonePositions = hardware.cvPipeline.getSkystonePositions(0);
//                }
                System.out.println("The Skystones are located at:" + skystonePositions[0] + "and " + skystonePositions[1]);
                hardware.killCV();
                break;
            }
        }
    }
}
