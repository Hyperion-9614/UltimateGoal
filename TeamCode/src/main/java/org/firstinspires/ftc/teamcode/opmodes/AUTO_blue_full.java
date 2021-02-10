package org.firstinspires.ftc.teamcode.opmodes;

import com.hyperion.motion.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.core.Apndg;
import org.firstinspires.ftc.teamcode.core.Apndg.State;
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
        gerald = new Gerald(this, "auto.blue.full");
        waitForStart();
        execute();
        gerald.end();
    }

    public void execute() {
        try {
            gerald.autoTime = new ElapsedTime();
            gerald.status = "Running OpMode " + gerald.opModeID.toString();

            Apndg.setShooter(State.ON);
            sleep(5000);
            Apndg.setShooter(State.OFF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
