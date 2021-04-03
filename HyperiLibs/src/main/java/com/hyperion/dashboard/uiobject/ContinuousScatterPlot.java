package com.hyperion.dashboard.uiobject;

import java.util.HashMap;

import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

public class ContinuousScatterPlot extends ScatterChart<Number, Number> {

    public double xOriginDelta = 0;
    public HashMap<String, XYChart.Series> seriesMap;

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
            XYChart.Series serieObj = new XYChart.Series();
            serieObj.setName(serie);
            seriesMap.put(serie, serieObj);
            getData().add(serieObj);
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

        XYChart.Series series = seriesMap.get(serie);
        series.getData().add(new XYChart.Data(x - xOriginDelta, y));
    }
}
