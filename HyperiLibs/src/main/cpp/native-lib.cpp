#include "opencv2/core.hpp"
#include <jni.h>
#include <opencv2/imgproc.hpp>
#include "ringStack.h"
#include "ringLocalization.h"

using namespace cv;
using namespace std;

extern "C"
JNIEXPORT int JNICALL
Java_org_firstinspires_ftc_robotcontroller_vision_Vision_ringStack(JNIEnv *env, jclass type,
                                                                   jlong addrRgba) {
    Mat &img = *(Mat *) addrRgba;
    cvtColor(img, img, COLOR_RGB2BGR);
    tuple<Mat, Mat> images = preProcess(img);
    Mat imageHSVMasked = getHSVImage(get<0>(images));
    Mat imageYCrCbMasked = getYCrCbImage(imageHSVMasked);
    tuple<Mat, int> outputs = postProcessImg(imageYCrCbMasked, images);
    img = get<0>(outputs);
    return get<1>(outputs);
}

extern "C"
JNIEXPORT double JNICALL
Java_org_firstinspires_ftc_robotcontroller_vision_Vision_ringLocalization(JNIEnv *env, jclass type,
                                                                          jlong addrRgba) {
    Mat &img = *(Mat *) addrRgba;
    cvtColor(img, img, COLOR_RGB2BGR);
    tuple<Mat, Mat> images = preProcess(img);
    Mat imageHSVMasked = getHSVImage(get<0>(images));
    Mat imageYCrCbMasked = getYCrCbImage(imageHSVMasked);
    tuple<Mat, double> outputs = postProcessImgWithDistance(imageYCrCbMasked, images, img);
    img = get<0>(outputs);
    return get<1>(outputs);
}