package org.firstinspires.ftc.teamcode.opmodes;

import com.hyperion.common.Constants;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;
import org.json.JSONArray;

/**
 *  Main AutoOp
 *  Select op mode then start
 */

@Autonomous
public class AUTO_blue_full extends LinearOpMode {

    private Hardware hw;

    @Override
    public void runOpMode() {
        hw = new Hardware(this);
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
