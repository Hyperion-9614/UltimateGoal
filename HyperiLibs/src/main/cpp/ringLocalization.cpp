//
// Created by tinku on 1/23/21.
//
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <cmath>
#include <tuple>
#include "ringStack.h"
#include "ringLocalization.h"

using namespace std;
using namespace cv;

tuple<double, Point>
distanceToRing(const int &width, const Point &centerBox, const Mat &cameraFrame) {
    double ringDim = 5 * 2.54;
    double ppi = width / ringDim;
    Point camOrigin(cameraFrame.rows / 2, cameraFrame.cols);
    return make_tuple(norm(camOrigin - centerBox) * ppi, camOrigin);
}

tuple<Mat, double> drawRectanglesWithDistance(std::vector<vector<Point>> contours, const Mat &input,
                                              const Mat &cameraFrame) {
    Mat originalImageCropped = input.clone();
    if (contours.empty()) {
        return make_tuple(originalImageCropped, 0);
    } else {
        Scalar color = Scalar(0, 0, 255);
        std::vector<Rect> boundRect(contours.size());
        std::vector<vector<Point> > contours_poly(contours.size());
        approxPolyDP(contours[0], contours_poly[0], 3, true);
        boundRect[0] = boundingRect(contours_poly[0]);
        rectangle(originalImageCropped, boundRect[0].tl(), boundRect[0].br(), color, 4);
        Point centerOfRect = (boundRect[0].br() + boundRect[0].tl()) * 0.5;
        tuple<double, Point> distanceAnalysis = distanceToRing(boundRect[0].width, centerOfRect,
                                                               cameraFrame);
        putText(originalImageCropped, std::to_string(get<0>(distanceAnalysis)) + " CM",
                boundRect[0].tl(),
                FONT_HERSHEY_DUPLEX, 1.0, CV_RGB(0, 255, 0), 2);
        line(originalImageCropped, centerOfRect, get<1>(distanceAnalysis),
             Scalar(0, 255, 0), 2, LINE_8);
        return make_tuple(originalImageCropped, get<0>(distanceAnalysis));
    }
}

std::vector<vector<Point>> findContoursImg(const Mat &input) {
    Mat maskedImage = input.clone();
    Mat thresHolded;
    std::vector<vector<Point>> contours;
    cv::GaussianBlur(maskedImage, maskedImage, Size(5, 5), 0);
    cv::cvtColor(maskedImage, maskedImage, COLOR_BGR2GRAY);
    cv::threshold(maskedImage, thresHolded, 128, 255, THRESH_BINARY | THRESH_OTSU);
    cv::findContours(thresHolded, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    contours = getLargestContour(contours);
    return contours;
}

tuple<Mat, double>
postProcessImgWithDistance(const Mat &maskedImageInput, const tuple<Mat, Mat> &images,
                           const Mat &cameraFrame) {
    Mat maskedImage = maskedImageInput.clone();
    Mat originalImageCropped = get<0>(images).clone();
    std::vector<vector<Point>> contours = findContoursImg(maskedImage);
    tuple<Mat, int> rectsDrawn = drawRectanglesWithDistance(contours, originalImageCropped,
                                                            cameraFrame);
    cvtColor(get<0>(rectsDrawn), get<0>(rectsDrawn), COLOR_BGR2RGB);
    return make_tuple(get<0>(rectsDrawn), get<1>(rectsDrawn));
}
