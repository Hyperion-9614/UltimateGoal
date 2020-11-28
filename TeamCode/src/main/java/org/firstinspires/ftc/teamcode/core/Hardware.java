package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.modules.Metrics;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;
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

    public OpenCvInternalCamera phoneCam;

    public BTClient btClient;
    public Metrics metrics;
    public String status = opModeID.toString();

    public File fieldJSON;
    public File nnConfigJson;
    public File modelConfig;

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

        // Init control, dashboard, telemetry, CV, & threads
        Motion.init(this);
        Appendages.init(this);
        if (Constants.getBoolean("dashboard.isDebugging"))
            btClient = new BTClient(this);
        metrics = new Metrics(this);
        initCV();
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

    // Initialize CV pipeline
    private void initCV() {
        int cameraMonitorViewId = hwmp.appContext.getResources().getIdentifier("java_camera_view", "id", hwmp.appContext.getPackageName());
        phoneCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
        phoneCam.openCameraDevice();
        phoneCam.startStreaming(1280, 720, OpenCvCameraRotation.SIDEWAYS_LEFT);
        phoneCam.setFlashlightEnabled(true);
        for (OpenCvInternalCamera.FrameTimingRange r : phoneCam.getFrameTimingRangesSupportedByHardware()) {
            if (r.max == 30 && r.min == 30) {
                phoneCam.setHardwareFrameTimingRange(r);
                break;
            }
        }
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
            nnConfigJson = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/model/config.json");
            modelConfig = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/model/model.config");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ///////////////////////// END //////////////////////////

    private void killCV() {
        if (phoneCam != null) {
            phoneCam.setFlashlightEnabled(false);
            phoneCam.pauseViewport();
            phoneCam.stopStreaming();
            phoneCam.setPipeline(null);
            phoneCam.closeCameraDevice();
            System.gc();
        }
    }

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

            btClient.sendMessage("OPMODE_ENDED", new JSONObject());
            btClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!ctx.isStopRequested() || ctx.opModeIsActive()) {
            ctx.requestOpModeStop();
        }
    }

}