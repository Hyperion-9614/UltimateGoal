package com.hyperion.common;

import com.hyperion.motion.math.Pose;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;

/**
 * Allows static access to the constants file
 * All values in the constants file can be identified
 * by a dot-separated flat ID String
 * <p>
 * Use the appropriate getter method for the type of value
 * that you expect to find in the constants file
 */
public class Constants {

    private static File file;
    public static JSONObject root;

    /**
     * Sets the file to retrieve constants from
     *
     * @param  file  a file that references res/data/constants.json
     */
    public static void init(File file) {
        try {
            Constants.file = file;
            init(new JSONObject(new JSONTokener(IOUtils.readFile(file))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the root json object to pull values from
     *
     * @param  root  a json object representation
     *               of the constants file
     */
    public static void init(JSONObject root) {
        Constants.root = root;
    }

    /**
     * Finds an Object value at a specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id  the dot-separated flat ID String
     * @return     the Object with the specified ID in constants
     */
    public static Object getByID(String id) {
        String[] split = id.split("\\.");
        JSONObject curr = root;
        String currSegment = "";
        try {
            for (int i = 0; i < split.length; i++) {
                int i0 = split[i].lastIndexOf("[");
                int jAI = split[i].endsWith("]") ? Integer.parseInt(split[i].substring(i0 + 1, i0 + 2)) : -1;
                if (jAI == -1) {
                    currSegment = split[i];
                    if (i == split.length - 1) return curr.get(currSegment);
                    else curr = curr.getJSONObject(currSegment);
                } else {
                    currSegment = split[i].substring(0, i0) + "[" + jAI + "]";
                    JSONArray jsonArray = curr.getJSONArray(split[i].substring(0, i0));
                    if (i == split.length - 1) return jsonArray.get(jAI);
                    else curr = jsonArray.getJSONObject(jAI);
                }
            }
        } catch (JSONException e) {
            throw new MissingConstantException("No constant found | ID: '" + id + "' | Segment: '"
                                                + currSegment + "'", e);
        }
        return curr;
    }

    /**
     * Finds an expected String at a specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id  the dot-separated flat ID String
     * @return     the String with the specified ID in constants
     */
    public static String getString(String id) {
        return String.valueOf(getByID(id));
    }

    /**
     * Finds an expected int at a specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id  the dot-separated flat ID String
     * @return     the int with the specified ID in constants
     */
    public static int getInt(String id) {
        return Integer.parseInt(getString(id));
    }

    /**
     * Finds an expected long at a specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id  the dot-separated flat ID String
     * @return     the long with the specified ID in constants
     */
    public static long getLong(String id) {
        return Long.parseLong(getString(id));
    }

    /**
     * Finds an expected double at a specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id  the dot-separated flat ID String
     * @return     the double with the specified ID in constants
     */
    public static double getDouble(String id) {
        return Double.parseDouble(getString(id));
    }

    /**
     * Finds an expected boolean at a specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id  the dot-separated flat ID String
     * @return     the boolean with the specified ID in constants
     */
    public static boolean getBoolean(String id) {
        return Boolean.parseBoolean(getString(id));
    }

    /**
     * Finds an expected JSONObject at a specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id  the dot-separated flat ID String
     * @return     the JSONObject with the specified ID in constants
     */
    public static JSONObject getJSONObject(String id) {
        return (JSONObject) getByID(id);
    }

    /**
     * Finds an expected JSONArray at a specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id  the dot-separated flat ID String
     * @return     the JSONArray with the specified ID in constants
     */
    public static JSONArray getJSONArray(String id) {
        return (JSONArray) getByID(id);
    }

    /**
     * Finds an expected Pose object at a specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id  the dot-separated flat ID String
     * @return     the Pose with the specified ID in constants
     */
    public static Pose getPose(String id) {
        try {
            JSONArray poseArr = getJSONArray(id);
            return new Pose(poseArr.getDouble(0), poseArr.getDouble(1), Math.toRadians(poseArr.getDouble(2)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Pose();
    }

    /**
     * Creates a correctly formatted File object
     * of a file in the res folder,
     * at the specified file path
     *
     * @param  resDir    the subfolder in res to find the file at
     * @param  filePath  the path/name of the file within a subfolder of res
     * @return           the file in the specified res directory/file path
     */
    public static File getFile(String resDir, String filePath) {
        File res = new File(System.getProperty("user.dir") + getString("io.resPrefix"));
        return new File(res + "\\" + resDir + "\\" + filePath.replaceAll("/", "\\"));
    }

    /**
     * Sets the generic type value of
     * a constant with the specified
     * ID/path in the constants file
     * <p>
     * For example, the size of a waypoint has an ID of:
     *      dashboard.gui.sizes.waypoint
     * <p>
     * Look at the structure of json objects & values
     * in the constants.json file to find the ID
     *
     * @param  id     the dot-separated flat ID String
     * @param  value  the generic type value to set the value at the ID to
     * @param  write  should the edit be saved into the file?
     */
    public static <T> void setAtID(String id, T value, boolean write) {
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
            if (write) write();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts odometry encoder counts to
     * IRL meters using the formula:
     *     counts / ((CYCLES_PER_REV * 4) / ODO_WHEEL_CIRCUMFERENCE)
     *
     * @param  counts  the number of odometry encoder counts travelled
     * @return         the number of meters traveled
     */
    public static double countsToM(double counts) {
        return counts / ((getInt("localization.odometryCyclesPerRevolution") * 4) / (2 * Math.PI * getDouble("localization.odometryWheelRadius")));
    }

    /**
     * Writes the constants JSONObject to its file
     */
    public static void write() {
        try {
            IOUtils.writeFile(root.toString(4), file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MissingConstantException extends RuntimeException {

        public MissingConstantException(String errorMessage, Throwable err) {
            super(errorMessage, err);
        }

    }

}
