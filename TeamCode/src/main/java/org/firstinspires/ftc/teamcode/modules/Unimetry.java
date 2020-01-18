package org.firstinspires.ftc.teamcode.modules;

import com.hyperion.common.Utils;

import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.opmodes.Vision;
import org.json.JSONArray;

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
        data.add(new Entry("Vision", Vision.skystonePositions[0]));
        data.add(new Entry("Vision", Vision.skystonePositions[1]));

    }

    public synchronized void updateTelemetry() {
        try {
            JSONArray dataArr = new JSONArray();
            for (Entry entry : data) {
                JSONArray miniArr = new JSONArray();
                if (entry.token2 == null) {
                    hardware.context.telemetry.addData(entry.token1.toString(), "");
                    miniArr.put("");
                    miniArr.put("");
                } else {
                    hardware.context.telemetry.addData(entry.token1.toString(), entry.token2.toString());
                    miniArr.put(entry.token1.toString());
                    miniArr.put(entry.token2.toString());
                }
                dataArr.put(miniArr);
            }

            json = dataArr.toString();
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

        public Object token1;
        public Object token2;

        public Entry() {
            token1 = newLineSpaces;
            token2 = newLineSpaces;
            newLineSpaces += " ";
        }

        public Entry(Object token1) {
            this.token1 = token1;
        }

        public Entry(Object token1, Object token2) {
            this.token1 = token1;
            this.token2 = token2;
        }

    }

}
