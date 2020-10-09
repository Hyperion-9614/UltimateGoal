package com.hyperion.common;

import static com.hyperion.common.MathUtils.*;

/**
 * Contains static methods that make array operations more convenient
 */
public class ArrayUtils {

    /**
     * Rounds all values of a double array
     * to the number of places specified
     *
     * @param  arr     the base double array
     * @param  places  the number of places to round to
     * @return         the rounded double array
     */
    public static double[] roundArr(double[] arr, int places) {
        for (int i = 0; i < arr.length; i++)
            arr[i] = round(arr[i], places);
        return arr;
    }

    /**
     * Rounds all values of a 2D double array
     * to the number of places specified
     *
     * @param  arr     the base 2D double array
     * @param  places  the number of places to round to
     * @return         the rounded 2D double array
     */
    public static double[][] roundArr(double[][] arr, int places) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = roundArr(arr[i], places);
        }
        return arr;
    }

    /**
     * Prints all values of a double array, space separated
     *
     * @param  arr  the double array
     */
    public static void printArray(double[] arr) {
        System.out.print("[ ");
        for (double d : arr) System.out.print(d + " ");
        System.out.println("]");
    }

    /**
     * Prints all values of a 2D double array, space separated
     *
     * @param  arr  the 2D double array
     */
    public static void printArray(double[][] arr) {
        System.out.print("[ ");
        for (double[] r : arr) {
            System.out.print("[ ");
            for (double c : r) {
                System.out.print(c + " ");
            }
            System.out.println("]");
        }
        System.out.print("]");
    }

    /**
     * Creates a subset of a double array
     * on indices [start, end)
     * <p>
     * Functions the same as the String#substring method
     *
     * @param  arr    the base double array
     * @param  start  the index to start the splice at
     * @param  end    the index to end the splice at
     * @return        a splice of the provided array
     */
    public static double[] spliceArr(double[] arr, int start, int end) {
        double[] spliced = new double[end - start];
        for (int i = start; i < end; i++) {
            spliced[i - start] = arr[i];
        }
        return spliced;
    }

    /**
     * Appends the values of a double array onto the end of another
     *
     * @param  a  the base double array
     * @param  b  the double array to append
     * @return    the combined double array
     */
    public static double[] combineArrs(double[] a, double[] b) {
        double[] result = new double[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /**
     * Appends the values of a generic type array onto the end of another
     *
     * @param  a  the base generic type array
     * @param  b  the generic type array to append
     * @return    the combined generic type array
     */
    public static <T> T[] combineArrs(T[] a, T[] b) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /**
     * Appends the values of a 2D double array onto the end of another
     *
     * @param  a  the base 2D double array
     * @param  b  the 2D double array to append
     * @return    the combined 2D double array
     */
    public static double[][] combineArrs(double[][] a, double[][] b) {
        double[][] result = new double[a.length + b.length][];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /**
     * Appends a double array as the new last row of a 2D double array
     *
     * @param  a  the base 2D double array
     * @param  b  the double array to append
     * @return    the combined 2D double array
     */
    public static double[][] combineArrs(double[][] a, double[] b) {
        double[][] result = new double[a.length + 1][];
        System.arraycopy(a, 0, result, 0, a.length);
        result[result.length - 1] = b;
        return result;
    }

    /**
     * Removes a row from a double array
     *
     * @param  arr       the base 2D double array
     * @param  toRemove  the row index to remove
     * @return           the 2D double array with the row at
     *                   the specified index removed
     */
    public static double[][] removeRow(double[][] arr, int toRemove) {
        double[][] newArr = new double[arr.length - 1][];
        int i = 0;
        while (i < newArr.length) {
            if (i != toRemove) {
                newArr[i] = arr[i];
                i++;
            }
        }
        return newArr;
    }

    /**
     * Replaces arr specified index row of arr 2D double
     * array with arr new double array
     *
     * @param  arr          the base 2D double array
     * @param  toEdit       the index of the row to replace
     * @param  replacement  the double array to replace the old row with
     * @return              the base 2D double array with the row
     *                      at the specified index replaced with a new
     *                      double array
     */
    public static double[][] replaceRow(double[][] arr, int toEdit, double[] replacement) {
        double[][] newArr = new double[arr.length][];
        int i = 0;
        while (i < newArr.length) {
            newArr[i] = arr[i];
            if (i == toEdit) {
                newArr[i] = replacement;
            }
            i++;
        }
        return newArr;
    }

    /**
     * Multiplies all values of a double array by a specified coefficient
     *
     * @param  arr    the base double array
     * @param  coeff  the coefficient to multiply by
     * @return        the  double array with all values
     *                multiplied by the coefficient
     */
    public static double[] coeffArr(double[] arr, double coeff) {
        double[] result = new double[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = arr[i] * coeff;
        return result;
    }

    /**
     * Adds a specified left and right
     * 0-value padding to a double array
     *
     * @param  arr       the base double array
     * @param  leftPad   the number of 0 indexes to add
     *                   to the start
     * @param  rightPad  the number of 0 indexes to add
     *                   to the end
     * @return           the double array with the specified amount
     *                   of left and right padding
     */
    public static double[] pad(double[] arr, int leftPad, int rightPad) {
        leftPad = Math.max(0, leftPad);
        rightPad = Math.max(0, rightPad);
        return combineArrs(combineArrs(new double[leftPad], arr), new double[rightPad]);
    }

    /**
     * Finds the maximum value in a double array
     *
     * @param  arr  the base double array
     * @return      the maximum value of the double array
     */
    public static double arrMax(double[] arr) {
        double max = 0;
        for (double n : arr) max = Math.max(n, max);
        return max;
    }

    /**
     * Finds the minimum value in a double array
     *
     * @param  arr  the base double array
     * @return      the minimum value in the double array
     */
    public static double arrMin(double[] arr) {
        double min = 0;
        for (double n : arr) min = Math.min(n, min);
        return min;
    }

    /**
     * Combines an indefinite number of double arrays together,
     * one after the other in index and argument order
     *
     * @param  arrs  the double arrays to combine
     * @return       a double array with all of the specified
     *               double arrays' values in order
     */
    public static double[] addArrs(double[]... arrs) {
        double[] arr0 = arrs[0];
        for (int i = 1; i < arrs.length; i++) {
            for (int j = 0; j < arr0.length; j++) {
                arr0[j] += arrs[i][j];
            }
        }
        return arr0;
    }

    /**
     * Finds the sum of all values in a double array
     *
     * @param  arr  the base double array
     * @return      the sum of all values in the double array
     */
    public static double arrSum(double[] arr) {
        double sum = 0;
        for (double d: arr) sum += d;
        return sum;
    }

}
