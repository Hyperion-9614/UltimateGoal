package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.core.Appendages;
import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;

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
            if (hardware.opModeID.endsWith("full")) {
                scanSkystone();
                dragFoundation();
                motion.goToWaypoint("park");
            } else if (hardware.opModeID.endsWith("foundation")) {
                dragFoundation();
                motion.goToWaypoint("park");
            } else if (hardware.opModeID.endsWith("brick")) {
                motion.goToWaypoint("park");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Locate and intake skystone
    public void scanSkystone() {
        motion.goToWaypoint("scan");

    }

    // Pivot drag foundation & push into building zone
    public void dragFoundation() {
        motion.followPath("drag");
    }

}
