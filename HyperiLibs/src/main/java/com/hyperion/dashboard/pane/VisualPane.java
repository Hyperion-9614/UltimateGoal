package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.uiobject.ContinuousScatterPlot;
import com.hyperion.dashboard.uiobject.fieldobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.PiecewiseLineGraph;
import com.hyperion.motion.math.Piecewise;

import java.util.HashMap;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;

/**
 * Contains motion profile graph
 */

public class VisualPane extends VBox {

    public PiecewiseLineGraph splineMpGraph;
    public ContinuousScatterPlot velMotorGraph;

    public VisualPane() {
        setBackground(Background.EMPTY);
        setAlignment(Pos.TOP_CENTER);

        splineMpGraph = new PiecewiseLineGraph("Spline Motion Profile", "Distance (cm)", "Velocity (cm/s)");
        getChildren().add(splineMpGraph);

        velMotorGraph = new ContinuousScatterPlot("Velocity Motor", "Time (s)", "Velocity (rpm)");
        velMotorGraph.init("Current", "Target");
        getChildren().add(velMotorGraph);
    }

    public void updateSplineMpGraph(DisplaySpline displaySpline) {
        Platform.runLater(() -> {
            splineMpGraph.getData().clear();
            if (displaySpline != null && displaySpline.waypoints.size() >= 2) {
                HashMap<String, Piecewise> map = new HashMap<>();
                map.put("Velocity (cm/s)", displaySpline.spline.mP.transVelProfile);
                splineMpGraph.rePlot(map);
            }
        });
    }

    public void updateVelMotorGraph(double currRPM, double targetRPM) {
        Platform.runLater(() -> {
            double timeS = System.currentTimeMillis() / 1000.0;
            velMotorGraph.addData("Current", timeS, currRPM);
            velMotorGraph.addData("Target", timeS, targetRPM);
        });
    }

}
