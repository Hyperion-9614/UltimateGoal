package com.hyperion.common;

import static com.hyperion.common.MathUtils.*;

public class ArrayUtils {

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

    public static double[] combineArrs(double[] a, double[] b) {
        double[] result = new double[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static <T> T[] combineArrs(T[] a, T[] b) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
    public static double[][] combineArrs(double[][] a, double[][] b) {
        double[][] result = new double[a.length + b.length][];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static double[][] combineArrs(double[][] a, double[] b) {
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

    public static double[] addArrs(double[]... arrs) {
        double[] arr0 = arrs[0];
        for (int i = 1; i < arrs.length; i++) {
            for (int j = 0; j < arr0.length; j++) {
                arr0[j] += arrs[i][j];
            }
        }
        return arr0;
    }

}
