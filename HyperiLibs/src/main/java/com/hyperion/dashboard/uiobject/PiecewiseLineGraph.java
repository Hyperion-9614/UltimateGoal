package com.hyperion.dashboard.uiobject;

import com.hyperion.motion.math.Piecewise;

import java.util.HashMap;
import java.util.Locale;

import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;

public class PiecewiseLineGraph extends LineChart<Number, Number> {

    public PiecewiseLineGraph(Axis<Number> numberAxis, Axis<Number> numberAxis2) {
        super(numberAxis, numberAxis2);
    }

    public void rePlot(HashMap<String, Piecewise> map) {
        getData().clear();
        setStyle(getColorStyle(map));

        for (String key : map.keySet()) {
            Piecewise piecewise = map.get(key);
            for (int j = 0; j < piecewise.size(); j++) {
                Piecewise.Interval interval = piecewise.intervals.get(j);
                Series<Number, Number> intervalSeries = new Series<>();
                if (j == 0) intervalSeries.setName(key);
                intervalSeries.getData().add(new XYChart.Data<>(interval.a, piecewise.evaluate(interval.a, 0)));
                intervalSeries.getData().add(new XYChart.Data<>(interval.b, piecewise.evaluate(interval.b, 0)));
                getData().add(intervalSeries);
            }
        }
    }

    private String getColorStyle(HashMap<String, Piecewise> map) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int iSoFar = 1;
        for (String key : map.keySet()) {
            for (int j = 0; j < map.get(key).size(); j++) {
                Color color = Color.hsb(360.0 * ((double) i / map.size()), 1.0, 1.0);
                int r = (int) (255 * color.getRed());
                int g = (int) (255 * color.getGreen());
                int b = (int) (255 * color.getBlue());
                sb.append(String.format(Locale.US, "CHART_COLOR_%d: %s; ", iSoFar, String.format("#%02x%02x%02x", r, g, b)));
                iSoFar++;
            }
            i++;
        }
        return sb.toString();
    }
}
