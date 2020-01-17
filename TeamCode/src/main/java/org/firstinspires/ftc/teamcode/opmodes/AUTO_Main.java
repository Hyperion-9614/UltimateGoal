package org.firstinspires.ftc.teamcode.opmodes;

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
            hardware.autoTime = new ElapsedTime();
            if (hardware.opModeID.endsWith("full")) {
                motion.followPath("test");

//                scanSkystone();
//                dragFoundation();
//                hardware.preset_placeStone();

//                scanSkystone();
//                hardware.preset_placeStone();
            } else if (hardware.opModeID.endsWith("foundation")) {
                dragFoundation();
            }

            motion.goToWaypoint("park");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Locate and intake skystone
    private void scanSkystone() {
        motion.goToWaypoint("scan");
        ElapsedTime timer = new ElapsedTime();
        motion.strafe(new Vector2D(0.5, 3 * Math.PI / 2, false));
        while ((timer.milliseconds() < 3000) && opModeIsActive()) {
            if (hardware.Pipeline.getSkystoneDetected() == true) {
                int firstPath = (hardware.Pipeline.getSkystonePositions(0)[0]);
                int secondPath = (hardware.Pipeline.getSkystonePositions(0)[1]);
                telemetry.addLine("The Skystone is located in positions:" + firstPath + "and" + secondPath);
                telemetry.update();
                hardware.killCV();
                break;
            }
            //hardware.checkForcePark();
        }
        //motion.setPath(firstPath);
        //motion.setPath(secondPath);
        motion.setDrive(0);

        motion.rotate(hardware.opModeID.contains("blue") ? 3 * Math.PI / 2 : Math.PI / 2);
        motion.strafe(new Vector2D(0.3, hardware.opModeID.contains("blue") ? 3 * Math.PI / 2 : Math.PI / 2, false), 30);
        appendages.setAutoClawSwingStatus("down");
        appendages.setAutoClawGripStatus("closed");
        appendages.setAutoClawSwingStatus("up");
    }

    // Pivot drag foundation & push into building zone
    private void dragFoundation() {
        motion.goToWaypoint(Objects.requireNonNull(motion.splines.get(hardware.opModeID + ".splines.drag")).waypoints.get(0).pose);
        appendages.setFoundationMoverStatus("down");
        motion.followPath("drag");
        appendages.setFoundationMoverStatus("up");
    }

}
