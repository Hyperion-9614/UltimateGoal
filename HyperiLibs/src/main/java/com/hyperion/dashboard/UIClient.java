package com.hyperion.dashboard;

import com.hyperion.common.Constants;
import com.hyperion.common.Options;
import com.hyperion.common.Utils;
import com.hyperion.dashboard.pane.DisplayPane;
import com.hyperion.dashboard.pane.FieldPane;
import com.hyperion.dashboard.pane.MenuPane;
import com.hyperion.dashboard.pane.OptionsPane;
import com.hyperion.dashboard.pane.TextPane;
import com.hyperion.dashboard.uiobj.DisplaySpline;
import com.hyperion.dashboard.uiobj.Waypoint;
import com.hyperion.motion.trajectory.SplineTrajectory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import io.socket.client.IO;
import io.socket.client.Socket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class UIClient extends Application {

    public static MenuPane menuPane;
    public static FieldPane fieldPane;
    public static OptionsPane optionsPane;
    public static TextPane textPane;
    public static DisplayPane displayPane;

    public static Socket uiClient;
    public static Constants constants;
    public static Options options;

    public static DisplaySpline selectedSpline = null;
    public static Waypoint selectedWaypoint = null;
    public static DisplaySpline currentPath = new DisplaySpline();
    public static ArrayList<DisplaySpline> splines = new ArrayList<>();
    public static ImageView robot = new ImageView();
    public static ArrayList<Waypoint> waypoints = new ArrayList<>();
    public static HashMap<String, String> unimetry = new HashMap<>();

    public static String opModeID = "auto.blue.full";
    public static boolean isBuildingPaths;
    public static boolean isSimulating;

    public static void main(String[] args) {
        constants = new Constants(new File(System.getProperty("user.dir") + "/HyperiLibs/src/main/res/data/constants.json"));
        options = new Options(new File(constants.RES_DATA_PREFIX + "/options.json"));
        startClient();
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(primaryScreenBounds.getMinX());
        stage.setY(primaryScreenBounds.getMinY());
        stage.setWidth(primaryScreenBounds.getWidth());
        stage.setHeight(primaryScreenBounds.getHeight());
        stage.setOnCloseRequest(e -> System.exit(0));

        FlowPane all = new FlowPane();
        all.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9)");
        VBox sideStuff = new VBox();
        sideStuff.setBackground(Background.EMPTY);
        sideStuff.setSpacing(10);
        menuPane = new MenuPane(stage);
        fieldPane = new FieldPane(stage);
        textPane = new TextPane();
        optionsPane = new OptionsPane();
        displayPane = new DisplayPane();
        all.getChildren().add(menuPane);
        all.getChildren().add(fieldPane);
        FlowPane.setMargin(fieldPane, new Insets(10));
        HBox hbox = new HBox();
        hbox.setBackground(Background.EMPTY);
        hbox.setSpacing(10);
        hbox.getChildren().add(optionsPane);
        hbox.getChildren().add(textPane);
        sideStuff.getChildren().add(hbox);
        sideStuff.getChildren().add(displayPane);

        ScrollPane sp = new ScrollPane();
        sp.setMaxHeight(fieldPane.fieldSize);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        sp.widthProperty().addListener((o) -> {
            Node vp = sp.lookup(".viewport");
            vp.setStyle("-fx-background-color: transparent;");
        });
        sp.setStyle("-fx-background-color: transparent;");
        sp.setContent(sideStuff);
        all.getChildren().add(sp);
        all.getChildren().add(sideStuff);
        Scene scene = new Scene(all, 1280, 720, Color.TRANSPARENT);

        stage.setScene(scene);
        stage.show();
    }

    private static void startClient() {
        try {
            uiClient = IO.socket(constants.ADDRESS);

            uiClient.on(Socket.EVENT_CONNECT, args -> {
                Utils.printSocketLog("UI", "SERVER", "connected", options);
            }).on(Socket.EVENT_DISCONNECT, args -> {
                Utils.printSocketLog("UI", "SERVER", "disconnected", options);
            }).on("constantsUpdated", args -> {
                try {
                    Utils.printSocketLog("SERVER", "UI", "constantsUpdated", options);
                    constants.read(new JSONObject(args[0].toString()));
                    textPane.constantsDisplay.setText(constants.root.toString(4));
                    constants.write();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).on("dashboardJson", args -> {
                Utils.printSocketLog("SERVER", "UI", "dashboardJson", options);
                readDashboard(args[0].toString());
            }).on("pathFound", args -> {
                Utils.printSocketLog("SERVER", "UI", "pathFound", options);
                currentPath = new DisplaySpline(args[0].toString(), constants);
                currentPath.addDisplayGroup();
            }).on("pathCompleted", args -> {
                Utils.printSocketLog("SERVER", "UI", "pathCompleted", options);
                currentPath.removeDisplayGroup();
            }).on("opModeEnded", args -> {
                Utils.printSocketLog("SERVER", "UI", "opModeEnded", options);
                currentPath.removeDisplayGroup();
            }).on("unimetryUpdated", args -> {
                Utils.printSocketLog("SERVER", "UI", "unimetryUpdated", options);
                readUnimetry(args[0].toString());
                Platform.runLater(() -> textPane.setUnimetryDisplayText());
            });

            uiClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read in waypoints & splines from json
    public static void readDashboard(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject waypointsObject = root.getJSONObject("waypoints");
            JSONObject splinesObject = root.getJSONObject("splines");

            for (Waypoint wp : waypoints) {
                wp.removeDisplayGroup();
            }
            waypoints.clear();
            for (DisplaySpline spline : splines) {
                spline.removeDisplayGroup();
            }
            splines.clear();

            for (Iterator keys = waypointsObject.keys(); keys.hasNext();) {
                String key = keys.next().toString();
                JSONArray wpArr = waypointsObject.getJSONArray(key);
                Waypoint newWP = new Waypoint(key, wpArr.getDouble(0), wpArr.getDouble(1), wpArr.getDouble(2), constants, null, true);
                newWP.addDisplayGroup();
                if (selectedWaypoint != null && key.equals(selectedWaypoint.id)) {
                    newWP.select();
                }
                waypoints.add(newWP);
            }

            for (Iterator keys = splinesObject.keys(); keys.hasNext();) {
                String key = keys.next().toString();
                SplineTrajectory splineTrajectory = new SplineTrajectory(splinesObject.getJSONObject(key).toString(), constants);
                DisplaySpline newSpline = new DisplaySpline(key, splineTrajectory, constants);
                newSpline.addDisplayGroup();
                if (selectedSpline != null && key.equals(selectedSpline.id)) {
                    if (selectedSpline.waypoints.contains(selectedWaypoint)) {
                        newSpline.waypoints.get(selectedSpline.waypoints.indexOf(selectedWaypoint)).select();
                    }
                    newSpline.select();
                }
                splines.add(newSpline);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Write waypoints & splines to json
    public static String writeDashboard() {
        JSONObject obj = new JSONObject();
        try {
            if (waypoints != null) {
                JSONObject wpObj = new JSONObject();
                for (Waypoint wp : waypoints) {
                    if (wp != null && wp.id != null && !wp.id.isEmpty()) {
                        wpObj.put(wp.id, new JSONArray(wp.pose.toArray()));
                    }
                }
                obj.put("waypoints", wpObj);
            }

            if (splines != null) {
                JSONObject splinesObj = new JSONObject();
                for (DisplaySpline spline : splines) {
                    if (spline != null && spline.id != null && !spline.id.isEmpty()) {
                        splinesObj.put(spline.id, new JSONObject(spline.spline.writeJSON()));
                    }
                }
                obj.put("splines", splinesObj);
            }

            displayPane.updateGraphs(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj.toString();
    }

    // Send dashboard json
    public static void sendDashboard() {
        uiClient.emit("dashboardJson", writeDashboard());
        Utils.printSocketLog("UI", "SERVER", "dashboardJson", options);
    }

    // Send constants json
    public static void sendConstants() {
        uiClient.emit("constantsUpdated", constants.toJSONObject().toString());
        Utils.printSocketLog("UI", "SERVER", "constantsUpdated", options);
    }

    // Read in unimetry from json
    private static void readUnimetry(String json) {
        try {
            unimetry = new HashMap<>();
            JSONArray data = new JSONArray(json);
            for (int i = 0; i < data.length(); i++) {
                JSONArray miniArr = data.getJSONArray(i);
                unimetry.put(miniArr.getString(0), miniArr.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
