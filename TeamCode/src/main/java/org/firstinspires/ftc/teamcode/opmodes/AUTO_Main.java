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
    private Motion motion;
    private Appendages appendages;

    public int skyStone0;
    public int skyStone1;

    private String[] opModeIDs = new String[]{ "auto.blue.full", "auto.red.full",
                                               "auto.blue.foundation", "auto.red.foundation",
                                               "auto.blue.brick", "auto.red.brick" };
    private int opModeSelectorIndex = -1;

    @Override
    public void runOpMode() {
        hw = new Hardware(this);
        motion = hw.motion;
        appendages = hw.appendages;

        while (!isStopRequested() && (!isStarted() || (opModeIsActive() && !hw.isRunning))) {
            if (gamepad1.dpad_up) {
                opModeSelectorIndex++;
                if (opModeSelectorIndex >= opModeIDs.length)
                    opModeSelectorIndex = 0;
                hw.initOpMode(opModeIDs[opModeSelectorIndex]);
                sleep(250);
            } else if (gamepad1.dpad_down) {
                opModeSelectorIndex--;
                if (opModeSelectorIndex < 0)
                    opModeSelectorIndex = opModeIDs.length - 1;
                hw.initOpMode(opModeIDs[opModeSelectorIndex]);
                sleep(250);
            }
        }

        execute();

        hw.end();
    }

    public void execute() {
        try {
            if (hw.cvPipeline.getPipelineActive()) {
                hw.cvPipeline.setPipelineActive(false);
                skyStone0 = hw.cvPipeline.getDetectedSkystonePosition();
                if (hw.opModeID.contains("red"))
                    skyStone0 = 2 - skyStone0;
                skyStone1 = skyStone0 + 3;
            }
            hw.killCV();
            hw.autoTime = new ElapsedTime();

            if (hw.opModeID.endsWith("full")) {
                hw.compWheelsR.setPower(0.6);
                sleep(750);
                appendages.setCompWheelsStatus("off");

                goToStone(skyStone0);
                pickUpBlock();
                dragFoundation();

//                goToStone(skyStone1);
//                pickUpBlock();
//                placeStone(true);
            } else if (hw.opModeID.endsWith("foundation")) {
                dragFoundation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Pick up a block
    private void pickUpBlock() {
        appendages.setCompWheelsStatus("in");
        motion.pidMove(45, 7 * Math.PI / 4, 5 * Math.PI / 4);
        sleep(500);
        appendages.cycleChainBar();
        appendages.setClawStatus("closed");
        appendages.setCompWheelsStatus("off");
        motion.pidMove(50, Math.PI, 0);
    }

    // Pivot drag foundation & push into building zone
    private void dragFoundation() {
        motion.pidMove("drag0");
        if (hw.opModeID.contains("full"))
            placeStone(false);
        appendages.setFoundationMoverStatus("down");
        motion.pidMove("drag1");
        motion.pidMove("drag2");
        appendages.setFoundationMoverStatus("up");
    }

    // Go to a stone position
    private void goToStone(int position) {
        motion.pidMove(motion.getWaypoint("pickup0").addVector(new Vector2D(position * 20.32, 3 * Math.PI / 2, false)));
    }

    // Place a stone on foundation
    public void placeStone(boolean goToWaypoint) {
        if (goToWaypoint)
            motion.pidMove("place");
        appendages.cycleChainBar();
        appendages.setClawStatus("open");
        appendages.cycleChainBar();
    }

}
