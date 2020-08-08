package com.hyperion.motion.trajectory;

import com.hyperion.common.ArrayUtils;
import com.hyperion.common.Constants;
import com.hyperion.common.MathUtils;
import com.hyperion.motion.math.Piecewise;
import com.hyperion.motion.math.Pose;
import com.hyperion.motion.math.RigidBody;

import org.apache.commons.math3.analysis.integration.IterativeLegendreGaussIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Creates a 2D parametric cubic spline curve given a set of at least 2 waypoints
 * References:
 *  (1) https://timodenk.com/blog/cubic-spline-interpolation/
 *  (3) http://www.nabla.hr/PC-ParametricEqu1.htm
 *  (2) https://people.cs.clemson.edu/~dhouse/courses/405/notes/splines.pdf
 *  (3) https://homepage.cs.uiowa.edu/~kearney/pubs/CurvesAndSurfacesArcLength.pdf
 */

public class SplineTrajectory {

    public ArrayList<RigidBody> waypoints;
    public ArrayList<RigidBody> planningPoints;
    public Piecewise tauX;
    public Piecewise tauY;
    public Piecewise distanceX;
    public Piecewise distanceY;
    public MotionProfile mP;
    public double segmentLength;
    public double length;

    public SplineTrajectory(ArrayList<Pose> waypoints) {
        this.waypoints = new ArrayList<>();
        for (Pose p : waypoints) {
            this.waypoints.add(new RigidBody(p));
        }
        mP = new MotionProfile(this);
        endPath();
    }

    public SplineTrajectory(RigidBody... rigidBodies) {
        this.waypoints = new ArrayList<>();
        for (RigidBody rb : rigidBodies) {
            this.waypoints.add(new RigidBody(rb));
        }
        mP = new MotionProfile(this);
        endPath();
    }

    public SplineTrajectory(Pose... poses) {
        this.waypoints = new ArrayList<>();
        for (Pose p : poses) {
            this.waypoints.add(new RigidBody(p));
        }
        mP = new MotionProfile(this);
        endPath();
    }

    public SplineTrajectory(JSONObject obj) {
        mP = new MotionProfile(this);
        readJSON(obj);
    }

    public void endPath() {
        if (waypoints.size() >= 2) {
            tauX = new Piecewise();
            tauY = new Piecewise();
            distanceX = new Piecewise();
            distanceY = new Piecewise();
            interpolate(waypoints, true, Constants.getBoolean("spline.verbose"));
        }
    }

    ///////////////////////////////////// I/O /////////////////////////////////////

    public JSONObject writeJSON() {
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

        return obj;
    }

    public void readJSON(JSONObject obj) {
        try {
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
                    if (i == 1) segmentLength = distance;
                    Pose newWaypoint = new Pose(planningPointArray.getDouble(2), planningPointArray.getDouble(3), planningPointArray.getDouble(4));
                    planningPoints.add(new RigidBody(T, distance, newWaypoint));
                }

                JSONObject coefficientsObj = obj.getJSONObject("coefficients");
                tauX = new Piecewise(coefficientsObj.getJSONArray("tauX"));
                tauY = new Piecewise(coefficientsObj.getJSONArray("tauY"));
                distanceX = new Piecewise(coefficientsObj.getJSONArray("distanceX"));
                distanceY = new Piecewise(coefficientsObj.getJSONArray("distanceY"));

                mP.recreate();
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
            double[][] traversion2Rows = new double[][]{ ArrayUtils.pad(cStart, 4 * i, 4 * numIntervals - 4 * i - 4),
                    ArrayUtils.pad(cEnd, 4 * i, 4 * numIntervals - 4 * i - 4) };
            M = ArrayUtils.combineArrs(M, traversion2Rows);
        }

        // First derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            double[] c = plugIn(i, 1);
            double[] cLR = ArrayUtils.combineArrs(c, ArrayUtils.coeffArr(c, -1.0));
            double[][] firstDerivCont1Row = new double[][]{ ArrayUtils.pad(cLR, 4 * (i - 1), 4 * numIntervals - 4 * (i - 1) - 8) };
            M = ArrayUtils.combineArrs(M, firstDerivCont1Row);
        }

        // Second derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            double[] c = plugIn(i, 2);
            double[] cLR = ArrayUtils.combineArrs(c, ArrayUtils.coeffArr(c, -1.0));
            double[][] secondDerivCont1Row = new double[][]{ ArrayUtils.pad(cLR, 4 * (i - 1), 4 * numIntervals - 4 * (i - 1) - 8) };
            M = ArrayUtils.combineArrs(M, secondDerivCont1Row);
        }

        // Boundary condition
        double[][] startPoint0Cont = new double[][]{ ArrayUtils.pad(plugIn(0, 2), 0, 4 * (numIntervals - 1)) };
        double[][] endPoint0Cont = new double[][]{ ArrayUtils.pad(plugIn(numIntervals + 1, 2), 4 * (numIntervals - 1), 0) };
        M = ArrayUtils.combineArrs(M, ArrayUtils.combineArrs(startPoint0Cont, endPoint0Cont));

        return M;
    }
    private double[] calculateXY(ArrayList<RigidBody> rigidBodies, int numIntervals, boolean isX) {
        double[] XY = new double[]{};

        // Goes through points
        for (int i = 0; i < numIntervals; i++) {
            double fStart = (isX) ? rigidBodies.get(i).x : rigidBodies.get(i).y;
            double fEnd = (isX) ? rigidBodies.get(i + 1).x : rigidBodies.get(i + 1).y;
            XY = ArrayUtils.combineArrs(XY, new double[] { fStart, fEnd });
        }

        // First derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            XY = ArrayUtils.combineArrs(XY, new double[] { 0 });
        }

        // Second derivative continuity
        for (int i = 1; i < numIntervals; i++) {
            XY = ArrayUtils.combineArrs(XY, new double[] { 0 });
        }

        // Boundary condition
        XY = ArrayUtils.combineArrs(XY, new double[] { 0, 0 });

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
        else return new double[] { 0, 0, 0, 0 };
    }

    private void interpolate(ArrayList<RigidBody> rigidBodies, boolean shouldReparameterize, boolean verbose) {
        if (verbose) System.out.println("* Interpolating " + rigidBodies.size() + (shouldReparameterize ? " Waypoints" : " Planning Points") + " *");
        long start = System.currentTimeMillis();

        int numIntervals = Math.max(0, rigidBodies.size() - 1);
        Piecewise pwX = (shouldReparameterize ? tauX : distanceX);
        Piecewise pwY = (shouldReparameterize ? tauY : distanceY);

        if (numIntervals == 1) {
            double dX = rigidBodies.get(1).x - rigidBodies.get(0).x;
            double dY = rigidBodies.get(1).y - rigidBodies.get(0).y;
            pwX.setInterval(0, 1, buildPolynomialExpression(ArrayUtils.roundArr(new double[] { 0, 0, dX, rigidBodies.get(0).x }, 3)));
            pwY.setInterval(0, 1, buildPolynomialExpression(ArrayUtils.roundArr(new double[] { 0, 0, dY, rigidBodies.get(0).y }, 3)));
        } else if (numIntervals > 1) {
            double[][] M = calculateM(numIntervals);
            double[] X = calculateXY(rigidBodies, numIntervals, true);
            double[] Y = calculateXY(rigidBodies, numIntervals, false);

            RealMatrix MMatInv = new LUDecomposition(MatrixUtils.createRealMatrix(M)).getSolver().getInverse();
            RealMatrix XMat = MatrixUtils.createColumnRealMatrix(X);
            RealMatrix YMat = MatrixUtils.createColumnRealMatrix(Y);

            double[] xCoeffs = ArrayUtils.roundArr(MMatInv.multiply(XMat).getColumn(0), 3);
            double[] yCoeffs = ArrayUtils.roundArr(MMatInv.multiply(YMat).getColumn(0), 3);
            for (int i = 0; i < numIntervals; i++) {
                pwX.setInterval(i, i + 1, buildPolynomialExpression(ArrayUtils.spliceArr(xCoeffs, i * 4, i * 4 + 4)));
                pwY.setInterval(i, i + 1, buildPolynomialExpression(ArrayUtils.spliceArr(yCoeffs, i * 4, i * 4 + 4)));
            }
        }

        long t1 = System.currentTimeMillis();
        if (verbose) System.out.printf((shouldReparameterize ? "Tau " : "Distance ") + "Piecewise Generation: %.3fs\n", MathUtils.round((t1 - start) / 1000.0, 3));

        if (numIntervals >= 1) {
            if (shouldReparameterize) {
                for (int i = 0; i < waypoints.size(); i++) {
                    waypoints.get(i).distance = arcDistance(i);
                    if (i == waypoints.size() - 1)
                        length = waypoints.get(i).distance;
                }
                long t2 = System.currentTimeMillis();
                if (verbose) System.out.printf("Segment Distances: %.3fs\n", MathUtils.round((t2 - t1) / 1000.0, 3));
                generatePlanningPoints(verbose);
                if (verbose) System.out.printf("Planning Points Generation: %.3fs\n", MathUtils.round((System.currentTimeMillis() - t2) / 1000.0, 3));
                interpolate(planningPoints, false, verbose);
            } else {
                mP.recreate();
                if (verbose) System.out.printf("Profile Generation: %.3fs\n", MathUtils.round((System.currentTimeMillis() - t1) / 1000.0, 3));
            }
        }

        if (shouldReparameterize && verbose) {
            System.out.printf("Total: %.3fs\n", MathUtils.round((System.currentTimeMillis() - start) / 1000.0, 3));
            System.out.printf("RARD: %.3fcm\n", getRARD());
            System.out.println();
        }
    }

    private void generatePlanningPoints(boolean verbose) {
        planningPoints = new ArrayList<>();
        planningPoints.add(new RigidBody(0, 0, waypoints.get(0).theta, this));
        double L = arcDistance(waypoints.size() - 1);
        int numSegments = (int) Math.ceil(L / Constants.getDouble("spline.segmentLength"));
        segmentLength = L / numSegments;

        if (verbose) System.out.printf("* Generating %d Planning Points *\n", numSegments - 1);
        for (int i = 1; i < numSegments; i++) {
            long start = System.currentTimeMillis();
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
            while (Math.abs(tMiddleLength - l) > Constants.getDouble("spline.maxBisectionError")) {
                if (l > tMiddleLength) {
                    tLeft = tMiddle;
                } else if (l < tMiddleLength) {
                    tRight = tMiddle;
                }
                tMiddle = (tLeft + tRight) / 2.0;
                tMiddleLength = arcDistance(tMiddle);
            }
            double theta = waypoints.get(tJ).theta + MathUtils.optThetaDiff(waypoints.get(tJ).theta, waypoints.get(tJ + 1).theta)
                           * ((l - waypoints.get(tJ).distance) / (waypoints.get(tJ + 1).distance - waypoints.get(tJ).distance));
            planningPoints.add(new RigidBody(tMiddle, l, theta, this));
            if (verbose) System.out.printf("    PP #%d: %.3fs\n", i, MathUtils.round((System.currentTimeMillis() - start) / 1000.0, 3));
        }
        planningPoints.add(new RigidBody(waypoints.size() - 1, segmentLength * numSegments, waypoints.get(waypoints.size() - 1).theta, this));
    }

    // RARD Accuracy Metric: Random Average Reparameterization Deviation
    private double getRARD() {
        Pose[] tPoses = new Pose[10];
        Pose[] dPoses = new Pose[10];
        double[] deviations = new double[10];
        Random rand = new Random();
        for (int i = 0; i < 10; i++) {
            double T = MathUtils.randInRange(rand, 0, waypoints.size() - 1);
            tPoses[i] = getTPose(T);
            dPoses[i] = getDPose(arcDistance(T));
            deviations[i] = tPoses[i].distanceTo(dPoses[i]);
        }
        return MathUtils.sum(deviations) / 10.0;
    }

    ///////////////////////// SPLINE INTERPRETATION //////////////////////////

    public double arcDistance(double T1) {
        if (T1 == 0) return 0;
        int t2 = (int) Math.floor(T1);
        double length = 0;

        UnivariateIntegrator integrator = new IterativeLegendreGaussIntegrator(
                Constants.getInt("spline.integrator.n"),
                Constants.getDouble("spline.integrator.relAcc"),
                Constants.getDouble("spline.integrator.relAcc"),
                Constants.getInt("spline.integrator.minIters"),
                Constants.getInt("spline.integrator.maxIters")
        );
        for (int T = 0; T < t2; T++) {
            length += arcLengthInterval(integrator, T, T + 1);
        }

        length += arcLengthInterval(integrator, t2, T1);
        return MathUtils.round(length, 3);
    }

    private double arcLengthInterval(UnivariateIntegrator integrator, int Tstart, double Tend) {
        if (Tstart < tauX.size() && Tend > Tstart) {
            String X = "(" + tauX.getExpressionString(Tstart, Tstart + 1, 1) + ")^2";
            String Y = "(" + tauY.getExpressionString(Tstart, Tstart + 1, 1) + ")^2";
            Function func = new Function("f(t) = sqrt(" + X + " + " + Y + ")");
            return integrator.integrate(2000, func::calculate, Tstart, Tend);
        } else {
            return 0;
        }
    }

    private int getPlanningPointInterval(double distance) {
        int i;
        for (i = 0; i < planningPoints.size() - 1; i++) {
            if (distance >= planningPoints.get(i).distance && distance < planningPoints.get(i + 1).distance) {
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
        if (T == waypoints.size())
            return waypoints.get(waypoints.size() - 1);
        return new Pose(tauX.evaluate(T, 0), tauY.evaluate(T, 0), 0);
    }

    public Pose getDPose(double distance) {
        if (distance == waypoints.get(waypoints.size() - 1).distance)
            return waypoints.get(waypoints.size() - 1);
        int interval = getPlanningPointInterval(distance);
        double theta = MathUtils.norm(planningPoints.get(interval).theta + MathUtils.optThetaDiff(planningPoints.get(interval).theta, planningPoints.get(interval + 1).theta)
                                            * ((distance - planningPoints.get(interval).distance) / (planningPoints.get(interval + 1).distance - planningPoints.get(interval).distance)));
        distance = paramDistance(distance);
        return new Pose(distanceX.evaluate(distance, 0), distanceY.evaluate(distance, 0), theta);
    }

    public double getCurvature(double distance) {
        distance = paramDistance(distance);
        double xP = distanceX.evaluate(distance, 1);
        double x2P = distanceX.evaluate(distance, 2);
        double yP = distanceY.evaluate(distance, 1);
        double y2P = distanceY.evaluate(distance, 2);
        return (xP * y2P - yP * x2P) / Math.pow(xP * xP + yP * yP, 1.5);
    }

    private String buildPolynomialExpression(double[] coeffs) {
        return "(" + coeffs[0] + ")*t^3 + (" + coeffs[1] + ")*t^2 + (" + coeffs[2] + ")*t + (" + coeffs[3] + ")";
    }

    public double totalArcLength() {
        return waypoints.get(waypoints.size() - 1).distance;
    }

}
