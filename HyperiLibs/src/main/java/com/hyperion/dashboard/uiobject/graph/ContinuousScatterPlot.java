package com.hyperion.dashboard.uiobject.graph;

import com.hyperion.motion.math.Piecewise;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

public class ContinuousScatterPlot extends ScatterChart<Number, Number> {

    public double xOriginDelta = 0;
    public HashMap<String, XYChart.Series<Number, Number>> seriesMap;

    public ContinuousScatterPlot(String title, String xLabel, String yLabel) {
        super(new NumberAxis(), new NumberAxis());
        setTitle(title);
        getXAxis().setLabel(xLabel);
        getYAxis().setLabel(yLabel);
        seriesMap = new HashMap<>();
    }

    public void init(String... series) {
        seriesMap = new HashMap<>();
        for (String serie : series) {
            XYChart.Series<Number, Number> serieObj = new XYChart.Series<>();
            serieObj.setName(serie);
            seriesMap.put(serie, serieObj);
            getData().add(serieObj);
        }

        Set<Node> nodes = lookupAll(".series");
        int i = 0;
        for (Node n : nodes) {
            Color color = Color.hsb(360.0 * ((double) i / nodes.size()), 1.0, 1.0);
            int r = (int) (255 * color.getRed());
            int g = (int) (255 * color.getGreen());
            int b = (int) (255 * color.getBlue());
            n.setStyle(String.format(Locale.US, "-fx-background-radius: 2px; -fx-background-color: %s; ", String.format("#%02x%02x%02x", r, g, b)));
            i++;
        }
    }

    public void reset() {
        xOriginDelta = 0;
        for (String key : seriesMap.keySet()) {
            seriesMap.get(key).getData().clear();
        }
    }

    public void addData(String serie, double x, double y) {
        if (xOriginDelta == 0) {
            xOriginDelta = x;
        }

        XYChart.Series<Number, Number> series = seriesMap.get(serie);
        series.getData().add(new XYChart.Data<>(x - xOriginDelta, y));
    }
}
