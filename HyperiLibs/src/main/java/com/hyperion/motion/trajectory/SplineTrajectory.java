package com.hyperion.motion.trajectory;

import com.hyperion.common.Constants;
import com.hyperion.common.Utils;
import com.hyperion.motion.math.PlanningPoint;
import com.hyperion.motion.math.Pose;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.ArrayList;

/**
 * Creates a 2D parametric cubic spline curve given a set of at least 2 waypoints
 * References:
 *  (1) https://timodenk.com/blog/cubic-spline-interpolation/
 *  (3) http://www.nabla.hr/PC-ParametricEqu1.htm
 *  (2) https://people.cs.clemson.edu/~dhouse/courses/405/notes/splines.pdf
 *  (3) https://homepage.cs.uiowa.edu/~kearney/pubs/CurvesAndSurfacesArcLength.pdf
 */

public class SplineTrajectory {

    public Constants constants;
    public ArrayList<PlanningPoint> waypoints;
    public ArrayList<PlanningPoint> planningPoints;
    public double[][][] coefficients;
    public double[][][] planningCoefficients;
    public MotionProfile motionProfile;

    public int numIntervals;
    public double segmentLength;

    public SplineTrajectory(Constants constants) {
        this.constants = constants;
        motionProfile = new MotionProfile(this);
    }

    public SplineTrajectory(ArrayList<PlanningPoint> waypoints, Constants constants) {
        this.constants = constants;
        this.waypoints = waypoints;
        motionProfile = new MotionProfile(this);
        endPath();
    }

    public SplineTrajectory(String json, Constants constants) {
        this.constants = constants;
        motionProfile = new MotionProfile(this);
        readJson(json);
    }

    public void endPath() {
        if (waypoints.size() >= 2) {
            recalculate(waypoints, true);
        }
    }

    ///////////////////////////////////// I/O /////////////////////////////////////

    public String writeJson() {
        JSONObject obj = new JSONObject();

        try {
            JSONArray waypointsArray = new JSONArray();
            for (PlanningPoint waypoint : waypoints) {
                waypointsArray.put(waypoint.toArray());
            }
            obj.put("waypoints", waypointsArray);

            if (waypointsArray.length() >= 2) {
                JSONArray coefficientsArray = new JSONArray();
                for (double[][] xyCoeffs : coefficients) {
                    JSONArray xyCoeffsArray = new JSONArray();
                    xyCoeffsArray.put(xyCoeffs[0]);
                    xyCoeffsArray.put(xyCoeffs[1]);
                    coefficientsArray.put(xyCoeffsArray);
                }
                obj.put("coefficients", coefficientsArray);

                JSONArray planningPointsArray = new JSONArray();
                for (PlanningPoint pp : planningPoints) {
                    planningPointsArray.put(pp.toArray());
                }
                obj.put("planningPoints", planningPointsArray);

                JSONArray planningCoefficientsArray = new JSONArray();
                for (double[][] xyCoeffs : planningCoefficients) {
                    JSONArray xyCoeffsArray = new JSONArray();
                    xyCoeffsArray.put(xyCoeffs[0]);
                    xyCoeffsArray.put(xyCoeffs[1]);
                    planningCoefficientsArray.put(xyCoeffsArray);
                }
                obj.put("planningCoefficients", planningCoefficientsArray);

                obj.put("motionProfile", motionProfile.toJSONObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obj.toString();
    }

    public void readJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            JSONArray waypointsArray = obj.getJSONArray("waypoints");
            waypoints = new ArrayList<>();
            if (waypointsArray == null) {
                return;
            }
            for (int i = 0; i < waypointsArray.length(); i++) {
                JSONArray waypointArray = waypointsArray.getJSONArray(i);
                double T = waypointArray.getDouble(0);
                double distance = waypointArray.getDouble(1);
                Pose newWaypoint = new Pose(waypointArray.getDouble(2), waypointArray.getDouble(3), waypointArray.getDouble(4));
                waypoints.add(new PlanningPoint(T, distance, newWaypoint));
            }

            if (waypoints.size() >= 2) {
                JSONArray coefficientsArray = obj.getJSONArray("coefficients");
                numIntervals = coefficientsArray.length();
                coefficients = new double[numIntervals][2][4];
                for (int i = 0; i < numIntervals; i++) {
                    JSONArray xyCoeffsArray = coefficientsArray.getJSONArray(i);
                    JSONArray xCa = xyCoeffsArray.getJSONArray(0);
                    JSONArray yCa = xyCoeffsArray.getJSONArray(1);
                    coefficients[i] = new double[][]{new double[]{xCa.getDouble(0), xCa.getDouble(1), xCa.getDouble(2), xCa.getDouble(3)},
                                                     new double[]{yCa.getDouble(0), yCa.getDouble(1), yCa.getDouble(2), yCa.getDouble(3)}};
                }

                JSONArray planningPointsArray = obj.getJSONArray("planningPoints");
                planningPoints = new ArrayList<>();
                for (int i = 0; i < planningPointsArray.length(); i++) {
                    JSONArray planningPointArray = planningPointsArray.getJSONArray(i);
                    double T = planningPointArray.getDouble(0);
                    double distance = planningPointArray.getDouble(1);
                    Pose newWaypoint = new Pose(planningPointArray.getDouble(2), planningPointArray.getDouble(3), planningPointArray.getDouble(4));
                    planningPoints.add(new PlanningPoint(T, distance, newWaypoint));
                }

                JSONArray planningCoefficientsArray = obj.getJSONArray("planningCoefficients");
                double L = arcDistance(waypoints.size() - 1);
                int numSegments = (int) Math.ceil(L / constants.MAX_SEGMENT_LENGTH);
                segmentLength = L / numSegments;
                planningCoefficients = new double[numIntervals][2][4];
                for (int i = 0; i < numIntervals; i++) {
                    JSONArray xyCoeffsArray = planningCoefficientsArray.getJSONArray(i);
                    JSONArray xCa = xyCoeffsArray.getJSONArray(0);
                    JSONArray yCa = xyCoeffsArray.getJSONArray(1);
                    planningCoefficients[i] = new double[][]{new double[]{xCa.getDouble(0), xCa.getDouble(1), xCa.getDouble(2), xCa.getDouble(3)},
                                                             new double[]{yCa.getDouble(0), yCa.getDouble(1), yCa.getDouble(2), yCa.getDouble(3)}};
                }

                motionProfile.fromJSON(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ///////////////////////////////////// SPLINE CALCULATIONS /////////////////////////////////////

    private void recalculate(ArrayList<PlanningPoint> planningPoints, boolean shouldReparameterize) {
        numIntervals = Math.max(0, planningPoints.size() - 1);
        double[][][] coefficients = new double[numIntervals][2][4];

        if (numIntervals == 1) {
            double dX = planningPoints.get(1).pose.x - planningPoints.get(0).pose.x;
            double dY = planningPoints.get(1).pose.y - planningPoints.get(0).pose.y;
            coefficients[0] = new double[][]{ Utils.roundArr(new double[] { 0, 0, dX, planningPoints.get(0).pose.x }, 3),
                                              Utils.roundArr(new double[] { 0, 0, dY, planningPoints.get(0).pose.y }, 3) };
        } else if (numIntervals > 1) {
            double[][] M = calculateM();
            double[] X = calculateXY(planningPoints, true);
            double[] Y = calculateXY(planningPoints, false);

            RealMatrix MMatInv = new LUDecomposition(MatrixUtils.createRealMatrix(M)).getSolver().getInverse();
            RealMatrix XMat = MatrixUtils.createColumnRealMatrix(X);
            RealMatrix YMat = MatrixUtils.createColumnRealMatrix(Y);

            double[] xCoeffs = Utils.roundArr(MMatInv.multiply(XMat).getColumn(0), 3);
            double[] yCoeffs = Utils.roundArr(MMatInv.multiply(YMat).getColumn(0), 3);
            for (int i = 0; i < numIntervals; i++) {
                coefficients[i] = new double[][]{ Utils.spliceArr(xCoeffs, i * 4, i * 4 + 4),
                                                  Utils.spliceArr(yCoeffs, i * 4, i * 4 + 4)};
            }
        }

        if (numIntervals >= 1) {
            if (shouldReparameterize) {
                this.coefficients = coefficients;
                for (int i = 0; i < waypoints.size(); i++) {
                    waypoints.get(i).distance = arcDistance(i);
                }
                reparameterizeByDistance();
            } else {
                this.planningCoefficients = coefficients;
                motionProfile.recreate();
            }
        }
    }

    private double[][] calculateM() {
        double[][] M = new double[][]{};

        // Goes through points
        for (int i = 0; i < numIntervals; i++) {
            double[] cStart = plugIn(i, 0);
            double[] cEnd = plugIn(i + 1, 0);
            double[][] traversion2Rows = new double[][]{Utils.pad(cStart, 4 * i, 4 * numIntervals - 4 * i - 4),
                                                        Utils.pad(cEnd, 4 * i, 4 * numIntervals - 4 * i - 4)};
            M = Utils.combineArrs(M, traversion2Rows);
        }

        // First derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            double[] c = plugIn(i, 1);
            double[] cLR = Utils.combineArrs(c, Utils.coeffArr(c, -1.0));
            double[][] firstDerivCont1Row = new double[][]{Utils.pad(cLR, 4 * (i - 1), 4 * numIntervals - 4 * (i - 1) - 8)};
            M = Utils.combineArrs(M, firstDerivCont1Row);
        }

        // Second derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            double[] c = plugIn(i, 2);
            double[] cLR = Utils.combineArrs(c, Utils.coeffArr(c, -1.0));
            double[][] secondDerivCont1Row = new double[][]{Utils.pad(cLR, 4 * (i - 1), 4 * numIntervals - 4 * (i - 1) - 8)};
            M = Utils.combineArrs(M, secondDerivCont1Row);
        }

        // Boundary condition
        double[][] startPoint0Cont = new double[][]{Utils.pad(plugIn(0, 2), 0, 4 * (numIntervals - 1))};
        double[][] endPoint0Cont = new double[][]{Utils.pad(plugIn(numIntervals + 1, 2), 4 * (numIntervals - 1), 0)};
        M = Utils.combineArrs(M, Utils.combineArrs(startPoint0Cont, endPoint0Cont));

        return M;
    }

    private double[] calculateXY(ArrayList<PlanningPoint> planningPoints, boolean isX) {
        double[] XY = new double[]{};

        // Goes through points
        for (int i = 0; i < numIntervals; i++) {
            double fStart = (isX) ? planningPoints.get(i).pose.x : planningPoints.get(i).pose.y;
            double fEnd = (isX) ? planningPoints.get(i + 1).pose.x : planningPoints.get(i + 1).pose.y;
            XY = Utils.combineArrs(XY, new double[] { fStart, fEnd });
        }

        // First derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            XY = Utils.combineArrs(XY, new double[] { 0 });
        }

        // Second derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            XY = Utils.combineArrs(XY, new double[] { 0 });
        }

        // Boundary condition
        XY = Utils.combineArrs(XY, new double[] { 0, 0 });

        return XY;
    }

    private double[] plugIn(int T, int deriv) {
        /*
         * x(t) = at^3 + bt^2 + ct + d
         * x'(t) = 3at^2 + 2bt + c
         * x''(t) = 6at + 2b
         */

        if (deriv == 0) return new double[] {Math.pow(T, 3), Math.pow(T, 2), T, 1};
        else if (deriv == 1) return new double[] {3 * Math.pow(T, 2), 2 * T, 1, 0};
        else if (deriv == 2) return new double[] {6 * T, 2, 0, 0};
        else return new double[]{};
    }

    private void calculatePlanningPoints(double L) {
        planningPoints = new ArrayList<>();
        int numSegments = (int) Math.ceil(L / constants.MAX_SEGMENT_LENGTH);
        segmentLength = L / numSegments;
        for (int i = 0; i < numSegments; i++) {
            double l = i * segmentLength;
            int tJ;
            for (tJ = 0; tJ < numIntervals; tJ++) {
                if (l >= waypoints.get(tJ).distance && l < waypoints.get(tJ + 1).distance) {
                    break;
                }
            }

            double tLeft = tJ;
            double tRight = tJ + 1;
            double tMiddle = (tLeft + tRight) / 2.0;
            double tMiddleLength = arcDistance(tMiddle);
            while (Math.abs(tMiddleLength - l) > constants.MAX_BISECTION_ERROR) {
                if (l > tMiddleLength) {
                    tLeft = tMiddle;
                } else if (l < tMiddleLength) {
                    tRight = tMiddle;
                }
                tMiddle = (tLeft + tRight) / 2.0;
                tMiddleLength = arcDistance(tMiddle);
            }
            double theta = waypoints.get(tJ).pose.theta + Utils.optimalThetaDifference(waypoints.get(tJ).pose.theta, waypoints.get(tJ + 1).pose.theta)
                           * ((l - waypoints.get(tJ).distance) / (waypoints.get(tJ + 1).distance - waypoints.get(tJ).distance));
            planningPoints.add(new PlanningPoint(tMiddle, l, theta, this));
        }
    }

    private void reparameterizeByDistance() {
        calculatePlanningPoints(arcDistance(waypoints.size() - 1));
        recalculate(planningPoints, false);
    }

    ///////////////////////// SPLINE INTERPRETATION //////////////////////////

    private double arcDistance(double T1) {
        if (T1 == 0) return 0;
        int t2 = (int) Math.floor(T1);

        double length = 0;
        for (int T = 0; T < t2; T++) {
            length += arcLengthInterval(T, T + 1);
        }
        length += arcLengthInterval(t2, T1);

        return Utils.round(length, 3);
    }

    private double arcLengthInterval(int Tstart, double Tend) {
        if (Tstart < coefficients.length) {
            String xDer = "(3*(" + coefficients[Tstart][0][0] + ")*t^2 + 2*(" + coefficients[Tstart][0][1] + ")*t + (" + coefficients[Tstart][0][2] + "))^2";
            String yDer = "(3*(" + coefficients[Tstart][1][0] + ")*t^2 + 2*(" + coefficients[Tstart][1][1] + ")*t + (" + coefficients[Tstart][1][2] + "))^2";
            Expression integral = new Expression("int(sqrt(" + xDer + " + " + yDer + "), t, " + Tstart + ", " + Tend + ")");
            return integral.calculate();
        } else {
            return 0;
        }
    }

    public int getInterval(double distance) {
        int i;
        for (i = 0; i < waypoints.size() - 1; i++) {
            if (distance >= waypoints.get(i).distance && distance < waypoints.get(i + 1).distance) {
                break;
            }
        }
        return i;
    }

    public Pose getPoseFromT(double T) {
        int interval = (int) Math.floor(T);
        double x = coefficients[interval][0][0] * Math.pow(T, 3) + coefficients[interval][0][1] * Math.pow(T, 2) + coefficients[interval][0][2] * T + coefficients[interval][0][3];
        double y = coefficients[interval][1][0] * Math.pow(T, 3) + coefficients[interval][1][1] * Math.pow(T, 2) + coefficients[interval][1][2] * T + coefficients[interval][1][3];
        double theta = 0;
        return new Pose(x, y, theta);
    }

    public Pose getPoseFromDistance(double distance) {
        int interval = getInterval(distance);
        double x = planningCoefficients[interval][0][0] * Math.pow(distance, 3) + planningCoefficients[interval][0][1] * Math.pow(distance, 2) + planningCoefficients[interval][0][2] * distance + planningCoefficients[interval][0][3];
        double y = planningCoefficients[interval][1][0] * Math.pow(distance, 3) + planningCoefficients[interval][1][1] * Math.pow(distance, 2) + planningCoefficients[interval][1][2] * distance + planningCoefficients[interval][1][3];
        double theta = waypoints.get(interval).pose.theta + Utils.optimalThetaDifference(waypoints.get(interval).pose.theta, waypoints.get(interval + 1).pose.theta)
                       * ((distance - waypoints.get(interval).distance) / (waypoints.get(interval + 1).distance - waypoints.get(interval).distance));
        return new Pose(x, y, theta);
    }

    public double[] getDerivative(double distance, int deriv) {
        deriv = Math.max(1, deriv);
        int interval = getInterval(distance);
        double x;
        double y;
        if (deriv == 1) {
            x = 3 * coefficients[interval][0][0] * Math.pow(distance, 2) + 2 * coefficients[interval][0][1] * distance + coefficients[interval][0][2];
            y = 3 * coefficients[interval][1][0] * Math.pow(distance, 2) + 2 * coefficients[interval][1][1] * distance + coefficients[interval][1][2];
        } else if (deriv == 2) {
            x = 6 * coefficients[interval][0][0] * distance + 2 * coefficients[interval][0][1];
            y = 6 * coefficients[interval][1][0] * distance + 2 * coefficients[interval][1][1];
        } else if (deriv == 3) {
            x = 6 * coefficients[interval][0][0];
            y = 6 * coefficients[interval][1][0];
        } else {
            x = 1;
            y = 0;
        }
        return new double[]{ x, y };
    }

}
