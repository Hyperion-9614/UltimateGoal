//
// Created by tinku on 11/22/20.
//

#ifndef HYPERION_VISION_H
#define HYPERION_VISION_H

#include <jni.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <cmath>
#include <tuple>

using namespace cv;
using namespace std;

tuple<Mat, Mat> preProcess(const Mat &input);

Mat getHSVImage(const Mat &croppedImageInput);

Mat getYCrCbImage(const Mat &croppedImageInput);

std::vector<vector<Point>> getLargestContour(std::vector<vector<Point>> vec);

Mat drawRectanglesImg(std::vector<vector<Point>> contours, const Mat &input);

int numberOfRings(int width, int height);

Mat postProcessImg(const Mat &maskedImageInput, const tuple<Mat, Mat> &images);

#endif //HYPERION_VISION_H
