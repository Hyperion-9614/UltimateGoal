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

    public int firstPath;
    public int secondPath;

    @Override
    public void runOpMode() {
        hw = new Hardware(this);
        motion = hw.motion;
        appendages = hw.appendages;

        while (!isStopRequested() && (!isStarted() || (opModeIsActive() && !hw.isRunning))) {
            if (gamepad1.b) {
                hw.initOpMode("auto.red.full");
            } else if (gamepad1.x) {
                hw.initOpMode("auto.blue.full");
            } else if (gamepad1.y) {
                hw.initOpMode("auto.red.foundation");
            } else if (gamepad1.a) {
                hw.initOpMode("auto.blue.foundation");
            } else if (gamepad1.dpad_right) {
                hw.initOpMode("auto.red.brick");
            } else if (gamepad1.dpad_left) {
                hw.initOpMode("auto.blue.brick");
            }
        }

        execute();

        hw.end();
    }

    public void execute() {
        try {
            //TODO: Test this shit out, its jank af
            if (hw.cvPipeline.getPipelineActive()) {
                hw.cvPipeline.setPipelineActive(false);
                if (hw.opModeID.contains("blue")) {
                    firstPath = hw.cvPipeline.getDetectedSkystonePosition() + 3;
                    secondPath = hw.cvPipeline.getDetectedSkystonePosition();
                } else if (hw.opModeID.contains("red")) {
                    firstPath = hw.cvPipeline.getDetectedSkystonePosition();
                    secondPath = hw.cvPipeline.getDetectedSkystonePosition() + 3;
                }

            }
            hw.killCV();
            hw.autoTime = new ElapsedTime();

            if (hw.opModeID.endsWith("full")) {
                goToStone(firstPath);
                pickUpBlock();
                dragFoundation();
                hw.preset_placeStone();

                goToStone(secondPath);
                pickUpBlock();
                hw.preset_placeStone();

//                for (int i = 0; i < 6; i++) {
//                    if (i != skystonePositions[0] && i != skystonePositions[1]) {
//                        goToStone(i);
//                        pickUpBlock();
//                        hw.preset_placeStone();
//                    }
//                }
            } else if (hw.opModeID.endsWith("foundation")) {
                dragFoundation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Pick up a block
    private void pickUpBlock() {
        appendages.setAutoClawSwingStatus("down");
        appendages.setAutoClawGripStatus("closed");
        appendages.setAutoClawSwingStatus("up");
        motion.pidMove(motion.robot.pose.addVector(new Vector2D(20, hw.opModeID.contains("blue") ? 0 : Math.PI, false)));
    }

    // Pivot drag foundation & push into building zone
    private void dragFoundation() {
        motion.pidMove("drag0");
        appendages.setFoundationMoverStatus("down");
        motion.translate("drag1");
        motion.rotate("drag1");
        appendages.setFoundationMoverStatus("up");
        motion.pidMove(new Vector2D(10, Math.PI / 2, false));
    }

    // Go to a stone position
    private void goToStone(int position) {
        motion.pidMove(motion.getWaypoint("pickup0").addVector(new Vector2D(position * 20.32, 3 * Math.PI / 2, false)));
    }

}
