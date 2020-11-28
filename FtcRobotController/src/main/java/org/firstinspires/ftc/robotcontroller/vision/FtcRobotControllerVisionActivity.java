package org.firstinspires.ftc.robotcontroller.vision;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.qualcomm.ftcrobotcontroller.R;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

@SuppressWarnings("ALL")
public class FtcRobotControllerVisionActivity extends FtcRobotControllerActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "myTag";
    @SuppressLint("StaticFieldLeak")
    public static FtcRobotControllerVisionActivity instance;
    public static int rings;
    JavaCameraView javaCameraView;
    Mat mRgba;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    System.loadLibrary("native-lib");
                    javaCameraView.enableView();
                    break;
                default:
                    Log.d(TAG, "callback: could not load OpenCV");
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        rings = Vision.ringStack(mRgba.getNativeObjAddr());
        Log.d(TAG, "in onCameraFrame!");
        return mRgba;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Thing is null: " + (findViewById(R.id.java_camera_view) == null));
        javaCameraView = findViewById(R.id.java_camera_view);

        javaCameraView.setCameraPermissionGranted();

        javaCameraView.setVisibility(View.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            boolean success = OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
            if (success) {
                Log.d(TAG, "asynchronous initialization succeeded!");
            } else {
                Log.d(TAG, "asynchronous initialization failed!");
            }
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}