package com.hyperion.motion.trajectory;

import com.hyperion.common.Constants;
import com.hyperion.common.Utils;
import com.hyperion.motion.math.Piecewise;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.math.Pose;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.Function;

import java.util.ArrayList;
import java.util.Arrays;

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
    public ArrayList<RigidBody> waypoints;
    public ArrayList<RigidBody> planningPoints;
    public Piecewise tauX = new Piecewise();
    public Piecewise tauY = new Piecewise();
    public Piecewise distanceX = new Piecewise();
    public Piecewise distanceY = new Piecewise();
    public MotionProfile motionProfile;
    public double segmentLength;
    public double length;

    public SplineTrajectory(ArrayList<RigidBody> waypoints, Constants constants) {
        this.constants = constants;
        this.waypoints = waypoints;
        motionProfile = new MotionProfile(this);
        endPath();
    }

    public SplineTrajectory(Constants constants, RigidBody... rigidBodies) {
        this.constants = constants;
        this.waypoints = new ArrayList<>(Arrays.asList(rigidBodies));
        motionProfile = new MotionProfile(this);
        endPath();
    }

    public SplineTrajectory(String json, Constants constants) {
        this.constants = constants;
        motionProfile = new MotionProfile(this);
        readJSON(json);
    }

    public void endPath() {
        if (waypoints.size() >= 2) {
            interpolate(waypoints, true);
        }
    }

    ///////////////////////////////////// I/O /////////////////////////////////////

    public String writeJSON() {
        JSONObject obj = new JSONObject();

        try {
            JSONArray waypointsArray = new JSONArray();
            for (RigidBody waypoint : waypoints) {
                waypointsArray.put(waypoint.toArray());
            }
            obj.put("waypoints", waypointsArray);

            if (waypointsArray.length() >= 2) {
                JSONArray planningPointsArray = new JSONArray();
                for (RigidBody pp : planningPoints) {
                    planningPointsArray.put(pp.toArray());
                }
                obj.put("planningPoints", planningPointsArray);

                JSONObject coefficients = new JSONObject();
                coefficients.put("tauX", tauX.toJSONArray());
                coefficients.put("tauY", tauY.toJSONArray());
                coefficients.put("distanceX", distanceX.toJSONArray());
                coefficients.put("distanceY", distanceY.toJSONArray());
                obj.put("coefficients", coefficients);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obj.toString();
    }

    public void readJSON(String json) {
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
                waypoints.add(new RigidBody(T, distance, newWaypoint));
                if (i == waypointsArray.length() - 1) length = waypoints.get(i).distance;
            }

            if (waypoints.size() >= 2) {
                JSONArray planningPointsArray = obj.getJSONArray("planningPoints");
                planningPoints = new ArrayList<>();
                for (int i = 0; i < planningPointsArray.length(); i++) {
                    JSONArray planningPointArray = planningPointsArray.getJSONArray(i);
                    double T = planningPointArray.getDouble(0);
                    double distance = planningPointArray.getDouble(1);
                    Pose newWaypoint = new Pose(planningPointArray.getDouble(2), planningPointArray.getDouble(3), planningPointArray.getDouble(4));
                    planningPoints.add(new RigidBody(T, distance, newWaypoint));
                }

                JSONObject coefficientsObj = obj.getJSONObject("coefficients");
                tauX = new Piecewise(coefficientsObj.getJSONArray("tauX"));
                tauY = new Piecewise(coefficientsObj.getJSONArray("tauY"));
                distanceX = new Piecewise(coefficientsObj.getJSONArray("distanceX"));
                distanceY = new Piecewise(coefficientsObj.getJSONArray("distanceY"));

                motionProfile.recreate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ///////////////////////////////////// SPLINE CALCULATIONS /////////////////////////////////////

    private double[][] calculateM(int numIntervals) {
        double[][] M = new double[][]{};

        // Goes through points
        for (int i = 0; i < numIntervals; i++) {
            double[] cStart = plugIn(i, 0);
            double[] cEnd = plugIn(i + 1, 0);
            double[][] traversion2Rows = new double[][]{ Utils.pad(cStart, 4 * i, 4 * numIntervals - 4 * i - 4),
                                                         Utils.pad(cEnd, 4 * i, 4 * numIntervals - 4 * i - 4) };
            M = Utils.combineArrs(M, traversion2Rows);
        }

        // First derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            double[] c = plugIn(i, 1);
            double[] cLR = Utils.combineArrs(c, Utils.coeffArr(c, -1.0));
            double[][] firstDerivCont1Row = new double[][]{ Utils.pad(cLR, 4 * (i - 1), 4 * numIntervals - 4 * (i - 1) - 8) };
            M = Utils.combineArrs(M, firstDerivCont1Row);
        }

        // Second derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            double[] c = plugIn(i, 2);
            double[] cLR = Utils.combineArrs(c, Utils.coeffArr(c, -1.0));
            double[][] secondDerivCont1Row = new double[][]{ Utils.pad(cLR, 4 * (i - 1), 4 * numIntervals - 4 * (i - 1) - 8) };
            M = Utils.combineArrs(M, secondDerivCont1Row);
        }

        // Boundary condition
        double[][] startPoint0Cont = new double[][]{ Utils.pad(plugIn(0, 2), 0, 4 * (numIntervals - 1)) };
        double[][] endPoint0Cont = new double[][]{ Utils.pad(plugIn(numIntervals + 1, 2), 4 * (numIntervals - 1), 0) };
        M = Utils.combineArrs(M, Utils.combineArrs(startPoint0Cont, endPoint0Cont));

        return M;
    }
    private double[] calculateXY(ArrayList<RigidBody> rigidBodies, int numIntervals, boolean isX) {
        double[] XY = new double[]{};

        // Goes through points
        for (int i = 0; i < numIntervals; i++) {
            double fStart = (isX) ? rigidBodies.get(i).pose.x : rigidBodies.get(i).pose.y;
            double fEnd = (isX) ? rigidBodies.get(i + 1).pose.x : rigidBodies.get(i + 1).pose.y;
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

        if (deriv == 0) return new double[] { Math.pow(T, 3), Math.pow(T, 2), T, 1 };
        else if (deriv == 1) return new double[] { 3 * Math.pow(T, 2), 2 * T, 1, 0 };
        else if (deriv == 2) return new double[] { 6 * T, 2, 0, 0 };
        else return new double[]{ 0, 0, 0, 0 };
    }

    private void interpolate(ArrayList<RigidBody> rigidBodies, boolean shouldReparameterize) {
        int numIntervals = Math.max(0, rigidBodies.size() - 1);
        Piecewise pwX = (shouldReparameterize ? tauX : distanceX);
        Piecewise pwY = (shouldReparameterize ? tauY : distanceY);

        if (numIntervals == 1) {
            double dX = rigidBodies.get(1).pose.x - rigidBodies.get(0).pose.x;
            double dY = rigidBodies.get(1).pose.y - rigidBodies.get(0).pose.y;
            pwX.setInterval(0, 1, buildPolynomialExpression(Utils.roundArr(new double[] { 0, 0, dX, rigidBodies.get(0).pose.x }, 3)));
            pwY.setInterval(0, 1, buildPolynomialExpression(Utils.roundArr(new double[] { 0, 0, dY, rigidBodies.get(0).pose.y }, 3)));
        } else if (numIntervals > 1) {
            double[][] M = calculateM(numIntervals);
            double[] X = calculateXY(rigidBodies, numIntervals, true);
            double[] Y = calculateXY(rigidBodies, numIntervals, false);

            RealMatrix MMatInv = new LUDecomposition(MatrixUtils.createRealMatrix(M)).getSolver().getInverse();
            RealMatrix XMat = MatrixUtils.createColumnRealMatrix(X);
            RealMatrix YMat = MatrixUtils.createColumnRealMatrix(Y);

            double[] xCoeffs = Utils.roundArr(MMatInv.multiply(XMat).getColumn(0), 3);
            double[] yCoeffs = Utils.roundArr(MMatInv.multiply(YMat).getColumn(0), 3);
            for (int i = 0; i < numIntervals; i++) {
                pwX.setInterval(i, i + 1, buildPolynomialExpression(Utils.spliceArr(xCoeffs, i * 4, i * 4 + 4)));
                pwY.setInterval(i, i + 1, buildPolynomialExpression(Utils.spliceArr(yCoeffs, i * 4, i * 4 + 4)));
            }
        }

        if (numIntervals >= 1) {
            if (shouldReparameterize) {
                for (int i = 0; i < waypoints.size(); i++) {
                    waypoints.get(i).distance = arcDistance(i);
                    if (i == waypoints.size() - 1) length = waypoints.get(i).distance;
                }
                calculatePlanningPoints();
                interpolate(this.planningPoints, false);
            } else {
                motionProfile.recreate();
            }
        }
    }
    private void calculatePlanningPoints() {
        planningPoints = new ArrayList<>();

        double L = arcDistance(waypoints.size() - 1);
        int numSegments = (int) Math.ceil(L / constants.MAX_SEGMENT_LENGTH);
        segmentLength = L / numSegments;
        for (int i = 0; i < numSegments; i++) {
            double l = i * segmentLength;
            int tJ;
            for (tJ = 0; tJ < tauX.size(); tJ++) {
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
            planningPoints.add(new RigidBody(tMiddle, l, theta, this));
        }
        planningPoints.add(new RigidBody(waypoints.size() - 1, segmentLength * numSegments, waypoints.get(waypoints.size() - 1).pose.theta, this));
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
        if (Tstart < tauX.size() && Tend > Tstart) {
            SimpsonIntegrator integrator = new SimpsonIntegrator();
            String X = "(" + tauX.getExpressionString(Tstart, Tstart + 1, 1) + ")^2";
            String Y = "(" + tauY.getExpressionString(Tstart, Tstart + 1, 1) + ")^2";
            Function func = new Function("f(t) = sqrt(" + X + " + " + Y + ")");
            return integrator.integrate(1000, func::calculate, Tstart, Tend);
        } else {
            return 0;
        }
    }

    private int getInterval(double distance) {
        int i;
        for (i = waypoints.size() - 2; i > 0; i--) {
            if (distance >= waypoints.get(i - 1).distance && distance < waypoints.get(i).distance) {
                break;
            }
        }
        return i;
    }
    public double paramDistance(double distance) {
        double count = 0;
        while (distance >= segmentLength) {
            distance -= segmentLength;
            count++;
        }
        return (distance / segmentLength) + count;
    }
    public Pose getTPose(double T) {
        if (T == waypoints.size()) return waypoints.get(waypoints.size() - 1).pose;
        return new Pose(tauX.evaluate(T, 0, true), tauY.evaluate(T, 0, true), 0);
    }
    public Pose getDPose(double distance) {
        if (distance == waypoints.get(waypoints.size() - 1).distance) return waypoints.get(waypoints.size() - 1).pose;
        int interval = getInterval(distance);
        double theta = Utils.normalizeTheta(waypoints.get(interval).pose.theta + Utils.optimalThetaDifference(waypoints.get(interval).pose.theta, waypoints.get(interval + 1).pose.theta)
                                                  * ((distance - waypoints.get(interval).distance) / (waypoints.get(interval + 1).distance - waypoints.get(interval).distance)), 0, 2 * Math.PI);
        distance = paramDistance(distance);
        return new Pose(distanceX.evaluate(distance, 0, true), distanceY.evaluate(distance, 0, true), theta);
    }
    private String buildPolynomialExpression(double[] coeffs) {
        return "(" + coeffs[0] + ")*t^3 + (" + coeffs[1] + ")*t^2 + (" + coeffs[2] + ")*t + (" + coeffs[3] + ")";
    }

}
