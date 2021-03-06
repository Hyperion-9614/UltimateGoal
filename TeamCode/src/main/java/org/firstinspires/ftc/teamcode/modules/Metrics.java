package org.firstinspires.ftc.teamcode.modules;

import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.net.Message;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.core.Apndg;
import org.firstinspires.ftc.teamcode.core.Gerald;
import org.firstinspires.ftc.teamcode.core.Motion;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom telemetry for universal usability
 */

public class Metrics {

    private final Gerald gerald;

    public List<Entry> data = new ArrayList<>();
    public String newLineSpaces = "";

    public Metrics(Gerald gerald) {
        this.gerald = gerald;
    }

    public synchronized void update() {
        poll();
        updateTelemetry();
    }

    public synchronized void poll() {
        data.clear();
        newLineSpaces = "";

        data.add(new Entry("Status", gerald.status));
        data.add(new Entry());

        data.add(new Entry("Motion"));
        data.add(new Entry("Current", Motion.robot));
        data.add(new Entry("Start", Motion.start));
        data.add(new Entry("Wheel Velocities (fL/fR/bL/bR)",
                "fL: " + MathUtils.round(Motion.fLDrive.getPower(), 2)
             + " fR: " + MathUtils.round(Motion.fRDrive.getPower(), 2)
             + " bL: " + MathUtils.round(Motion.bLDrive.getPower(), 2)
             + " bR: " + MathUtils.round(Motion.bRDrive.getPower(), 2)));
        data.add(new Entry("Odometry Counts",
                "xL: " + Motion.localizer.oldxlCounts
             + " xR: " + Motion.localizer.oldxrCounts
              + " y: " + Motion.localizer.oldyCounts));
        data.add(new Entry("Odometry Cycle Time",
               "Min: " + MathUtils.round(Motion.localizer.minDt, 6)
            + " Ave: " + MathUtils.round(Motion.localizer.aveDt, 6)
            + " Max: " +  MathUtils.round(Motion.localizer.maxDt, 6)));
        data.add(new Entry());

        data.add(new Entry("Appendages"));
        data.add(new Entry("Shooter", Apndg.States.shooter
                + " - L: " + MathUtils.round(MathUtils.radToRPM(Apndg.shooterL.getVelocity(AngleUnit.RADIANS)), 2)
                + " RPM R: " + MathUtils.round(MathUtils.radToRPM(Apndg.shooterR.getVelocity(AngleUnit.RADIANS)), 2) + " RPM"));
        data.add(new Entry("Flap", MathUtils.round(Apndg.States.flap, 2) + "\u00B0 - " + MathUtils.round(Apndg.flap.getPosition(), 2)));
        data.add(new Entry("Angler", MathUtils.round(Apndg.States.anglerDeg, 2) + "\u00B0 - " + MathUtils.round(Apndg.angler.getPosition(), 2)));
        data.add(new Entry("Loader", Apndg.States.loader + " - " + MathUtils.round(Apndg.loader.getPosition(), 2)));
        data.add(new Entry("Intake", Apndg.States.intake + " - L: " + MathUtils.round(Apndg.shooterL.getPower(), 2)
                                                           + " R: " + MathUtils.round(Apndg.shooterR.getPower(), 2)));
        data.add(new Entry("Mustache", Apndg.States.mustache + " - L: " + MathUtils.round(Apndg.mustacheL.getPosition(), 2)
                                                               + " R: " + MathUtils.round(Apndg.mustacheR.getPosition(), 2)));
        data.add(new Entry("Arm", Apndg.States.arm + " - " + Apndg.arm.getCurrentPosition()));
        data.add(new Entry("Claw", Apndg.States.claw + " - " + MathUtils.round(Apndg.claw.getPosition(), 2)));
        data.add(new Entry());

        data.add(new Entry("Gamepad 1"));
        data.add(new Entry("Left Stick X", MathUtils.round(gerald.ctx.gamepad1.left_stick_x, 3)));
        data.add(new Entry("-Left Stick Y", MathUtils.round(-gerald.ctx.gamepad1.left_stick_y, 3)));
        data.add(new Entry("Right Stick X", MathUtils.round(gerald.ctx.gamepad1.right_stick_x, 3)));
        data.add(new Entry("Left Trigger", MathUtils.round(gerald.ctx.gamepad1.left_trigger, 3)));
        data.add(new Entry("Right Trigger", MathUtils.round(gerald.ctx.gamepad1.right_trigger, 3)));
        data.add(new Entry());

        data.add(new Entry("Vision"));
        data.add(new Entry());

    }

    public synchronized void appendSilentData() throws Exception {
        JSONObject velMotorsObj = new JSONObject();
        for (DcMotorEx motor : new DcMotorEx[]{ Apndg.shooterL }) {
            JSONObject motorData = new JSONObject();
            motorData.put("currRPM", MathUtils.radToRPM(motor.getVelocity(AngleUnit.RADIANS)));
            double targetRPM = 0;
            if (motor.getDeviceName().equals("shooterL"))
                targetRPM = Apndg.States.shooterLrpm;
            motorData.put("targetRPM", targetRPM);
            velMotorsObj.put(motor.getDeviceName(), motorData);
        }
        data.add(new Entry("velMotors", velMotorsObj));
    }

    public synchronized void updateTelemetry() {
        try {
            JSONObject telemetryDataObj = new JSONObject();
            int i;
            for (i = 0; i < data.size(); i++) {
                Entry entry = data.get(i);
                gerald.ctx.telemetry.addData(entry.token0, entry.token1);
                if (Constants.getBoolean("dashboard.isDebugging"))
                    telemetryDataObj.put(entry.token0, entry.token1);
            }
            gerald.ctx.telemetry.update();

            appendSilentData();
            JSONObject silentDataObj = new JSONObject();
            for (int j = i; j < data.size(); j++) {
                Entry entry = data.get(j);
                if (Constants.getBoolean("dashboard.isDebugging"))
                    silentDataObj.put(entry.token0, entry.token1);
            }

            JSONObject metricsObj = new JSONObject();
            metricsObj.put("telemetry", telemetryDataObj);
            metricsObj.put("silent", silentDataObj);
            gerald.rcSocket.sendMessage(Message.Event.METRICS_UPDATED, metricsObj);
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
