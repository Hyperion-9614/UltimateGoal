package com.hyperion.common;

import com.hyperion.motion.math.Pose;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.net.InetAddress;

public class Constants {

    private static File file;
    public static JSONObject root;

    public static void init(File file) {
        try {
            Constants.file = file;
            init(new JSONObject(new JSONTokener(IOUtils.readFile(file))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init(JSONObject root) {
        try {
            Constants.root = root;

            if (getString("dashboard.net.hostIP").equals("this")) {
                setAtID("dashboard.net.hostIP", InetAddress.getLocalHost().getHostAddress());
            }

            System.out.println("Host IP: " + getString("dashboard.net.hostIP"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object getByID(String id) {
        String[] split = id.split("\\.");
        JSONObject curr = root;
        try {
            for (int i = 0; i < split.length; i++) {
                int i0 = split[i].lastIndexOf("[");
                int jAI = split[i].endsWith("]") ? Integer.parseInt(split[i].substring(i0 + 1, i0 + 2)) : -1;
                if (jAI == -1) {
                    if (i == split.length - 1) return curr.get(split[i]);
                    else curr = curr.getJSONObject(split[i]);
                } else {
                    JSONArray jsonArray = curr.getJSONArray(split[i].substring(0, i0));
                    if (i == split.length - 1) return jsonArray.get(jAI);
                    else curr = jsonArray.getJSONObject(jAI);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return curr;
    }

    public static String getString(String id) {
        return String.valueOf(getByID(id));
    }
    public static int getInt(String id) {
        return Integer.parseInt(getString(id));
    }
    public static double getDouble(String id) {
        return Double.parseDouble(getString(id));
    }
    public static JSONObject getJSONObject(String id) {
        return (JSONObject) getByID(id);
    }
    public static JSONArray getJSONArray(String id) {
        return (JSONArray) getByID(id);
    }

    public static File getFile(String resDir, String filePath) {
        File res = new File(System.getProperty("user.dir") + getString("io.filepaths.resPrefix"));
        return new File(res + "\\" + resDir + "\\" + filePath.replaceAll("/", "\\"));
    }
    public static Pose getPose(String id) {
        try {
            JSONArray poseArr = getJSONArray(id);
            return new Pose(poseArr.getDouble(0), poseArr.getDouble(1), Math.toRadians(poseArr.getDouble(2)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Pose();
    }

    public static <T> void setAtID(String id, T value) {
        String[] split = id.split("\\.");
        JSONObject curr = root;
        try {
            for (int i = 0; i < split.length - 1; i++) {
                int i0 = split[i].lastIndexOf("[");
                int jAI = split[i].endsWith("]") ? Integer.parseInt(split[i].substring(i0 + 1, i0 + 2)) : -1;
                if (jAI == -1) {
                    curr = curr.getJSONObject(split[i]);
                } else {
                    JSONArray jsonArray = curr.getJSONArray(split[i].substring(0, i0));
                    curr = jsonArray.getJSONObject(jAI);
                }
            }
            int i = split.length - 1;
            if (id.endsWith("]")) {
                int i0 = split[i].lastIndexOf("[");
                int jAI = Integer.parseInt(split[i].substring(i0 + 1, i0 + 2));
                JSONArray jsonArray = curr.getJSONArray(split[i].substring(0, i0));
                jsonArray.put(jAI, value);
            } else {
                curr.put(split[i], value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double countsToM(double counts) {
        return counts / ((getInt("localization.odometryCyclesPerRevolution") * 4) / (2 * Math.PI * getDouble("localization.odometryWheelRadius")));
    }

    public static void write() {
        try {
            IOUtils.writeFile(root.toString(4), file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
