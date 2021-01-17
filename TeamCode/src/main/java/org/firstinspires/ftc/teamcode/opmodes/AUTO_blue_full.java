package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.core.Gerald;

/**
 *  Main AutoOp
 *  Select op mode then start
 */

@Autonomous
public class AUTO_blue_full extends LinearOpMode {

    private Gerald hw;

    @Override
    public void runOpMode() {
        hw = new Gerald(this);
        hw.initOpMode("auto.blue.full");
        execute();
        hw.end();
    }

    public void execute() {
        try {
            hw.autoTime = new ElapsedTime();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
