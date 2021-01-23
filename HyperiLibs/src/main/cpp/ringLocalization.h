//
// Created by tinku on 1/23/21.
//

#ifndef ULTIMATEGOAL_RINGLOCALIZATION_H
#define ULTIMATEGOAL_RINGLOCALIZATION_H

tuple<Mat, double> drawRectanglesWithDistance(std::vector<vector<Point>> contours, const Mat &input,
                                              const Mat &cameraFrame);

tuple<double, Point>
distanceToRing(const int &width, const Point &centerBox, const Mat &cameraFrame);

std::vector<vector<Point>> findContoursImg(const Mat &input);

tuple<Mat, double>
postProcessImgWithDistance(const Mat &maskedImageInput, const tuple<Mat, Mat> &images,
                           const Mat &cameraFrame);

#endif //ULTIMATEGOAL_RINGLOCALIZATION_H
