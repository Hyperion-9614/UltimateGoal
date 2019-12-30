package com.hyperion.motion.math;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.ArrayList;
import java.util.Iterator;

public class Piecewise {

    public ArrayList<Interval> intervals = new ArrayList<>();

    public Piecewise() {

    }

    public Piecewise(JSONArray jsonArray) {
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject intervalObj = jsonArray.getJSONObject(i);
                intervals.add(new Interval(intervalObj.getDouble("a"), intervalObj.getDouble("b"), new Expression(intervalObj.getString("exp"))));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setInterval(double a, double b, String exp) {
        Interval setTo = new Interval(a, b, new Expression(exp));
        for (Iterator iter = intervals.iterator(); iter.hasNext();) {
            Interval interval = (Interval) iter.next();
            if (interval.boundsEquals(setTo)) {
                iter.remove();
            }
        }
        intervals.add(setTo);
    }

    public double evaluate(double t, int derivative) {
        derivative = Math.max(0, derivative);
        for (Interval interval : intervals) {
            if (t >= interval.a && t < interval.b) {
                String expression = getExpressionString(interval.a, interval.b, derivative);
                return new Expression(expression, new Argument("t = " + t)).calculate();
            }
        }
        return 0;
    }

    public String getExpressionString(double a, double b, int derivative) {
        String expression = "";
        for (int i = 0; i < size(); i++) {
            if (intervals.get(i).boundsEquals(a, b)) {
                expression = intervals.get(i).exp.getExpressionString();
                break;
            }
        }
        for (int i = 0; i < derivative; i++) {
            expression = "der(" + expression + ", t)";
        }
        return expression;
    }

    public void changeInterval(double aOld, double bOld, double aNew, double bNew) {
        for (int i = 0; i < size(); i++) {
            if (intervals.get(i).boundsEquals(aOld, bOld)) {
                intervals.set(i, new Interval(aNew, bNew, intervals.get(i).exp));
            }
        }
    }

    public JSONArray toJSONArray() {
        JSONArray arr = new JSONArray();
        try {
            for (Interval interval : intervals) {
                arr.put(interval.toJSONObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    public int size() {
        return intervals.size();
    }

    public class Interval {

        public double a;
        public double b;
        public Expression exp;

        public Interval(double a, double b, Expression exp) {
            this.a = a;
            this.b = b;
            this.exp = exp;
        }

        public boolean boundsEquals(double a, double b) {
            return this.a == a && this.b == b;
        }

        public boolean boundsEquals(Interval other) {
            return boundsEquals(other.a, other.b);
        }

        public JSONObject toJSONObject() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("a", a);
                obj.put("b", b);
                obj.put("exp", exp.getExpressionString());
                return obj;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

}
