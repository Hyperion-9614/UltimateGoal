package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.core.Hardware;
@TeleOp
public class Vision extends LinearOpMode {
    private Hardware hardware;

    @Override
    public void runOpMode() {
        hardware = new Hardware(this);
        waitForStart();
        while (opModeIsActive()){

        }
    }
}
