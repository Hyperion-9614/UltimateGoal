package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.IOUtils;
import com.hyperion.common.TextUtils;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.modules.CvPipeline;
import org.firstinspires.ftc.teamcode.modules.RectangleSampling;
import org.firstinspires.ftc.teamcode.modules.Metrics;
import org.json.JSONArray;
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
 */

public class Hardware {

    public ExpansionHubEx expansionHubL;
    public ExpansionHubEx expansionHubR;
    public HardwareMap hwmp;
    public boolean isRunning;
    public Thread localizationUpdater;
    public Thread unimetryUpdater;
    public ElapsedTime autoTime;

    public LinearOpMode ctx;
    public String opModeID = "Choose OpMode";

    public OpenCvInternalCamera phoneCam;
    public CvPipeline cvPipeline;

    public Socket rcClient;
    public Metrics metrics;
    public String status = opModeID;

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

    public int opModeSelectorIndex = -1;
    public String[] autoOpModeIDs = new String[]{ "auto.blue.full", "auto.red.full",
                                                  "auto.blue.foundation", "auto.red.foundation",
                                                  "auto.blue.brick", "auto.red.brick",
                                                };
    public String[] teleOpModeIDs = new String[]{ "tele.blue", "tele.red",
                                                  "tele.test"
                                                };

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
        initDashboard();
        metrics = new Metrics(this);
        initCV();
        initUpdaters();
    }

    ///////////////////////// INIT //////////////////////////

    // Initialize localizationUpdater and unimetryUpdater threads
    public void initUpdaters() {
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

        unimetryUpdater = new Thread(() -> {
            long lastUpdateTime = System.currentTimeMillis();
            while (!unimetryUpdater.isInterrupted() && unimetryUpdater.isAlive() && !ctx.isStopRequested()) {
                if (System.currentTimeMillis() - lastUpdateTime >= Constants.getInt("teamcode.unimetryDelay")) {
                    lastUpdateTime = System.currentTimeMillis();
                    metrics.update();
                }
            }
        });
        unimetryUpdater.start();
    }

    // Initialize CV pipeline
    public void initCV() {
        int cameraMonitorViewId = hwmp.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hwmp.appContext.getPackageName());
        phoneCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);
        phoneCam.openCameraDevice();
        cvPipeline = new RectangleSampling();
        phoneCam.setPipeline(cvPipeline);
        phoneCam.startStreaming(1280, 720, OpenCvCameraRotation.SIDEWAYS_LEFT);
        phoneCam.setFlashlightEnabled(true);
        for (OpenCvInternalCamera.FrameTimingRange r : phoneCam.getFrameTimingRangesSupportedByHardware()) {
            if (r.max == 30 && r.min == 30) {
                phoneCam.setHardwareFrameTimingRange(r);
                break;
            }
        }
    }

    // Initialize dashboard RC client socket
    public void initDashboard() {
        try {
            rcClient = IO.socket("http://" + Constants.getString("dashboard.net.hostIP") + ":" + Constants.getString("dashboard.net.port"));

            rcClient.on(Socket.EVENT_CONNECT, args -> {
                TextUtils.printSocketLog("RC", "SERVER", "connected");
            }).on(Socket.EVENT_DISCONNECT, args -> {
                TextUtils.printSocketLog("RC", "SERVER", "disconnected");
            }).on("fieldEdited", args -> {
                TextUtils.printSocketLog("SERVER", "RC", "fieldEdited");
                writeFieldEditsToFieldJSON(args[0].toString());
            }).on("constantsUpdated", args -> {
                try {
                    TextUtils.printSocketLog("SERVER", "RC", "constantsUpdated");
                    Constants.init(new JSONObject(args[0].toString()));
                    Constants.write();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                initFiles();
            });

            rcClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Exactly what the method header says
    public void writeFieldEditsToFieldJSON(String json) {
        try {
            JSONObject field = new JSONObject(IOUtils.readFile(fieldJSON));
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                FieldEdit edit = new FieldEdit(arr.getJSONObject(i));
                if (!edit.id.equals("robot")) {
                    String o = edit.id.contains("waypoint") ? "waypoints" : "splines";
                    JSONObject target = field.getJSONObject(o);
                    switch (edit.type) {
                        case CREATE:
                        case EDIT_BODY:
                            target.put(edit.id, o.equals("waypoints") ? new JSONArray(edit.body) : new JSONObject(edit.body));
                            break;
                        case EDIT_ID:
                            if (edit.id.contains("waypoint")) {
                                JSONArray wpArr = target.getJSONArray(edit.id);
                                target.remove(edit.id);
                                target.put(edit.body, wpArr);
                            } else {
                                JSONObject splineObj = target.getJSONObject(edit.id);
                                target.remove(edit.id);
                                target.put(edit.body, splineObj);
                            }
                            break;
                        case DELETE:
                            target.remove(edit.id);
                            break;
                    }
                    field.put(o, target);
                }
            }
            IOUtils.writeFile(field.toString(), fieldJSON);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initFiles();
    }

    // Initialize OpMode
    public void initOpMode(String opModeID) {
        isRunning = true;
        this.opModeID = opModeID;
        status = "Running " + opModeID;

        Pose startPose = Motion.waypoints.get(opModeID + ".waypoint.start");
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

    public void initLoop(boolean isAuto) {
        String[] opModeIDs = isAuto ? autoOpModeIDs : teleOpModeIDs;
        if (ctx.gamepad1.dpad_up) {
            opModeSelectorIndex++;
            if (opModeSelectorIndex >= opModeIDs.length)
                opModeSelectorIndex = 0;
            ctx.sleep(250);
            initOpMode(opModeIDs[opModeSelectorIndex]);
        } else if (ctx.gamepad1.dpad_down) {
            opModeSelectorIndex--;
            if (opModeSelectorIndex < 0)
                opModeSelectorIndex = opModeIDs.length - 1;
            ctx.sleep(250);
            initOpMode(opModeIDs[opModeSelectorIndex]);
        }

        if (isAuto) {
            if (ctx.gamepad1.dpad_right) {
                if (!opModeID.equals("Choose OpMode") && Motion.getWaypoint("parkEast") != null) {
                    Motion.waypoints.put("park", Motion.getWaypoint("parkEast"));
                }
            } else if (ctx.gamepad1.dpad_left) {
                if (!opModeID.equals("Choose OpMode") && Motion.getWaypoint("parkWest") != null) {
                    Motion.waypoints.put("park", Motion.getWaypoint("parkWest"));
                }
            }
        }
    }

    ///////////////////////// GENERAL & PRESETS //////////////////////////

    // Choose between two values depending on opMode color
    public double choose(double blue, double red) {
        return opModeID.contains("blue") ? blue : red;
    }

    ///////////////////////// END //////////////////////////

    public void killCV() {
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
        if (opModeID.contains("auto") && Motion.robot.distanceTo(Motion.getWaypoint("park")) > Constants.getDouble("motionProfile.endErrorThresholds.translation")) {
            Motion.pidMove("park");
        }

        status = "Ending";
        isRunning = false;

        if (localizationUpdater != null && localizationUpdater.isAlive() && !localizationUpdater.isInterrupted())
            localizationUpdater.interrupt();
        if (unimetryUpdater != null && unimetryUpdater.isAlive() && !unimetryUpdater.isInterrupted())
            unimetryUpdater.interrupt();

        try {
            if (opModeID.startsWith("auto")) {
                JSONObject obj = new JSONObject(IOUtils.readFile(fieldJSON));
                JSONObject wpObj = obj.getJSONObject("waypoints");
                String key = "tele." + (opModeID.contains("red") ? "red" : "blue") + ".waypoint.start";
                JSONArray wpArr = new JSONArray(Motion.robot.toArray());
                wpObj.put(key, wpArr);
                obj.put("waypoints", wpObj);
                IOUtils.writeFile(obj.toString(), fieldJSON);
            }

            if (rcClient != null) {
                rcClient.emit("opModeEnded", "{}");
                TextUtils.printSocketLog("RC", "SERVER", "opModeEnded");
                rcClient.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!ctx.isStopRequested() || ctx.opModeIsActive()) {
            ctx.requestOpModeStop();
        }
    }

}