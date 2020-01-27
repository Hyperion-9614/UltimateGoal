package org.firstinspires.ftc.teamcode.modules;

import com.hyperion.common.Utils;

import org.firstinspires.ftc.teamcode.core.Hardware;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Custom telemetry for universal usability
 */

public class Unimetry {

    private Hardware hardware;

    public ArrayList<Entry> data = new ArrayList<>();
    public String json = "";
    public String newLineSpaces = "";

    public Unimetry(Hardware hardware) {
        this.hardware = hardware;
    }

    public synchronized void update() {
        poll();
        updateTelemetry();
    }

    public synchronized void poll() {
        data.clear();
        newLineSpaces = "";

        data.add(new Entry("Status", hardware.status));
        data.add(new Entry());

        data.add(new Entry("Motion"));
        data.add(new Entry("Current", hardware.motion.robot.toString()));
        data.add(new Entry("Start", hardware.motion.start.toString()));
        data.add(new Entry("Max Acceleration", hardware.motion.localizer.maxAccel));
        data.add(new Entry("Max Deceleration", hardware.motion.localizer.maxDecel));
        data.add(new Entry("Max Velocity", hardware.motion.localizer.maxVel));
        data.add(new Entry("Wheel Velocities (fL/fR/bL/bR)", Utils.round(hardware.fLDrive.getPower(), 2) + " " + Utils.round(hardware.fRDrive.getPower(), 2) + " " + Utils.round(hardware.bLDrive.getPower(), 2) + " " + Utils.round(hardware.bRDrive.getPower(), 2)));
        data.add(new Entry("Odometry Counts (xL/xR/y)", hardware.motion.localizer.oldxlCounts + " " + hardware.motion.localizer.oldxrCounts + " " + hardware.motion.localizer.oldyCounts));
        data.add(new Entry());

        data.add(new Entry("Appendages"));
        data.add(new Entry("Vertical Slides (lPower/rPower/lTicks/rTicks)", Utils.round(hardware.vertSlideL.getPower(), 2) + " " + Utils.round(hardware.vertSlideR.getPower(), 2)
                            + " " + hardware.vertSlideL.getCurrentPosition() + " " + hardware.vertSlideR.getCurrentPosition()));
        data.add(new Entry("Compliant Wheels Status", hardware.appendages.compWheelsStatus));
        data.add(new Entry("Foundation Mover Status", hardware.appendages.foundationMoverStatus));
        data.add(new Entry("Chain Bar Status", hardware.appendages.chainBarStatus));
        data.add(new Entry("Claw Status", hardware.appendages.clawStatus));
        data.add(new Entry("Auto Claw Swing Status", hardware.appendages.autoClawSwingStatus));
        data.add(new Entry("Auto Claw Grip Status", hardware.appendages.autoClawGripStatus));
        data.add(new Entry());

        data.add(new Entry("Gamepad 1"));
        data.add(new Entry("Left Stick X", Utils.round(hardware.context.gamepad1.left_stick_x, 3)));
        data.add(new Entry("-Left Stick Y", Utils.round(-hardware.context.gamepad1.left_stick_y, 3)));
        data.add(new Entry("Right Stick X", Utils.round(hardware.context.gamepad1.right_stick_x, 3)));
        data.add(new Entry("Left Trigger", Utils.round(hardware.context.gamepad1.left_trigger, 3)));
        data.add(new Entry("Right Trigger", Utils.round(hardware.context.gamepad1.right_trigger, 3)));
        data.add(new Entry());

        data.add(new Entry("Vision"));
        data.add(new Entry("Skystone Position 1", hardware.cvPipeline.getDetectedSkystonePosition()));
        data.add(new Entry("Skystone Position 2", (hardware.cvPipeline.getDetectedSkystonePosition() + 3)));
        data.add(new Entry());

    }

    public synchronized void updateTelemetry() {
        try {
            JSONObject dataObj = new JSONObject();
            for (int i = 0; i < data.size(); i++) {
                Entry entry = data.get(i);
                JSONObject miniObj = new JSONObject();
                hardware.context.telemetry.addData(entry.token0, entry.token1);
                if (entry.token1.isEmpty()) {
                    miniObj.put("token0", "");
                    miniObj.put("token1", "");
                } else {
                    miniObj.put("token0", entry.token0);
                    miniObj.put("token1", entry.token1);
                }
                dataObj.put("" + i, miniObj);
            }

            json = dataObj.toString();
            hardware.context.telemetry.update();
            if (hardware.options.debug && hardware.rcClient != null) {
                hardware.rcClient.emit("unimetryUpdated", json);
                Utils.printSocketLog("RC", "SERVER", "unimetryUpdated", hardware.options);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Entry {

        public String token0;
        public String token1 = "";

        public Entry() {
            token0 = newLineSpaces;
            token1 = newLineSpaces;
            newLineSpaces += " ";
        }

        public Entry(String token0) {
            this.token0 = token0;
        }

        public Entry(String token0, Object token1) {
            this.token0 = token0;
            this.token1 = token1 == null ? "" : token1.toString();
        }

    }

}
