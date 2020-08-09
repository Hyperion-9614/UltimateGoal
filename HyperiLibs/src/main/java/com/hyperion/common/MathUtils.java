package com.hyperion.common;

import com.hyperion.motion.math.Pose;

import org.apache.commons.math3.util.Precision;

import java.util.Random;

public class MathUtils {

    public static double round(double n, int places) {
        return Precision.round(n, places);
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

    public static double norm(double theta, double min, double max) {
        while (theta < min) theta += 2 * Math.PI;
        while (theta >= max) theta -= 2 * Math.PI;
        return theta;
    }

    public static double norm(double theta) {
        return norm(theta, 0, 2 * Math.PI);
    }

    public static double optThetaDiff(double thetaStart, double thetaEnd) {
        double difference = norm(thetaEnd, 0, 2 * Math.PI) - norm(thetaStart, 0, 2 * Math.PI);
        if (difference < -Math.PI) difference += 2 * Math.PI;
        if (difference > Math.PI) difference -= 2 * Math.PI;
        return difference;
    }

    public static double clip(double n, double min, double max) {
        if (n > max) n = max;
        else if (n < min) n = min;
        return n;
    }

    public static boolean isInRange(double val, double a, double b) {
        if (a > b) {
            double temp = a;
            a = b;
            b = temp;
        }
        return val >= a && val <= b;
    }

    public static int sign(double val) {
        return (val >= 0 ? 1 : -1);
    }

    public static double randInRange(Random rand, double lower, double upper) {
        return lower + (upper - lower) * rand.nextDouble();
    }

    public static double halfway(double a, double b) {
        return a + (b - a) / 2;
    }

}
