package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.UIClient;
import com.hyperion.dashboard.uiobj.DisplaySpline;
import com.hyperion.motion.math.Piecewise;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DisplayPane extends VBox {

    public LineChart<Number, Number> tVdGraph;

    public DisplayPane() {
        setBackground(Background.EMPTY);
        setAlignment(Pos.TOP_CENTER);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Distance (cm)");
        yAxis.setLabel("Translational Velocity (cm/s)");

        tVdGraph = new LineChart<>(xAxis, yAxis);
        tVdGraph.setCreateSymbols(false);
        tVdGraph.setTitle("Translational Magnitudes vs. Distance");

        updateGraphs(UIClient.selectedSpline);
        getChildren().add(tVdGraph);
    }

    @SuppressWarnings("unchecked")
    public void updateGraphs(DisplaySpline displaySpline) {
        Platform.runLater(() -> {
            tVdGraph.getData().clear();
            if (displaySpline != null && displaySpline.waypoints.size() >= 2) {
                XYChart.Series velSeries = new XYChart.Series();
                velSeries.setName("Velocity (cm/s)");
                Piecewise velocityProfile = displaySpline.spline.motionProfile.translationalVelocityProfile;
                if (velocityProfile != null) {
                    for (Piecewise.Interval interval : velocityProfile.intervals) {
                        velSeries.getData().add(new XYChart.Data(interval.a, displaySpline.spline.motionProfile.getTranslationalVelocity(interval.a).magnitude));
                    }
                    Piecewise.Interval lastInterval = velocityProfile.intervals.get(velocityProfile.size() - 1);
                    velSeries.getData().add(new XYChart.Data(lastInterval.b, displaySpline.spline.motionProfile.getTranslationalVelocity(lastInterval.b).magnitude));
                    tVdGraph.getData().add(velSeries);
                }

                XYChart.Series accSeries = new XYChart.Series();
                accSeries.setName("Acceleration (cm/s\u00B2)");
                Piecewise accelerationProfile = displaySpline.spline.motionProfile.translationalAccelerationProfile;
                if (accelerationProfile != null) {
                    for (Piecewise.Interval interval : accelerationProfile.intervals) {
                        accSeries.getData().add(new XYChart.Data(interval.a, displaySpline.spline.motionProfile.getTranslationalAcceleration(interval.a).magnitude));
                    }
                    Piecewise.Interval lastInterval = accelerationProfile.intervals.get(accelerationProfile.size() - 1);
                    accSeries.getData().add(new XYChart.Data(lastInterval.b, displaySpline.spline.motionProfile.getTranslationalAcceleration(lastInterval.b).magnitude));
                    tVdGraph.getData().add(accSeries);
                }
            }
        });
    }

}
