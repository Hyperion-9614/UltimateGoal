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
public class AUTO_Main extends LinearOpMode {

    private Hardware hw;

    @Override
    public void runOpMode() {
        hw = new Hardware(this);
        int opModeSelectorIndex = -1;
        while ((!isStopRequested() && (!isStarted() || (opModeIsActive() && !hw.isRunning)))) {
            try {
                JSONArray autoOpModeIDs = Constants.getJSONArray("teamcode.autoOpModeIDs");
                if (hw.ctx.gamepad1.dpad_up) {
                    opModeSelectorIndex++;
                    if (opModeSelectorIndex >= autoOpModeIDs.length())
                        opModeSelectorIndex = 0;
                    hw.initOpMode(autoOpModeIDs.getString(opModeSelectorIndex));
                    hw.ctx.sleep(100);
                } else if (hw.ctx.gamepad1.dpad_down) {
                    opModeSelectorIndex--;
                    if (opModeSelectorIndex < 0)
                        opModeSelectorIndex = autoOpModeIDs.length() - 1;
                    hw.initOpMode(autoOpModeIDs.getString(opModeSelectorIndex));
                    hw.ctx.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
