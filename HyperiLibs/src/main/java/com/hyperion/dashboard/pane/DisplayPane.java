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

public class DisplayPane extends HBox {

    public LineChart<Number, Number> translationalVelocityVsDistanceGraph;

    public DisplayPane() {
        setBackground(Background.EMPTY);
        setAlignment(Pos.TOP_CENTER);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Distance (cm)");
        yAxis.setLabel("Translational Velocity (cm/s)");

        translationalVelocityVsDistanceGraph = new LineChart<>(xAxis, yAxis);
        translationalVelocityVsDistanceGraph.setCreateSymbols(false);
        translationalVelocityVsDistanceGraph.setTitle("Translational Velocity vs. Distance");
        updateVelocityDistanceGraph(UIClient.selectedSpline);
        getChildren().add(translationalVelocityVsDistanceGraph);
    }

    @SuppressWarnings("unchecked")
    public void updateVelocityDistanceGraph(DisplaySpline displaySpline) {
        Platform.runLater(() -> {
            translationalVelocityVsDistanceGraph.getData().clear();
            if (displaySpline != null) {
                XYChart.Series series = new XYChart.Series();
                Piecewise velocityProfile = displaySpline.spline.motionProfile.translationalVelocityProfile;
                if (velocityProfile != null) {
                    for (double[] interval : velocityProfile.getIntervals()) {
                        series.getData().add(new XYChart.Data(interval[0], displaySpline.spline.motionProfile.getTranslationalVelocity(interval[0]).magnitude));
                    }
                    double lastD = velocityProfile.getIntervals()[velocityProfile.intervals.size() - 1][1];
                    series.getData().add(new XYChart.Data(lastD, displaySpline.spline.motionProfile.getTranslationalVelocity(lastD).magnitude));
                    translationalVelocityVsDistanceGraph.getData().add(series);
                }
            }
        });
    }

}
