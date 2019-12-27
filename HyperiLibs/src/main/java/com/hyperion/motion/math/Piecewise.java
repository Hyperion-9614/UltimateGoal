package com.hyperion.motion.math;

import com.hyperion.common.Utils;
import com.hyperion.motion.trajectory.SplineTrajectory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Function;

import java.util.HashMap;
import java.util.Iterator;

public class Piecewise {

    public HashMap<double[], Function> intervals;

    public Piecewise() {
        intervals = new HashMap<>();
    }

    public void setLTVInterval(PlanningPoint pp0, PlanningPoint pp1) {
        double slope = Utils.slope(pp0.distance, pp0.translationalVelocity.magnitude, pp1.distance, pp1.translationalVelocity.magnitude);
        setInterval(pp0.distance, pp1.distance, "(" + slope + ")*d + (" + -(slope * pp0.distance) + ") + (" + pp0.translationalVelocity.magnitude + ")");
    }

    public void setLTAInterval(PlanningPoint pp0, PlanningPoint pp1) {
        double slope = Utils.slope(pp0.distance, pp0.translationalAcceleration.magnitude, pp1.distance, pp1.translationalAcceleration.magnitude);
        setInterval(pp0.distance, pp1.distance, "(" + slope + ")*d + (" + -(slope * pp0.distance) + ") + (" + pp0.translationalAcceleration.magnitude + ")");
    }

    public void setInterval(double d0, double d1, String expression) {
        intervals.put(new double[]{ d0, d1 }, new Function("f(d) = " + expression));
    }

    public Vector2D evaluate(double d, SplineTrajectory spline) {
        Vector2D vec = new Vector2D();
        for (double[] interval : intervals.keySet()) {
            if (d >= interval[0] && d < interval[1]) {
                double[] derivative = spline.getDerivative(d, 1);
                vec = new Vector2D(intervals.get(interval).calculate(d), Utils.normalizeTheta(Math.atan2(derivative[1], derivative[0]), 0, 2 * Math.PI), false);
                break;
            }
        }
        return vec;
    }

    public double[][] getIntervals() {
        double[][] intvs = new double[intervals.size()][];
        Iterator<double[]> keys = intervals.keySet().iterator();
        for (int i = 0; i < intervals.size(); i++) {
            intvs[i] = keys.next();
        }
        return intvs;
    }

    public JSONArray toJSONArray() {
        JSONArray arr = new JSONArray();
        try {
            for (double[] interval : intervals.keySet()) {
                JSONObject intervalObj = new JSONObject();
                intervalObj.put("d0", interval[0]);
                intervalObj.put("d1", interval[1]);
                intervalObj.put("expression", intervals.get(interval).getFunctionExpressionString());
                arr.put(intervalObj);
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

}
