package com.hyperion.dashboard.pane;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.dashboard.uiobject.graph.ContinuousScatterPlot;
import com.hyperion.dashboard.uiobject.fieldobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.graph.PiecewiseLineGraph;
import com.hyperion.motion.math.Piecewise;
import com.hyperion.net.Message;

import org.json.JSONObject;

import java.util.HashMap;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

/**
 * Contains motion profile graph
 */

public class VisualPane extends VBox {

    public double width;

    public PiecewiseLineGraph splineMpGraph;
    public ContinuousScatterPlot velMotorGraph;
    public final ComboBox<String> velMotorSelector;
    public ObservableList<String> velMotors = FXCollections.observableArrayList();

    public SpinnerValueFactory.DoubleSpinnerValueFactory kPFac = new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10, 0, 0.05);
    public SpinnerValueFactory.DoubleSpinnerValueFactory kIFac = new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10, 0, 0.05);
    public SpinnerValueFactory.DoubleSpinnerValueFactory kDFac = new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10, 0, 0.05);
    public SpinnerValueFactory.DoubleSpinnerValueFactory kFFac = new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10, 0, 0.05);

    public VisualPane() {
        setBackground(Background.EMPTY);
        setSpacing(10);
        setAlignment(Pos.TOP_CENTER);
        width = (Screen.getPrimary().getVisualBounds().getWidth() - Dashboard.fieldPane.fieldSize) - 100;

        splineMpGraph = new PiecewiseLineGraph("Spline Motion Profile", "Distance (cm)", "Velocity (cm/s)");
        getChildren().add(splineMpGraph);

        velMotorGraph = new ContinuousScatterPlot("Velocity Motor", "Time (s)", "Velocity (rpm)");
        velMotorGraph.init("Current", "Target");
        getChildren().add(velMotorGraph);

        velMotorSelector = new ComboBox<>(velMotors);
        velMotorSelector.setStyle("-fx-font: 24px \"Arial\"; -fx-focus-color: transparent;");
        velMotorSelector.setPrefSize(width / 2 + 10, 60);
        velMotorSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            velMotorGraph.reset();
            JSONObject kMotor = Constants.getJSONObject(new ID("pid", newValue).toString());
            kPFac.setValue(kMotor.getDouble("kP"));
            kIFac.setValue(kMotor.getDouble("kI"));
            kDFac.setValue(kMotor.getDouble("kD"));
            kFFac.setValue(kMotor.getDouble("kF"));
        });
        getChildren().add(velMotorSelector);

        Label P = new Label("P:");
        P.setTextFill(Color.WHITE);
        P.setStyle("-fx-font: 16px \"Arial\";");
        Spinner<Double> kP = new Spinner<>();
        kP.setPrefWidth(60);
        kP.setValueFactory(kPFac);
        kP.valueProperty().addListener((observable, oldValue, newValue) -> editkPIDF("kP", newValue));

        Label I = new Label("I:");
        I.setTextFill(Color.WHITE);
        I.setStyle("-fx-font: 16px \"Arial\";");
        Spinner<Double> kI = new Spinner<>();
        kI.setPrefWidth(60);
        kI.setValueFactory(kIFac);
        kI.valueProperty().addListener((observable, oldValue, newValue) -> editkPIDF("kI", newValue));

        Label D = new Label("D:");
        D.setTextFill(Color.WHITE);
        D.setStyle("-fx-font: 16px \"Arial\";");
        Spinner<Double> kD = new Spinner<>();
        kD.setPrefWidth(60);
        kD.setValueFactory(kDFac);
        kD.valueProperty().addListener((observable, oldValue, newValue) -> editkPIDF("kD", newValue));

        Label F = new Label("F:");
        F.setTextFill(Color.WHITE);
        F.setStyle("-fx-font: 16px \"Arial\";");
        Spinner<Double> kF = new Spinner<>();
        kF.setPrefWidth(60);
        kF.setValueFactory(kFFac);
        kF.valueProperty().addListener((observable, oldValue, newValue) -> editkPIDF("kF", newValue));

        HBox pidf = new HBox();
        pidf.setPrefWidth(width / 2 + 10);
        pidf.setAlignment(Pos.CENTER);
        pidf.setSpacing(10);
        pidf.getChildren().addAll(P, kP, I, kI, D, kD, F, kF);

        FlowPane velMotorFlowPane = new FlowPane();
        velMotorFlowPane.setHgap(10);
        velMotorFlowPane.setVgap(10);
        velMotorFlowPane.getChildren().addAll(velMotorSelector, pidf);
        getChildren().add(velMotorFlowPane);
    }

    public void editkPIDF(String k, double val) {
        Constants.setAtID(new ID("pid", velMotorSelector.getValue(), k).toString(), val, true);
        Dashboard.leftPane.constantsSave = Constants.root.toString(4);
        Dashboard.leftPane.setConstantsDisplayText(Dashboard.leftPane.constantsSave);
        Dashboard.dbSocket.sendMessage(Message.Event.CONSTANTS_UPDATED, Dashboard.leftPane.constantsSave);
    }

    public void updateSplineMpGraph(DisplaySpline displaySpline) {
        Platform.runLater(() -> {
            splineMpGraph.getData().clear();
            if (displaySpline != null && displaySpline.waypoints.size() >= 2) {
                HashMap<String, Piecewise> map = new HashMap<>();
                map.put("Translational Velocity", displaySpline.spline.mP.transVelProfile);
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
