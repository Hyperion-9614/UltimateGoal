package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.teamcode.core.Appendages;
import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;

/**
 *  Main AutoOp
 *  Select op mode then start
 */

@Autonomous
public class AUTO_Main extends OpMode {

    private Hardware hardware;
    private Motion motion;
    private Appendages appendages;

    @Override
    public void init() {
        hardware = new Hardware(this);
        motion = hardware.motion;
        appendages = hardware.appendages;
    }

    @Override
    public void init_loop() {
        if (hardware != null) {
            if (gamepad1.b) {
                hardware.opModeID = "auto.red.full";
            } else if (gamepad1.x) {
                hardware.opModeID = "auto.blue.full";
            } else if (gamepad1.y) {
                hardware.opModeID = "auto.red.foundation";
            } else if (gamepad1.a) {
                hardware.opModeID = "auto.blue.foundation";
            } else if (gamepad1.dpad_right) {
                hardware.opModeID = "auto.red.brick";
            } else if (gamepad1.dpad_left) {
                hardware.opModeID = "auto.blue.brick";
            }

            if (motion.localizer != null && hardware.unimetry != null) {
                if (!hardware.opModeID.isEmpty()) hardware.status = "Running " + hardware.opModeID;
                motion.localizer.update();
                hardware.unimetry.update();
            }
        }
    }

    @Override
    public void start() {
        hardware.init();

        if (hardware.isRunning) {
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
    }

    @Override
    public void loop() {
        motion.localizer.update();
        hardware.unimetry.update();
        if (!hardware.isRunning) {
            if (!hardware.opModeID.equals("Choose OpMode")) {
                hardware.init();
                start();
            }
            if (gamepad1.b) {
                hardware.opModeID = "auto.red.full";
            } else if (gamepad1.x) {
                hardware.opModeID = "auto.blue.full";
            } else if (gamepad1.y) {
                hardware.opModeID = "auto.red.foundation";
            } else if (gamepad1.a) {
                hardware.opModeID = "auto.blue.foundation";
            } else if (gamepad1.dpad_right) {
                hardware.opModeID = "auto.red.brick";
            } else if (gamepad1.dpad_left) {
                hardware.opModeID = "auto.blue.brick";
            }
        }
    }

    @Override
    public void stop() {
        hardware.end();
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
