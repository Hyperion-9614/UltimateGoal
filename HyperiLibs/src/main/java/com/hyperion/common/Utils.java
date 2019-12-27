package com.hyperion.common;

import com.hyperion.motion.math.PlanningPoint;
import com.hyperion.motion.math.Pose;

import org.apache.commons.math3.util.Precision;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class Utils {

    public static double round(double n, int places) {
        return Precision.round(n, places);
    }

    public static double[] roundArr(double[] arr, int places) {
        for (int i = 0; i < arr.length; i++)
            arr[i] = round(arr[i], places);
        return arr;
    }

    public static double[][] roundArr(double[][] arr, int places) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = roundArr(arr[i], places);
        }
        return arr;
    }

    public static boolean isCollinear(Pose o1, Pose o2, Pose o3) {
        if (slope(o1, o2) == slope(o2, o3)) return true;
        return false;
    }

    public static double distance(Pose o1, Pose o2) {
        return Math.sqrt(Math.pow(o2.x - o1.x, 2) + Math.pow(o2.y - o1.y, 2));
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static double slope(Pose o1, Pose o2) {
        return (o2.y - o1.y) / (o2.x - o1.x);
    }

    public static double slope(double x1, double y1, double x2, double y2) {
        return (y2 - y1) / (x2 - x1);
    }

    public static double sum(double[] nums) {
        double sum = 0;
        for (double n : nums) sum += n;
        return sum;
    }

    public static Pose midpoint(Pose o1, Pose o2) {
        return new Pose((o1.x + o2.x) / 2.0, (o1.y + o2.y) / 2.0);
    }

    public static double normalizeTheta(double theta, double min, double max) {
        while (theta < min) theta += 2 * Math.PI;
        while (theta >= max) theta -= 2 * Math.PI;
        return theta;
    }

    public static double optimalThetaDifference(double thetaStart, double thetaEnd) {
        double difference = normalizeTheta(thetaEnd, 0, 2 * Math.PI) - normalizeTheta(thetaStart, 0, 2 * Math.PI);
        if (difference < -Math.PI) difference += 2 * Math.PI;
        if (difference > Math.PI) difference -= 2 * Math.PI;
        return difference;
    }

    public static String readDataJSON(String fileName, Constants constants) {
        return readFile(new File(constants.RES_DATA_PREFIX + "/" + fileName + ".json"));
    }

    public static String readFile(File file) {
        try {
            return readStream(new FileInputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String readStream(InputStream stream) {
        String json = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                json += line;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static void writeDataJSON(String json, String fileName, Constants constants) {
        try {
            writeFile(new JSONObject(json).toString(4), new File(constants.RES_DATA_PREFIX + "/" + fileName + ".json"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String toWrite, File file) {
        try {
            writeStream(toWrite, new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeStream(String toWrite, OutputStream stream) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
            writer.write(toWrite);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String join(String separator, String[] elements) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i <  elements.length; i++) {
            joined.append(elements[i]);
            if (i < elements.length - 1) {
                joined.append(separator);
            }
        }
        return joined.toString();
    }

    public static ArrayList<PlanningPoint> toPlanningPointList(ArrayList<Pose> poses) {
        ArrayList<PlanningPoint> planningPoints = new ArrayList<>();
        for (Pose p : poses) {
            planningPoints.add(new PlanningPoint(p));
        }
        return planningPoints;
    }

    public static String getFormattedDate() {
        String pattern = "MM/dd/yyyy h:mm:ss:S a";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        return simpleDateFormat.format(new Date());
    }

    public static void printSocketLog(String from, String to, String message, Options options) {
        if (options.socketLog) {
            System.out.println("[" + Utils.getFormattedDate() + "] " + from.toUpperCase() + " -> " + to.toUpperCase() + ": " + message);
        }
    }

    public static double clip(double n, double min, double max) {
        if (n > max) n = max;
        else if (n < min) n = min;
        return n;
    }

    public static void printArray(double[] arr) {
        for (double d : arr) System.out.print(d + " ");
        System.out.println();
    }

    public static void printArray(double[][] arr) {
        for (double[] r : arr) {
            for (double c : r) {
                System.out.print(c + " ");
            }
            System.out.println();
        }
    }

    public static double[] spliceArr(double[] arr, int start, int end) {
        double[] spliced = new double[end - start];
        for (int i = start; i < end; i++) {
            spliced[i - start] = arr[i];
        }
        return spliced;
    }

    public static double[] combineArrs(double[] a, double[] b){
        double[] result = new double[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
    public static double[][] combineArrs(double[][] a, double[][] b){
        double[][] result = new double[a.length + b.length][];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static double[][] combineArrs(double[][] a, double[] b){
        double[][] result = new double[a.length + 1][];
        System.arraycopy(a, 0, result, 0, a.length);
        result[result.length - 1] = b;
        return result;
    }

    public static double[][] removeArr(double[][] a, int toRemove) {
        double[][] newArr = new double[a.length - 1][];
        int i = 0;
        while (i < newArr.length) {
            if (i != toRemove) {
                newArr[i] = a[i];
                i++;
            }
        }
        return newArr;
    }

    public static double[][] editArr(double[][] a, int toEdit, double[] edit) {
        double[][] newArr = new double[a.length][];
        int i = 0;
        while (i < newArr.length) {
            newArr[i] = a[i];
            if (i == toEdit) {
                newArr[i] = edit;
            }
            i++;
        }
        return newArr;
    }

    public static double[] coeffArr(double[] arr, double coeff) {
        double[] result = new double[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = arr[i] * coeff;
        return result;
    }

    public static double[] pad(double[] arr, int leftPad, int rightPad) {
        leftPad = Math.max(0, leftPad);
        rightPad = Math.max(0, rightPad);
        return combineArrs(combineArrs(new double[leftPad], arr), new double[rightPad]);
    }

    public static double arrMax(double[] arr) {
        double max = 0;
        for (double n : arr) max = Math.max(n, max);
        return max;
    }

    public static double arrMin(double[] arr) {
        double min = 0;
        for (double n : arr) min = Math.min(n, min);
        return min;
    }

}
