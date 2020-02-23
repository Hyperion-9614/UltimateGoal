package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.UICMain;
import com.hyperion.dashboard.uiobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.PiecewiseLineGraph;
import com.hyperion.motion.math.Piecewise;

import java.util.HashMap;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.chart.NumberAxis;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;

/**
 * Contains motion profile graph
 */

public class VisualPane extends VBox {

    public PiecewiseLineGraph tVdGraph;

    public VisualPane() {
        setBackground(Background.EMPTY);
        setAlignment(Pos.TOP_CENTER);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Distance (cm)");
        yAxis.setLabel("Translational Magnitude (cm/s), (cm/s\u00B2)");

        tVdGraph = new PiecewiseLineGraph(xAxis, yAxis);
        tVdGraph.setCreateSymbols(false);
        tVdGraph.setTitle("Translational Magnitude vs. Distance");

        updateGraphs(UICMain.selectedSpline);
        getChildren().add(tVdGraph);
    }

    public void updateGraphs(DisplaySpline displaySpline) {
        Platform.runLater(() -> {
            tVdGraph.getData().clear();
            if (displaySpline != null && displaySpline.waypoints.size() >= 2) {
                HashMap<String, Piecewise> map = new HashMap<>();
                map.put("Velocity (cm/s)", displaySpline.spline.mP.tVelProfile);
                map.put("Acceleration (cm/s\u00B2)", displaySpline.spline.mP.tAccProfile);
                tVdGraph.rePlot(map);
            }
        });
    }

}
