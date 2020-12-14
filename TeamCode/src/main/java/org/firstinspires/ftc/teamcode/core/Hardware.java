package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.net.Message;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
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

public class Hardware {

    public ExpansionHubEx expansionHubL;
    public ExpansionHubEx expansionHubR;
    public HardwareMap hwmp;
    public boolean isRunning;
    public Thread localizationUpdater;
    public Thread metricsUpdater;
    public ElapsedTime autoTime;

    public LinearOpMode ctx;
    public ID opModeID = new ID("Choose OpMode");

    public RCSocket rcSocket;
    public Metrics metrics;
    public String status = opModeID.toString();
    public File fieldJSON;

    public DcMotor fLDrive;
    public DcMotor fRDrive;
    public DcMotor bLDrive;
    public DcMotor bRDrive;

    public DcMotor xLOdo;
    public DcMotor xROdo;
    public DcMotor yOdo;

    public Hardware(LinearOpMode ctx) {
        this.ctx = ctx;
        this.hwmp = ctx.hardwareMap;

        initFiles();

        // Init hw
        expansionHubL = hwmp.get(ExpansionHubEx.class, "Expansion Hub L");
        expansionHubR = hwmp.get(ExpansionHubEx.class, "Expansion Hub R");

        fLDrive = hwmp.dcMotor.get("fLDrive");
        fRDrive = hwmp.dcMotor.get("fRDrive");
        bLDrive = hwmp.dcMotor.get("bLDrive");
        bRDrive = hwmp.dcMotor.get("bRDrive");

        xLOdo = fLDrive;
        xROdo = fRDrive;
        yOdo = bLDrive;

        // Init control, dashboard, telemetry, & threads
        Motion.init(this);
        Appendages.init(this);
        if (Constants.getBoolean("dashboard.isDebugging"))
            rcSocket = new RCSocket(this);
        metrics = new Metrics(this);
        initThreads();
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

    // Initialize OpMode
    public void initOpMode(String opModeID) {
        isRunning = true;
        this.opModeID = new ID(opModeID);
        status = "Running " + opModeID;

        Pose startPose = Motion.waypoints.get(new ID(opModeID + ".waypoint.start"));
        if (startPose == null) startPose = new Pose();
        Motion.start = new RigidBody(startPose);
        Motion.robot = new RigidBody(startPose);
    }

    // Init all files & resources
    public void initFiles() {
        try {
            Constants.init(new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/constants.json"));
            fieldJSON = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/field.json");
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
        int rings = FtcRobotControllerVisionActivity.instance.getRings();
        destroyCV();
        return rings;
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
                wpObj.put("tele.waypoint.start", new JSONArray(Motion.robot.toArray()));
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