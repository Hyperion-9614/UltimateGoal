package org.firstinspires.ftc.teamcode.modules;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.net.Message;

import org.firstinspires.ftc.teamcode.core.Hardware;
import org.firstinspires.ftc.teamcode.core.Motion;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom telemetry for universal usability
 */

public class Metrics {

    private final Hardware hw;

    public List<Entry> data = new ArrayList<>();
    public String newLineSpaces = "";

    public Metrics(Hardware hardware) {
        this.hw = hardware;
    }

    public synchronized void update() {
        poll();
        updateTelemetry();
    }

    public synchronized void poll() {
        data.clear();
        newLineSpaces = "";

        data.add(new Entry("Status", hw.status));
        if (hw.opModeID.contains("auto")) {
            data.add(new Entry("Park", Motion.getWaypoint("park") == null ? "null" : Motion.getWaypoint("park")));
        }
        data.add(new Entry());

        data.add(new Entry("Motion"));
        data.add(new Entry("Current", Motion.robot));
        data.add(new Entry("Start", Motion.start));
        data.add(new Entry("Wheel Velocities (fL/fR/bL/bR)", MathUtils.round(hw.fLDrive.getPower(), 2) + " " + MathUtils.round(hw.fRDrive.getPower(), 2) + " " + MathUtils.round(hw.bLDrive.getPower(), 2) + " " + MathUtils.round(hw.bRDrive.getPower(), 2)));
        data.add(new Entry("Odometry Counts (xL/xR/y)", Motion.localizer.oldxlCounts + " " + Motion.localizer.oldxrCounts + " " + Motion.localizer.oldyCounts));
        data.add(new Entry());

        data.add(new Entry("Appendages"));
        data.add(new Entry());

        data.add(new Entry("Gamepad 1"));
        data.add(new Entry("Left Stick X", MathUtils.round(hw.ctx.gamepad1.left_stick_x, 3)));
        data.add(new Entry("-Left Stick Y", MathUtils.round(-hw.ctx.gamepad1.left_stick_y, 3)));
        data.add(new Entry("Right Stick X", MathUtils.round(hw.ctx.gamepad1.right_stick_x, 3)));
        data.add(new Entry("Left Trigger", MathUtils.round(hw.ctx.gamepad1.left_trigger, 3)));
        data.add(new Entry("Right Trigger", MathUtils.round(hw.ctx.gamepad1.right_trigger, 3)));
        data.add(new Entry());

        data.add(new Entry("Vision"));
        data.add(new Entry());

    }

    public synchronized void updateTelemetry() {
        try {
            JSONArray dataArr = new JSONArray();
            for (int i = 0; i < data.size(); i++) {
                Entry entry = data.get(i);
                hw.ctx.telemetry.addData(entry.token0, entry.token1);
                if (Constants.getBoolean("dashboard.isDebugging"))
                    dataArr.put(new JSONArray(entry.toArray()));
            }
            hw.ctx.telemetry.update();
            hw.rcSocket.sendMessage(Message.Event.METRICS_UPDATED, dataArr);
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

        public String[] toArray() {
            return new String[]{ token0, token1 };
        }

    }

}
