package org.firstinspires.ftc.teamcode.modules;

import org.firstinspires.ftc.teamcode.core.Hardware;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * SUPER COOL, ADVANCED, OPENCV DNN NEURAL NETWORK!!!
 */

public class CvPipeline extends OpenCvPipeline {

    public Rect skystone = null;
    public Hardware hardware;

    public CvPipeline(Hardware hardware) {
        this.hardware = hardware;
    }

    @Override
    public Mat processFrame(Mat toProc) {
        Imgproc.cvtColor(toProc, toProc, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(toProc, toProc, hardware.constants.BLACK_THRESHOLD, 0, Imgproc.THRESH_TOZERO);
        Imgproc.threshold(toProc, toProc, hardware.constants.BLACK_THRESHOLD, 255, Imgproc.THRESH_BINARY);
        Mat roi = toProc.clone();

        Scalar black = new Scalar(0);
        Scalar green = new Scalar(0, 255, 0);

        Core.inRange(toProc, black, black, roi);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(roi, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        RotatedRect mostLikelySkystone = null;
        for (MatOfPoint contour : contours) {
            MatOfPoint2f areaPoints = new MatOfPoint2f(contour.toArray());
            RotatedRect boundingRect = Imgproc.minAreaRect(areaPoints);

            double rectangleArea = boundingRect.size.area();
            double viewportArea = 1280 * 720;
            if ((mostLikelySkystone == null || rectangleArea > mostLikelySkystone.size.area())
                && rectangleArea < viewportArea / 2 && rectangleArea > viewportArea / 18) {
                mostLikelySkystone = boundingRect;
            }
        }

        if (mostLikelySkystone != null) {
            Point[] rotatedRectPoints = new Point[4];
            mostLikelySkystone.points(rotatedRectPoints);
            Rect blackRect = Imgproc.boundingRect(new MatOfPoint(rotatedRectPoints));
            Imgproc.cvtColor(toProc, toProc, Imgproc.COLOR_GRAY2RGB);
            Imgproc.rectangle(toProc, blackRect.tl(), blackRect.br(), green, 3);
            skystone = blackRect.clone();
        } else {
            skystone = null;
        }

        return toProc;
    }

}
