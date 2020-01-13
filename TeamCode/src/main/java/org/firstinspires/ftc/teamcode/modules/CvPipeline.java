package org.firstinspires.ftc.teamcode.modules;

import org.firstinspires.ftc.teamcode.core.Hardware;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.apache.commons.io.FilenameUtils.getPath;
import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX;

public class CvPipeline extends OpenCvPipeline {

    private static Net net;
    public Hardware hardware;
    private static final String TAG = "OpenCV/DNN";
    public static boolean skyStoneDetected = false;

    public CvPipeline(Hardware hardware) {
        String pbFile = getPath("model.pb");
        String pbTxt = getPath("model.pbtxt");
        net = Dnn.readNetFromTensorflow(pbFile, pbTxt);
        this.hardware = hardware;
    }

    @Override
    public Mat processFrame(Mat inputFrame) {
        skyStoneDetected = false;
        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        final double IN_SCALE_FACTOR = 1;
        final double MEAN_VAL = 0;
        final double THRESHOLD = 0.85;

        // Get a new frame
        Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_RGBA2BGR);

        // Forward image through network.
        Mat blob = Dnn.blobFromImage(inputFrame, IN_SCALE_FACTOR, new Size(IN_WIDTH, IN_HEIGHT), new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL), false, false);
        net.setInput(blob);

        List<List<Double>> blobList = new ArrayList<>();

        Mat detections = net.forward();

        int cols = inputFrame.cols();
        int rows = inputFrame.rows();


        detections = detections.reshape(1, (int) detections.total() / 7);
        for (int i = 0; i < detections.rows(); ++i) {
            System.out.println(detections);
            double confidence = detections.get(i, 2)[0];

            if (confidence > THRESHOLD) {
                int classId = (int) detections.get(i, 1)[0];
                int left = (int) (detections.get(i, 3)[0] * cols);
                int top = (int) (detections.get(i, 4)[0] * rows);
                int right = (int) (detections.get(i, 5)[0] * cols);
                int bottom = (int) (detections.get(i, 6)[0] * rows);

                List<Double> list = new ArrayList<>();
                list.add(confidence);
                list.add(Double.valueOf(left));
                list.add(Double.valueOf(top));
                list.add(Double.valueOf(right));
                list.add(Double.valueOf(bottom));
                list.add(Double.valueOf(classId));

                blobList.add(list);
            }
        }

        Collections.sort(blobList, new Comparator<List<Double>>() {
            @Override
            public int compare(List<Double> a, List<Double> b) {
                return a.get(0) > b.get(0) ? 1 : -1;
            }
        });

        Collections.reverse(blobList);

        int maxIndex = blobList.size() > 6 ? 6 : blobList.size();
        int numOfSkystone = 0;

        for (int i = 0; i < 6; i++) {
            List<Double> blobStuff = blobList.get(i);
            String detectedObj = "";


            double v = blobStuff.get(5).doubleValue();
            if (v == 3.0) {
                detectedObj = "Skystone";
                numOfSkystone++;
                skyStoneDetected = true;
            } else if (v == 4.0) {
                detectedObj = "Stone";
            } else if (v == 2.0) {
                detectedObj = "Red Foundation";
            } else if (v == 1.0) {
                detectedObj = "Blue Foundation";
            } else {
                detectedObj = "Unknown";
            }

            String label = detectedObj + ": " + blobStuff.get(0);
            int[] baseLine = new int[1];
            Size labelSize = Imgproc.getTextSize(label, FONT_HERSHEY_SIMPLEX, 0.5, 1, baseLine);
            Imgproc.rectangle(inputFrame, new Point(blobStuff.get(1).intValue(), blobStuff.get(2).intValue() - labelSize.height),
                    new Point(blobStuff.get(1).intValue() + labelSize.width, blobStuff.get(2).intValue() + baseLine[0]),
                    new Scalar(255, 255, 255), Imgproc.FILLED);
            // Write class name and confidence.
            Imgproc.putText(inputFrame, label, new Point(blobStuff.get(0), blobStuff.get(2)),
                    FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 0));
        }
        return inputFrame;
    }
}