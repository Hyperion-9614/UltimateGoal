package com.hyperion.motion.math;

import com.hyperion.common.MathUtils;

import org.json.JSONArray;

/**
 * Orientation object for global positioning, pathing, etc.
 */

public class Pose {

    public double x;
    public double y;
    public double theta = 0;

    /**
     * Default constructor
     */
    public Pose() {

    }

    /**
     * Constructor to create a pose manually from an (x, y) position
     *
     * @param  x  an x value
     * @param  y  a y value
     */
    public Pose(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constructor to create a pose manually from
     * an (x, y) position and a theta (in radians)
     *
     * @param  x      an x value
     * @param  y      a y value
     * @param  theta  a theta (in radians)
     */
    public Pose(double x, double y, double theta) {
        this.x = x;
        this.y = y;
        this.theta = theta;
    }

    /**
     * Constructor for a shallow copy of another pose
     *
     * @param  copyFrom  the pose to create a shallow copy from
     */
    public Pose(Pose copyFrom) {
        this.x = copyFrom.x;
        this.y = copyFrom.y;
        this.theta = copyFrom.theta;
    }

    /**
     * Constructor to create a pose from a 2 or 3 length double[]
     *
     * @param  arr  the double[] to create a pose from
     */
    public Pose(double[] arr) {
        this.x = arr[0];
        this.y = arr[1];
        if (arr.length == 3)
            this.theta = arr[2];
    }

    /**
     * Constructor to create a pose from a JSONArray (import poses from files)
     *
     * @param  arr  the JSONArray to create a pose from
     */
    public Pose(JSONArray arr) {
        this(arr.getDouble(0), arr.getDouble(1), arr.getDouble(2));
    }

    /**
     * Helper method to overwrite (x, y) position
     *
     * @param  x  an x value
     * @param  y  a y value
     */
    public void setXY(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Helper method to overwrite the theta value
     *
     * @param  theta  a theta (in radians)
     */
    public void setTheta(double theta) {
        this.theta = theta;
    }

    /**
     * Helper method to overwrite (x, y) position
     * and theta value simultaneously
     *
     * @param  x      an x value
     * @param  y      a y value
     * @param  theta  a theta (in radians)
     */
    public void setXYTheta(double x, double y, double theta) {
        setXY(x, y);
        setTheta(theta);
    }

    /**
     * Helper method to add to the (x, y) position
     *
     * @param  dX  a change in x value
     * @param  dY  a change in y value
     */
    public void addXY(double dX, double dY) {
        this.x += dX;
        this.y += dY;
    }

    /**
     * Helper method to add to the (x, y) position and theta value
     *
     * @param  dX  a change in x value
     * @param  dY  a change in y value
     * @param  dTheta  a change in theta value
     */
    public void addXYT(double dX, double dY, double dTheta) {
        this.x += dX;
        this.y += dY;
        this.theta += dTheta;
    }

    /**
     * Calculates the distance between this and
     * another pose's (x, y) positions
     * using the distance formula
     *
     * @param  o1  the other pose to calculate the distance to
     * @return     the distance between this and the other pose's (x, y) positions
     */
    public double distanceTo(Pose o1) {
        return Math.sqrt(Math.pow(this.x - o1.x, 2) + Math.pow(this.y - o1.y, 2));
    }

    /**
     * Overwrites this pose's x, y, and theta with another pose's x, y, and theta
     *
     * @param  newPose  the other pose to overwrite this pose's x, y, and theta values with
     */
    public void setPose(Pose newPose) {
        this.x = newPose.x;
        this.y = newPose.y;
        this.theta = newPose.theta;
    }

    /**
     * Creates a new pose using the (x, y) position
     * of this pose and a provided theta value
     *
     * @param  newTheta  the theta value to use for the new pose
     * @return           a new pose with the (x, y) position of this pose and the provided theta value
     */
    public Pose thetaed(double newTheta) {
        return new Pose(x, y, newTheta);
    }

    /**
     * Creates a new pose by adding the dX and dY values of a vector
     * to this pose's (x, y) position
     *
     * @param  vec  the vector to add to this pose's position
     * @return      a new pose with:
     *                  x = this pose's x + the provided vector's dX
     *                  y = this pose's y + the provided vector's dY
     */
    public Pose addVector(Vector2D vec) {
        return new Pose(this.x + vec.x, this.y + vec.y, this.theta);
    }

    /**
     * Converts this pose to a double[]
     * with the following index mapping:
     * <p>
     *      0    1    2
     *      x    y  theta
     *
     * @return  a double[] representation of this pose
     */
    public double[] toArray() {
        return new double[] { x, y, theta};
    }

    /**
     * Converts this pose to a JSONArray
     * with the following index mapping:
     * <p>
     *      0    1    2
     *      x    y  theta
     *
     * @return  a JSONArray representation of this pose
     */
    public JSONArray toJSONArray() {
        return new JSONArray(toArray());
    }

    /**
     * Checks if the x, y, and theta values of this pose are equal
     * to those of a provided pose
     *
     * @param  other  the other pose to check value equality with
     * @return        true:  the values of this pose are equal to
     *                       those of the provided pose
     *                false: the values of this pose are NOT equal
     *                       to those of the provided pose
     */
    public boolean equals(Pose other) {
        return x == other.x && y == other.y && theta == other.theta;
    }

    /**
     * Creates a formatted, readable text (String)
     * representation of this pose as such:
     * <p>
     *      X: {x value} | Y: {y value} | {theta character}: {theta in degrees}{degree symbol}
     * <p>
     * in which numerical values are rounded to three decimal places
     *
     * @return  a String representation of this pose
     */
    @Override
    public String toString() {
        return "X: " + MathUtils.round(x, 3) + " | Y: " + MathUtils.round(y, 3)
                + " | " + "\u03F4".toLowerCase() + ": " + MathUtils.round(Math.toDegrees(theta), 3) + "°";
    }

}
