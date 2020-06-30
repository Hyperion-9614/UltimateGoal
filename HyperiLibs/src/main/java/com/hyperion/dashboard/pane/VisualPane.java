package com.hyperion.dashboard.pane;

import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.uiobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.FieldObject;
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

        updateGraphs(Dashboard.fieldPane.selected);
        getChildren().add(tVdGraph);
    }

    public void updateGraphs(FieldObject fieldObject) {
        Platform.runLater(() -> {
            tVdGraph.getData().clear();
            if (fieldObject instanceof DisplaySpline && ((DisplaySpline) fieldObject).waypoints.size() >= 2) {
                HashMap<String, Piecewise> map = new HashMap<>();
                map.put("Velocity (cm/s)", ((DisplaySpline) fieldObject).spline.mP.transVelProfile);
                tVdGraph.rePlot(map);
            }
        });
    }

}
