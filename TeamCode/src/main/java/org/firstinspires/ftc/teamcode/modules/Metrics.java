package org.firstinspires.ftc.teamcode.modules;

import com.hyperion.common.*;

import org.firstinspires.ftc.teamcode.core.*;
import org.json.JSONArray;

import java.util.*;

/**
 * Custom telemetry for universal usability
 */

public class Metrics {

    private Hardware hw;

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
        data.add(new Entry("Skystone 0", hw.cvPipeline.getDetectedSkystonePosition()));
        data.add(new Entry("Skystone 1", (hw.cvPipeline.getDetectedSkystonePosition() + 3)));
        data.add(new Entry("Theoretical max FPS", hw.phoneCam.getCurrentPipelineMaxFps()));
        data.add(new Entry("Current FPS", String.format(Locale.US, "%.2f", hw.phoneCam.getFps())));
        data.add(new Entry("Total frame time ms", hw.phoneCam.getTotalFrameTimeMs()));
        data.add(new Entry("Pipeline time ms", hw.phoneCam.getPipelineTimeMs()));
        data.add(new Entry());

    }

    public synchronized void updateTelemetry() {
        try {
            JSONArray dataArr = new JSONArray();
            for (int i = 0; i < data.size(); i++) {
                Entry entry = data.get(i);
                hw.ctx.telemetry.addData(entry.token0, entry.token1);
                if (hw.rcClient != null)
                    dataArr.put(new JSONArray(entry.toArray()));
            }
            hw.ctx.telemetry.update();

            if (hw.rcClient != null) {
                hw.rcClient.emit("unimetryUpdated", dataArr.toString());
                TextUtils.printSocketLog("RC", "SERVER", "unimetryUpdated");
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

        public String[] toArray() {
            return new String[]{ token0, token1 };
        }

    }

}
