package com.hyperion.common;

import com.hyperion.motion.math.Pose;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.net.InetAddress;

public class Constants {

    // Localization
    public Pose XL_REL; // cm, cm, degrees
    public Pose XR_REL; // cm, cm, degrees
    public Pose Y_REL ; // cm, cm, degrees
    public double TRACK_WIDTH; // cm
    public double DRIVE_BASE; // cm
    public double ODO_WHEEL_RADIUS; // cm
    public double ODO_WHEEL_CIRCUMFERENCE; // cm
    public int ODO_CYCLES_PER_REV; // cycles
    public int ODO_CPR; // counts
    public double COUNTS_PER_M; // counts
    public double FIELD_SIDE_LENGTH; // cm
    public double COORD_AXIS_LENGTH_UNITS; // coords
    public double M_PER_COORD; // cm

    // PID
    public double X_K_P; // coefficient
    public double X_K_I; // coefficient
    public double X_K_D; // coefficient
    public double Y_K_P; // coefficient
    public double Y_K_I; // coefficient
    public double Y_K_D; // coefficient
    public double THETA_K_P; // coefficient
    public double THETA_K_I; // coefficient
    public double THETA_K_D; // coefficient
    public double U_X_SCALE; // scaling factor
    public double U_Y_SCALE; // scaling factor
    public double U_THETA_SCALE; // scaling factor

    // Motion Profile
    public double MP_K_TA; // coefficient
    public double MP_K_TV; // coefficient
    public double MP_K_AA; // coefficient
    public double MP_K_AV; // coefficient
    public double MAX_SEGMENT_LENGTH; // cm
    public double MAX_BISECTION_ERROR; // cm
    public double MAX_TRANSLATIONAL_VELOCITY; // cm/s
    public double MAX_TRANSLATIONAL_ACCELERATION; // cm/s^2
    public double MAX_TRANSLATIONAL_DECELERATION; // cm/s^2
    public double MAX_ANGULAR_VELOCITY; // rad/s
    public double MAX_ANGULAR_ACCELERATION; // rad/s^2
    public double MAX_ANGULAR_DECELERATION; // rad/s^2
    public double END_TRANSLATION_ERROR_THRESHOLD; // coords
    public double END_ROTATION_ERROR_THRESHOLD; // degrees

    // CV
    public double BLACK_THRESHOLD; // threshold

    // I/O
    public File RES_PREFIX; // file
    public File RES_DATA_PREFIX; // file
    public File RES_IMG_PREFIX; // file

    // Appendages
    public int STONE_VERT_SLIDE_TICKS; // ticks
    public double VERT_SLIDE_POWER; // power

    // Dashboard
    public String HOST_IP; // IP Address
    public int PORT; // Port
    public String ADDRESS; // Web Address
    public String RC_IP;
    public String DASHBOARD_VERSION;
    public double WAYPOINT_SIZE; // pixels
    public double PLANNINGPOINT_SIZE; // pixels
    public double PATHPOINT_SIZE; // pixels
    public double WBB_STROKE_WIDTH; // pixels
    public double WBB_GRAY_SCALE; // pixels

    public Constants(File file) {
        try {
            JSONTokener tokener = new JSONTokener(Utils.readFile(file));
            JSONObject root = new JSONObject(tokener);

            JSONObject localization = root.getJSONObject("localization");
            JSONObject odometryPoses = localization.getJSONObject("odometryPoses");
            JSONArray xLRelArr = odometryPoses.getJSONArray("xLRelativePose");
            XL_REL = new Pose(xLRelArr.getDouble(0), xLRelArr.getDouble(1), Math.toRadians(xLRelArr.getDouble(2)));
            JSONArray xRRelArr = odometryPoses.getJSONArray("xRRelativePose");
            XR_REL = new Pose(xRRelArr.getDouble(0), xRRelArr.getDouble(1), Math.toRadians(xRRelArr.getDouble(2)));
            JSONArray yRelArr = odometryPoses.getJSONArray("yRelativePose");
            Y_REL = new Pose(yRelArr.getDouble(0), yRelArr.getDouble(1), Math.toRadians(yRelArr.getDouble(2)));

            TRACK_WIDTH = localization.getDouble("trackWidth");
            DRIVE_BASE = localization.getDouble("driveBase");
            ODO_WHEEL_RADIUS = localization.getDouble("odometryWheelRadius");
            ODO_WHEEL_CIRCUMFERENCE = 2 * Math.PI * ODO_WHEEL_RADIUS;
            ODO_CYCLES_PER_REV = localization.getInt("odometryCyclesPerRevolution");
            ODO_CPR = ODO_CYCLES_PER_REV * 4;
            COUNTS_PER_M = ODO_CPR / ODO_WHEEL_CIRCUMFERENCE;
            FIELD_SIDE_LENGTH = localization.getDouble("fieldSideLength");
            COORD_AXIS_LENGTH_UNITS = FIELD_SIDE_LENGTH;
            M_PER_COORD = FIELD_SIDE_LENGTH / COORD_AXIS_LENGTH_UNITS;

            JSONObject pid = root.getJSONObject("pid");
            JSONObject x = pid.getJSONObject("x");
            X_K_P = x.getDouble("kP");
            X_K_I = x.getDouble("kI");
            X_K_D = x.getDouble("kD");
            JSONObject y = pid.getJSONObject("y");
            Y_K_P = y.getDouble("kP");
            Y_K_I = y.getDouble("kI");
            Y_K_D = y.getDouble("kD");
            JSONObject theta = pid.getJSONObject("theta");
            THETA_K_P = theta.getDouble("kP");
            THETA_K_I = theta.getDouble("kI");
            THETA_K_D = theta.getDouble("kD");
            JSONObject outputScales = pid.getJSONObject("outputScales");
            U_X_SCALE = outputScales.getDouble("uX");
            U_Y_SCALE = outputScales.getDouble("uY");
            U_THETA_SCALE = outputScales.getDouble("uTheta");

            JSONObject splinesMotionProfile = root.getJSONObject("splinesMotionProfile");
            JSONObject k = splinesMotionProfile.getJSONObject("k");
            MP_K_TA = k.getDouble("kTv");
            MP_K_TV = k.getDouble("kTa");
            MP_K_AA = k.getDouble("kAa");
            MP_K_AV = k.getDouble("kAv");
            JSONObject maxes = splinesMotionProfile.getJSONObject("maxes");
            MAX_SEGMENT_LENGTH = maxes.getDouble("segmentLength");
            MAX_BISECTION_ERROR = maxes.getDouble("bisectionError");
            MAX_TRANSLATIONAL_VELOCITY = maxes.getDouble("translationalVelocity");
            MAX_TRANSLATIONAL_ACCELERATION = maxes.getDouble("translationalAcceleration");
            MAX_TRANSLATIONAL_DECELERATION = maxes.getDouble("translationalDeceleration");
            MAX_ANGULAR_VELOCITY = maxes.getDouble("angularVelocity");
            MAX_ANGULAR_ACCELERATION = maxes.getDouble("angularAcceleration");
            MAX_ANGULAR_DECELERATION = maxes.getDouble("angularDeceleration");
            JSONObject endErrorThresholds = splinesMotionProfile.getJSONObject("endErrorThresholds");
            END_TRANSLATION_ERROR_THRESHOLD = endErrorThresholds.getDouble("translation");
            END_ROTATION_ERROR_THRESHOLD = Math.toRadians(endErrorThresholds.getDouble("rotation"));

            JSONObject cv = root.getJSONObject("cv");
            BLACK_THRESHOLD = cv.getDouble("blackThreshold");

            JSONObject io = root.getJSONObject("io");
            JSONObject filePaths = io.getJSONObject("filePaths");
            RES_PREFIX = new File(System.getProperty("user.dir") + filePaths.getString("resPrefix"));
            RES_DATA_PREFIX = new File(RES_PREFIX + filePaths.getString("resDataPrefix"));
            RES_IMG_PREFIX = new File(RES_PREFIX + filePaths.getString("resImgPrefix"));

            JSONObject appendages = root.getJSONObject("appendages");
            STONE_VERT_SLIDE_TICKS = appendages.getInt("stoneVerticalSlideCounts");
            VERT_SLIDE_POWER = appendages.getDouble("verticalSlidePower");

            JSONObject dashboard = root.getJSONObject("dashboard");
            DASHBOARD_VERSION = dashboard.getString("version");
            JSONObject net = dashboard.getJSONObject("net");
            HOST_IP = net.getString("hostIP");
            if (HOST_IP.equals("this")) {
                HOST_IP = InetAddress.getLocalHost().getHostAddress();
            }
            System.out.println("Host IP: " + HOST_IP);
            PORT = net.getInt("port");
            ADDRESS = "http://" + HOST_IP + ":" + PORT;
            RC_IP = net.getString("rcIP");
            JSONObject gui = dashboard.getJSONObject("gui");
            JSONObject sizes = gui.getJSONObject("sizes");
            WAYPOINT_SIZE = sizes.getDouble("waypoint");
            PLANNINGPOINT_SIZE = sizes.getDouble("planningPoint");
            PATHPOINT_SIZE = sizes.getDouble("pathPoint");
            JSONObject wbb = gui.getJSONObject("wbb");
            WBB_STROKE_WIDTH = wbb.getDouble("strokeWidth");
            WBB_GRAY_SCALE = wbb.getDouble("grayScale");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double mToCoords(double m) {
        return m / M_PER_COORD;
    } // coords
    public double countsToM(double counts) {
        return counts / COUNTS_PER_M;
    } // m

}
