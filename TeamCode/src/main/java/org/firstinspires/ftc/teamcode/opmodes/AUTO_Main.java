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

    public int skyStone0;
    public int skyStone1;

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
            scan();

            if (hw.opModeID.get(-1).equals("full")) {
                goToStone(skyStone0);
                pickUpBlock();
                dragFoundation();

//                goToStone(skyStone1);
//                pickUpBlock();
//                placeStone();
            } else if (hw.opModeID.get(-1).equals("foundation")) {
                dragFoundation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Determine skystone positions
    private void scan() {
        if (hw.cvPipeline.getPipelineActive()) {
            hw.cvPipeline.setPipelineActive(false);
            skyStone0 = hw.cvPipeline.getDetectedSkystonePosition();
            if (hw.opModeID.contains("red"))
                skyStone0 = 2 - skyStone0;
            skyStone1 = skyStone0 + 3;
        }
        hw.killCV();
    }

    // Pick up a block
    private void pickUpBlock() {

    }

    // Pivot drag foundation & push into building zone
    private void dragFoundation() {

    }

    // Go to a stone position
    private void goToStone(int position) {

    }

    // Place a stone on foundation
    public void placeStone() {

    }

}
