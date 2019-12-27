package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.Options;
import com.hyperion.common.Utils;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

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
 * Handles all hardware interfacing and robot/external initialization
 * Also contains presets
 */

public class Hardware {

    public ExpansionHubEx expansionHubL;
    public ExpansionHubEx expansionHubR;
    public HardwareMap hwmp;
    public BNO055IMU imu;
    public boolean isRunning;

    public Motion motion;
    public Appendages appendages;

    public OpMode context;
    public String opModeID = "Choose OpMode";

    public OpenCvInternalCamera phoneCam;
    public CvPipeline cvPipeline;

    public Socket rcClient;
    public Constants constants;
    public Options options;
    public Unimetry unimetry;
    public String status = "";

    public File dashboardJson;
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

    public CRServo foundationMoverL;
    public CRServo foundationMoverR;
    public CRServo claw;

    public int presetPlaceStoneCounts = 2500;

    public Hardware(OpMode context) {
        this.context = context;
        this.hwmp = context.hardwareMap;

        // Init files & resources
        try {
            constants = new Constants(new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/constants.json"));
            dashboardJson = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/data/dashboard.json");
            nnConfigJson = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/model/config.json");
            modelConfig = new File(hwmp.appContext.getFilesDir() + "/hyperilibs/model/model.config");
            options = new Options(dashboardJson);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Init hardware
        expansionHubL = hwmp.get(ExpansionHubEx.class, "Expansion Hub L");
        expansionHubR = hwmp.get(ExpansionHubEx.class, "Expansion Hub R");
        imu = hwmp.get(BNO055IMU.class, "imu");

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

        foundationMoverL = hwmp.crservo.get("foundationMoverL");
        foundationMoverR = hwmp.crservo.get("foundationMoverR");
        claw = hwmp.crservo.get("claw");

        // Init control & settings
        motion = new Motion(this);
        appendages = new Appendages(this);
        unimetry = new Unimetry(this);

        // Init options & dashboard
        if (options.debug) dashboardInit();
    }

    ///////////////////////// PRE & POST //////////////////////////

    // Dashboard init
    public void dashboardInit() {
        try {
            rcClient = IO.socket(constants.ADDRESS);

            rcClient.on(Socket.EVENT_CONNECT, args -> {
                Utils.printSocketLog("RC", "SERVER", "connected", options);
            }).on(Socket.EVENT_DISCONNECT, args -> {
                Utils.printSocketLog("RC", "SERVER", "disconnected", options);
            }).on("dashboardJson", args -> {
                Utils.printSocketLog("SERVER", "RC", "dashboardJson", options);
                Utils.writeFile(args[0].toString(), dashboardJson);
            });

            rcClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Init
    public void init() {
        isRunning = true;
        status = "Running " + opModeID;

        // Init CV
        int cameraMonitorViewId = hwmp.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hwmp.appContext.getPackageName());
        phoneCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
        phoneCam.openCameraDevice();
        cvPipeline = new CvPipeline(this);
        phoneCam.setPipeline(cvPipeline);
        phoneCam.startStreaming(1280, 720, OpenCvCameraRotation.UPRIGHT);
    }

    // Wrap up OpMode
    public void end() {
        status = "Ending";
        isRunning = false;

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
                wpObj.put("tele." + (opModeID.contains("red") ? "red" : "blue") + ".waypoint.sStart", motion.robot.pose.toArray());
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
        }
    }

    //////////////////////////// PRESETS /////////////////////////////

    // Place a stone on foundation
    public void preset_placeStone(boolean goToWaypoint) {
        if (goToWaypoint) motion.goToWaypoint("place");

        if (context.gamepad1.left_stick_x == 0 && context.gamepad1.left_stick_y == 0 && context.gamepad1.right_stick_x == 0) {
            appendages.setVerticalSlideTarget(presetPlaceStoneCounts);
            appendages.setVerticalSlideMode(DcMotor.RunMode.RUN_TO_POSITION);
            appendages.setVerticalSlidePower(constants.VERT_SLIDE_POWER);
            ElapsedTime timer = new ElapsedTime();
            while (timer.milliseconds() <= 2000 && vertSlideL.isBusy() && vertSlideR.isBusy()) {
                motion.localizer.update();
                unimetry.update();
            }
            appendages.setVerticalSlidePower(0);

            // TODO: Claw

            appendages.setVerticalSlideTarget(0);
            appendages.setVerticalSlideMode(DcMotor.RunMode.RUN_TO_POSITION);
            appendages.setVerticalSlidePower(constants.VERT_SLIDE_POWER);
            timer.reset();
            while (timer.milliseconds() <= 2000 && vertSlideL.isBusy() && vertSlideR.isBusy()) {
                motion.localizer.update();
                unimetry.update();
            }
            appendages.setVerticalSlidePower(0);
            presetPlaceStoneCounts += constants.STONE_VERT_SLIDE_COUNTS;
        }
    }

}
