package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.core.Gerald;
import org.firstinspires.ftc.teamcode.core.Motion;

/**
 *  Main AutoOp
 *  Select op mode then start
 */

@Autonomous
public class AUTO_blue_full extends LinearOpMode {

    private Gerald gerald;

    @Override
    public void runOpMode() {
        gerald = new Gerald(this);
        gerald.initOpMode("auto.blue.full");
        execute();
        gerald.end();
    }

    public void execute() {
        try {
            gerald.autoTime = new ElapsedTime();
            Motion.followSpline(Motion.getSpline("test"), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
