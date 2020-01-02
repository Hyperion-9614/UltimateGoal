package org.firstinspires.ftc.teamcode.modules;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import org.firstinspires.ftc.teamcode.core.Hardware;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * SUPER COOL, ADVANCED, OPENCV DNN NEURAL NETWORK!!!
 */

public class CvPipeline extends OpenCvPipeline {

    public Hardware hardware;
    private static final String TAG = "OpenCV/DNN";

    public CvPipeline(Hardware hardware) {
//        Net net;
//        String pbFile = getPath("model.pb", this);
//        String pbTxt = getPath("model.pbtxt", this);
//        net = Dnn.readNetFromTensorflow(pbFile, pbTxt);
        this.hardware = hardware;
       // return net;
    }

    @Override
    public Mat processFrame(Mat inputFrame) {
//        final int IN_WIDTH = 300;
//        final int IN_HEIGHT = 300;
//        final float WH_RATIO = (float) IN_WIDTH / IN_HEIGHT;
//        final double IN_SCALE_FACTOR = 1;
//        final double MEAN_VAL = 0;
//        final double THRESHOLD = 0.85;
//        // Get a new frame
//        Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_RGBA2RGB);
//        // Forward image through network.
//        Mat blob = Dnn.blobFromImage(inputFrame, IN_SCALE_FACTOR,
//                new Size(IN_WIDTH, IN_HEIGHT),
//                new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), /*swapRB*/true, /*crop*/false);
//        net.setInput(blob);
//        Mat detections = net.forward();
//        int cols = inputFrame.cols();
//        int rows = inputFrame.rows();
//        detections = detections.reshape(1, (int) detections.total() / 7);
//        for (int i = 0; i < detections.rows(); ++i) {
//            double confidence = detections.get(i, 2)[0];
//            if (confidence > THRESHOLD) {
//                int classId = (int) detections.get(i, 1)[0];
//                int left = (int) (detections.get(i, 3)[0] * cols);
//                int top = (int) (detections.get(i, 4)[0] * rows);
//                int right = (int) (detections.get(i, 5)[0] * cols);
//                int bottom = (int) (detections.get(i, 6)[0] * rows);
//                // Draw rectangle around detected object.
//                Imgproc.rectangle(inputFrame, new Point(left, top), new Point(right, bottom),
//                        new Scalar(0, 255, 0));
//                String label = classNames[classId] + ": " + confidence;
//                int[] baseLine = new int[1];
//                Size labelSize = Imgproc.getTextSize(label, Core.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);
//                // Draw background for label.
//                Imgproc.rectangle(inputFrame, new Point(left, top - labelSize.height),
//                        new Point(left + labelSize.width, top + baseLine[0]),
//                        new Scalar(255, 255, 255), Imgproc.FILLED);
//                // Write class name and confidence.
//                Imgproc.putText(inputFrame, label, new Point(left, top),
//                        Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0));
//            }

            return inputFrame;
        }


    }


//    public static String getPath (String file, Context context){
//        AssetManager assetManager = context.getAssets();
//        BufferedInputStream inputStream = null;
//        try {
//            // Read data from assets.
//            inputStream = new BufferedInputStream(assetManager.open(file));
//            byte[] data = new byte[inputStream.available()];
//            inputStream.read(data);
//            inputStream.close();
//            // Create copy file in storage.
//            File outFile = new File(context.getFilesDir(), file);
//            FileOutputStream os = new FileOutputStream(outFile);
//            os.write(data);
//            os.close();
//            // Return a path to file which may be read in common way.
//            return outFile.getAbsolutePath();
//        } catch (IOException ex) {
//            Log.i(TAG, "Failed to upload a file");
//        }
//        return "";
//    }
//}
