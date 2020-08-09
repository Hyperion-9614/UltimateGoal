package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.uiobject.fieldobject.DisplaySpline;
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
        yAxis.setLabel("Translational Velocity (cm/s)");

        tVdGraph = new PiecewiseLineGraph(xAxis, yAxis);
        tVdGraph.setCreateSymbols(false);
        tVdGraph.setTitle("Translational Velocity vs. Distance");

        getChildren().add(tVdGraph);
    }

    public void updateGraphs(DisplaySpline displaySpline) {
        Platform.runLater(() -> {
            tVdGraph.getData().clear();
            if (displaySpline.waypoints.size() >= 2) {
                HashMap<String, Piecewise> map = new HashMap<>();
                map.put("Velocity (cm/s)", displaySpline.spline.mP.transVelProfile);
                tVdGraph.rePlot(map);
            }
        });
    }

}
