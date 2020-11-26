package org.firstinspires.ftc.teamcode.opmodes;

import com.hyperion.motion.math.Vector2D;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.core.Appendages;
import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;

/**
 *  Main AutoOp
 *  Select op mode then start
 */

@Autonomous
public class AUTO_Main extends LinearOpMode {

    private Hardware hw;

    @Override
    public void runOpMode() {
        hw = new Hardware(this);
        while ((!isStopRequested() && (!isStarted() || (opModeIsActive() && !hw.isRunning))) || Motion.getWaypoint("park") == null) {
            hw.initLoop(true);
        }
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
