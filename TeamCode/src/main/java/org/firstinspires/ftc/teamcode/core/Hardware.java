package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.Options;
import com.hyperion.common.Utils;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.modules.CvPipeline;
import org.firstinspires.ftc.teamcode.modules.Unimetry;
import org.json.JSONObject;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;
import org.openftc.revextensions2.ExpansionHubEx;

import java.io.File;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Handles all hw interfacing and robot/external initialization
 * Also contains presets
 */

public class Hardware {

    public ExpansionHubEx expansionHubL;
    public ExpansionHubEx expansionHubR;
    public HardwareMap hwmp;
    public boolean isRunning;
    public Thread updater;

    public Motion motion;
    public Appendages appendages;

    public LinearOpMode context;
    public String opModeID = "Choose OpMode";

    public OpenCvInternalCamera phoneCam;
    public CvPipeline cvPipeline;

    public Socket rcClient;
    public Constants constants;
    public Options options;
    public Unimetry unimetry;
    public String status = opModeID;

    public File dashboardJson;
    public File optionsJson;
    public File nnConfigJson;
    public File modelConfig;

    public DcMotor fLDrive;
    public DcMotor fRDrive;
    public DcMotor bLDrive;
    public DcMotor bRDrive;

    public DcMotor xLOdo;
    public DcMotor xROdo;
    public DcMotor yOdo;

    public DcMotor vertSlideL;
    public DcMotor vertSlideR;

    public DcMotor compWheelsL;
    public DcMotor compWheelsR;

    public Servo foundationMoverL;
    public Servo foundationMoverR;
    public Servo chainBarL;
    public Servo chainBarR;
    public CRServo claw;
    public Servo autoClawSwing;
    public CRServo autoClawGrip;

    public int presetPlaceStoneTicks = 2500;

    public Hardware(LinearOpMode context) {
        this.context = context;
        this.hwmp = context.hardwareMap;

        // Init files & resources
        try {
            constants = new Constants(new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/constants.json"));
            dashboardJson = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/dashboard.json");
            optionsJson = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/options.json");
            options = new Options(optionsJson);
            nnConfigJson = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/model/config.json");
            modelConfig = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/model/model.config");
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        vertSlideL = hwmp.dcMotor.get("vertSlideL");
        vertSlideR = hwmp.dcMotor.get("vertSlideR");

        compWheelsL = hwmp.dcMotor.get("compWheelsL");
        compWheelsR = hwmp.dcMotor.get("compWheelsR");

        foundationMoverL = hwmp.servo.get("foundationMoverL");
        foundationMoverR = hwmp.servo.get("foundationMoverR");
        chainBarL = hwmp.servo.get("chainBarL");
        chainBarR = hwmp.servo.get("chainBarR");
        claw = hwmp.crservo.get("claw");
        autoClawSwing = hwmp.servo.get("autoClawSwing");
        autoClawGrip = hwmp.crservo.get("autoClawGrip");

        // Init control, telemetry, & settings
        motion = new Motion(this);
        appendages = new Appendages(this);
        unimetry = new Unimetry(this);

        initUpdater();
        initCV();

        // Init options & dashboard
        if (options.debug) initDashboard();
    }

    ///////////////////////// PRE & POST //////////////////////////

    // Initialize updater thread
    public void initUpdater() {
        updater = new Thread(() -> {
            long lastUpdateTime = System.currentTimeMillis();
            while (!updater.isInterrupted() && updater.isAlive() && !context.isStopRequested()) {
                if (System.currentTimeMillis() - lastUpdateTime >= constants.UPDATER_DELAY) {
                    lastUpdateTime = System.currentTimeMillis();
                    motion.localizer.update();
                    unimetry.update();
                }
            }
        });
        updater.start();
    }

    // Initialize CV pipeline
    public void initCV() {
        int cameraMonitorViewId = hwmp.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hwmp.appContext.getPackageName());
        phoneCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
        phoneCam.openCameraDevice();
        cvPipeline = new CvPipeline(this);
        phoneCam.setPipeline(cvPipeline);
        phoneCam.startStreaming(1280, 720, OpenCvCameraRotation.UPRIGHT);
    }

    // Initialize dashboard RC client socket
    public void initDashboard() {
        try {
            rcClient = IO.socket(constants.ADDRESS);

            rcClient.on(Socket.EVENT_CONNECT, args -> {
                Utils.printSocketLog("RC", "SERVER", "connected", options);
            }).on(Socket.EVENT_DISCONNECT, args -> {
                Utils.printSocketLog("RC", "SERVER", "disconnected", options);
            }).on("dashboardJson", args -> {
                Utils.printSocketLog("SERVER", "RC", "dashboardJson", options);
                Utils.writeFile(args[0].toString(), dashboardJson);
            }).on("constantsUpdated", args -> {
                try {
                    Utils.printSocketLog("SERVER", "RC", "constantsUpdated", options);
                    constants.read(new JSONObject(args[0].toString()));
                    constants.write();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            rcClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Initialize OpMode
    public void initOpMode(String opModeID) {
        isRunning = true;
        this.opModeID = opModeID;
        status = "Running " + opModeID;

        Pose startPose = motion.waypoints.get(opModeID + ".waypoint.start");
        if (startPose == null) startPose = new Pose();
        motion.start = new RigidBody(startPose);
        motion.robot = new RigidBody(motion.start);
    }

    // Wrap up OpMode
    public void end() {
        status = "Ending";
        isRunning = false;

        if (updater != null && updater.isAlive() && !updater.isInterrupted()) updater.interrupt();

        if (phoneCam != null) {
            phoneCam.pauseViewport();
            phoneCam.stopStreaming();
            phoneCam.setPipeline(null);
            phoneCam.closeCameraDevice();
        }

        if (opModeID.startsWith("auto")) {
            try {
                JSONObject obj = new JSONObject(Utils.readFile(dashboardJson));
                JSONObject wpObj = obj.getJSONObject("waypoints");
                wpObj.put("tele." + (opModeID.contains("red") ? "red" : "blue") + ".waypoint.start", motion.robot.pose.toArray());
                obj.put("waypoints", wpObj);
                Utils.writeFile(obj.toString(), dashboardJson);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (options.debug) {
            rcClient.emit("dashboardJson", Utils.readFile(dashboardJson));
            Utils.printSocketLog("RC", "SERVER", "dashboardJson", options);
            rcClient.emit("opModeEnded", "{}");
            Utils.printSocketLog("RC", "SERVER", "opModeEnded", options);
            rcClient.close();
        }
    }

    //////////////////////////// PRESETS /////////////////////////////

    // Place a stone on foundation
    public void preset_placeStone() {
        motion.goToWaypoint("place");
        if (opModeID.contains("auto")) {
            appendages.setAutoClawSwingStatus("down");
            appendages.setAutoClawGripStatus("open");
            appendages.setAutoClawSwingStatus("up");
        } else {
            appendages.setVerticalSlidePosition(presetPlaceStoneTicks);
            appendages.setVerticalSlidePosition(0);
            presetPlaceStoneTicks += constants.STONE_VERT_SLIDE_TICKS;
        }
    }

}