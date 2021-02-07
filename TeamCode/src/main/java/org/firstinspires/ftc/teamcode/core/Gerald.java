package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.net.Message;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcontroller.vision.FtcRobotControllerVisionActivity;
import org.firstinspires.ftc.teamcode.modules.Metrics;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openftc.revextensions2.ExpansionHubEx;

import java.io.File;

/**
 * Handles all hw interfacing and robot/external initialization
 */

public class Gerald {

    public ExpansionHubEx controlHub;
    public ExpansionHubEx expansionHub;
    public HardwareMap hwmp;
    public LinearOpMode ctx;

    public boolean isRunning;
    public Thread localizationUpdater;
    public Thread metricsUpdater;
    public ElapsedTime autoTime;

    public String dataDirPref = "/data/data/com.qualcomm.ftcrobotcontroller/files/";
    public ID opModeID;
    public RCSocket rcSocket;
    public Metrics metrics;
    public String status;
    public File fieldJSON;

    public Gerald(LinearOpMode ctx, String opModeID) {
        this.ctx = ctx;
        this.hwmp = ctx.hardwareMap;
        this.opModeID = new ID(opModeID);

        initFiles();

        // Init expansion hubs
        controlHub = hwmp.get(ExpansionHubEx.class, "Control Hub");
//        expansionHub = hwmp.get(ExpansionHubEx.class, "Expansion Hub");

        // Init motion & appendages
        Motion.init(this);
        Apndg.init(this);

        // Init dashboard, telemetry, & threads
        rcSocket = new RCSocket(this);
        metrics = new Metrics(this);
        initThreads();

        isRunning = true;
        status = "OpMode " + opModeID + " inited and ready to run";
    }

    ///////////////////////// INIT //////////////////////////

    // Initialize localizationUpdater and unimetryUpdater threads
    private void initThreads() {
        localizationUpdater = new Thread(() -> {
            long lastUpdateTime = System.currentTimeMillis();
            while (!localizationUpdater.isInterrupted() && localizationUpdater.isAlive() && !ctx.isStopRequested()) {
                if (System.currentTimeMillis() - lastUpdateTime >= Constants.getInt("teamcode.localizationDelay")) {
                    lastUpdateTime = System.currentTimeMillis();
                    Motion.localizer.update();
                }
            }
        });
        localizationUpdater.start();

        metricsUpdater = new Thread(() -> {
            long lastUpdateTime = System.currentTimeMillis();
            while (!metricsUpdater.isInterrupted() && metricsUpdater.isAlive() && !ctx.isStopRequested()) {
                if (System.currentTimeMillis() - lastUpdateTime >= Constants.getInt("teamcode.metricsDelay")) {
                    lastUpdateTime = System.currentTimeMillis();
                    metrics.update();
                }
            }
        });
        metricsUpdater.start();
    }

    // Init all files & resources
    public void initFiles() {
        try {
            Constants.init(new File(dataDirPref + "hyperilibs/constants.json"));
            fieldJSON = new File(dataDirPref + "hyperilibs/field.json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //////////////////////// COMPUTER VISION ////////////////////////

    // Initialize camera by enabling view and not killing OpenCV
    public void initializeCV() {
        FtcRobotControllerVisionActivity.instance.enableView();
        FtcRobotControllerVisionActivity.reviveOpenCV();
    }

    // Destroy camera instance to save CPU + GPU power
    public void destroyCV() {
        FtcRobotControllerVisionActivity.instance.disableView();
        FtcRobotControllerVisionActivity.killOpenCV();
    }

    // Get the number of rings in the stack
    public int getStackHeight() {
        initializeCV();
        FtcRobotControllerVisionActivity.setPipeline("ringStack");
        int rings = FtcRobotControllerVisionActivity.getRings();
        destroyCV();
        return rings;
    }

    public double distanceToRing() {
        initializeCV();
        FtcRobotControllerVisionActivity.setPipeline("ringLocalization");
        return FtcRobotControllerVisionActivity.getDistance();
    }

//////////////////////// END ////////////////////////

    // Wrap up OpMode
    public void end() {
        status = "Ending";
        isRunning = false;

        if (localizationUpdater != null && localizationUpdater.isAlive() && !localizationUpdater.isInterrupted())
            localizationUpdater.interrupt();
        if (metricsUpdater != null && metricsUpdater.isAlive() && !metricsUpdater.isInterrupted())
            metricsUpdater.interrupt();

        try {
            if (opModeID.get(0).equals("auto")) {
                JSONObject obj = new JSONObject(IOUtils.readFile(fieldJSON));
                JSONObject wpObj = obj.getJSONObject("waypoints");
                wpObj.put(new ID("tele", opModeID.sub(1, 2), "start").toString(), new JSONArray(Motion.robot.toArray()));
                obj.put("waypoints", wpObj);
                IOUtils.writeFile(obj.toString(), fieldJSON);
            }

            rcSocket.sendMessage(Message.Event.OPMODE_ENDED, new JSONObject());
            rcSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!ctx.isStopRequested() || ctx.opModeIsActive()) {
            ctx.requestOpModeStop();
        }
    }

}