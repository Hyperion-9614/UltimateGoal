//
// Created by tinku on 11/22/20.
//
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <cmath>
#include <tuple>

using namespace std;
using namespace cv;

tuple<Mat, Mat> preProcess(const Mat &input) {
    Mat image = input.clone();
    Mat croppedImage = input.clone();
    return make_tuple(croppedImage, image);
}

Mat getHSVImage(const Mat &croppedImageInput) {
    Mat croppedImage = croppedImageInput.clone();
    Mat croppedImageHSVMasked;
    Mat mask;
    Scalar lowerBoundHSV = Scalar(8, 92, 77);
    Scalar upperBoundHSV = Scalar(90, 255, 255);
    cv::cvtColor(croppedImage, croppedImage, COLOR_BGR2HSV);
    cv::inRange(croppedImage, lowerBoundHSV, upperBoundHSV, mask);
    cv::bitwise_and(croppedImage, croppedImage, croppedImageHSVMasked, mask);
    cv::cvtColor(croppedImageHSVMasked, croppedImageHSVMasked, COLOR_HSV2BGR);
    return croppedImageHSVMasked;
}

Mat getYCrCbImage(const Mat &croppedImageInput) {
    Mat croppedImage = croppedImageInput.clone();
    Mat croppedImageYCrCbMasked;
    Mat mask;
    Scalar lowerBoundYCrCb = Scalar(0, 152, 64);
    Scalar upperBoundYCrCb = Scalar(255, 255, 113);
    cv::cvtColor(croppedImage, croppedImage, COLOR_BGR2YCrCb);
    cv::inRange(croppedImage, lowerBoundYCrCb, upperBoundYCrCb, mask);
    cv::bitwise_and(croppedImage, croppedImage, croppedImageYCrCbMasked, mask);
    return croppedImageYCrCbMasked;
}

std::vector<vector<Point>> getLargestContour(std::vector<vector<Point>> vec) {
    if (vec.empty()) {
        return vec;
    } else {
        double max = 0;
        int index = 0;
        for (auto &i : vec) {
            if (contourArea(i) > max) {
                max = contourArea(i);
                index++;
            }
        }
        vec.erase(vec.begin(), vec.begin() + index - 1);
        vec.erase(vec.begin() + 1, vec.end());
        return vec;
    }
}

int numberOfRings(int width, int height) {
    double aspectRatio = (double) width / height;
    if ((aspectRatio > 1.3) && (aspectRatio < 1.7)) {
        return 4;
    } else if (aspectRatio > 1.7) {
        return 1;
    } else {
        return 0;
    }
}

std::vector<vector<Point>> findContours(const Mat &input) {
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

Mat drawRectanglesImg(std::vector<vector<Point>> contours, const Mat &input) {
    Mat originalImageCropped = input.clone();
    if (contours.empty()) {
        return originalImageCropped;
    } else {
        Scalar color = Scalar(0, 0, 255);
        std::vector<Rect> boundRect(contours.size());
        std::vector<vector<Point> > contours_poly(contours.size());
        approxPolyDP(contours[0], contours_poly[0], 3, true);
        boundRect[0] = boundingRect(contours_poly[0]);
        rectangle(originalImageCropped, boundRect[0].tl(), boundRect[0].br(), color, 4);
        int rings = numberOfRings(boundRect[0].width, boundRect[0].height);
        putText(originalImageCropped, std::to_string(rings), boundRect[0].tl(),
                FONT_HERSHEY_DUPLEX, 1.0, CV_RGB(0, 255, 0), 2);
        cout << rings;
        cout << "\n";
        return (originalImageCropped);
    }
}


Mat postProcessImg(const Mat &maskedImageInput, const tuple<Mat, Mat> &images) {
    Mat maskedImage = maskedImageInput.clone();
    Mat originalImageCropped = get<0>(images).clone();
    std::vector<vector<Point>> contours = findContours(maskedImage);
    Mat rectsDrawn = drawRectanglesImg(contours, originalImageCropped);
    cvtColor(rectsDrawn, rectsDrawn, COLOR_BGR2RGB);
    return rectsDrawn;
}

