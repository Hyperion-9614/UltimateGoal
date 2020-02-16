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
                sleep(500);
                Appendages.setCompWheelsStatus("off");

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
        Appendages.setCompWheelsStatus("in");
        Motion.pidMove(37, 7 * Math.PI / 4, 5 * Math.PI / 4);
        Appendages.cycleChainBar();
        Appendages.setClawStatus("closed");
        Appendages.setCompWheelsStatus("off");
        Motion.pidMove(40, Math.PI, 0);
    }

    // Pivot drag foundation & push into building zone
    private void dragFoundation() {
        Motion.splineToWaypoint(Motion.getSpline("drag").waypoints.get(0).pose);
        if (hw.opModeID.contains("full"))
            placeStone(false);
        Appendages.setFoundationMoverStatus("down");
        Motion.followSpline("drag");
        Appendages.setFoundationMoverStatus("up");
    }

    // Go to a stone position
    private void goToStone(int position) {
        Motion.splineToWaypoint(Motion.getWaypoint("pickup0").addVector(new Vector2D(position * 20.32, 3 * Math.PI / 2, false)));
    }

    // Place a stone on foundation
    public void placeStone(boolean goToWaypoint) {
        if (goToWaypoint)
            Motion.pidMove("place");
        Appendages.cycleChainBar();
        Appendages.setClawStatus("open");
        Appendages.cycleChainBar();
    }

}
