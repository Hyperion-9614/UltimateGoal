package org.firstinspires.ftc.teamcode.opmodes;

import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.Vector2D;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.core.Appendages;
import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;

import java.util.Objects;

/**
 *  Main AutoOp
 *  Select op mode then start
 */

@Autonomous
public class AUTO_Main extends LinearOpMode {

    private Hardware hardware;
    private Motion motion;
    private Appendages appendages;

    public int[] skystonePositions;

    @Override
    public void runOpMode() {
        hardware = new Hardware(this);
        motion = hardware.motion;
        appendages = hardware.appendages;

        while (!isStopRequested() && (!isStarted() || (opModeIsActive() && !hardware.isRunning))) {
            if (gamepad1.b) {
                hardware.initOpMode("auto.red.full");
            } else if (gamepad1.x) {
                hardware.initOpMode("auto.blue.full");
            } else if (gamepad1.y) {
                hardware.initOpMode("auto.red.foundation");
            } else if (gamepad1.a) {
                hardware.initOpMode("auto.blue.foundation");
            } else if (gamepad1.dpad_right) {
                hardware.initOpMode("auto.red.brick");
            } else if (gamepad1.dpad_left) {
                hardware.initOpMode("auto.blue.brick");
            }
        }

        execute();

        hardware.end();
    }

    public void execute() {
        try {
            if (hardware.cvPipeline.getPipelineActive()) {
                hardware.cvPipeline.setPipelineActive(false);
                if (hardware.opModeID.contains("blue")) {
                    skystonePositions = hardware.cvPipeline.getSkystonePositions(5);
                } else if (hardware.opModeID.contains("red")) {
                    skystonePositions = hardware.cvPipeline.getSkystonePositions(0);
                }

            }
            skystonePositions = new int[]{0, 3};
            hardware.autoTime = new ElapsedTime();

            if (hardware.opModeID.endsWith("full")) {
                goToStone(skystonePositions[0]);
                pickUpBlock();
                dragFoundation();
                hardware.preset_placeStone();

                goToStone(skystonePositions[1]);
                pickUpBlock();
                hardware.preset_placeStone();

//                for (int i = 0; i < 6; i++) {
//                    if (i != skystonePositions[0] && i != skystonePositions[1]) {
//                        goToStone(i);
//                        pickUpBlock();
//                        hardware.preset_placeStone();
//                    }
//                }
            } else if (hardware.opModeID.endsWith("foundation")) {
                dragFoundation();
            }

            Pose park = motion.waypoints.get(hardware.opModeID + ".waypoint.park");
            motion.rotate(Objects.requireNonNull(park).theta);
            motion.strafe(park);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Pick up a block
    private void pickUpBlock() {
        appendages.setAutoClawSwingStatus("down");
        appendages.setAutoClawGripStatus("closed");
        appendages.setAutoClawSwingStatus("up");
        motion.strafe(new Vector2D(0.7, hardware.opModeID.contains("blue") ? Math.PI : 0, false), 20);
    }

    // Pivot drag foundation & push into building zone
    private void dragFoundation() {
        Pose target = Objects.requireNonNull(motion.splines.get(hardware.opModeID + ".spline.drag")).waypoints.get(0).pose;
        motion.rotate(target.theta);
        motion.strafe(target);
        appendages.setFoundationMoverStatus("down");
        motion.pivot(3 * Math.PI / 2, hardware.opModeID.contains("blue") ? 0 : 1, hardware.opModeID.contains("blue") ? 1 : -1);
        appendages.setFoundationMoverStatus("up");
        motion.strafe(new Vector2D(0.5, Math.PI / 2, false), 10);
    }

    // Go to a stone position
    private void goToStone(int position) {
        Pose goal = motion.waypoints.get(hardware.opModeID + ".waypoint.pickup0");
        if (goal != null) {
            goal = goal.addVector(new Vector2D(position * 20.32, 3 * Math.PI / 2, false));
            motion.rotate(goal.theta);
            motion.strafe(goal);
            motion.rotate(goal.theta);
            motion.strafe(goal);
        }
    }

}
