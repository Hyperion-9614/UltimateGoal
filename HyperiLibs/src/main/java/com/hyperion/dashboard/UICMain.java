package com.hyperion.dashboard;

import com.hyperion.common.Constants;
import com.hyperion.common.TextUtils;
import com.hyperion.dashboard.pane.VisualPane;
import com.hyperion.dashboard.pane.FieldPane;
import com.hyperion.dashboard.pane.MenuPane;
import com.hyperion.dashboard.pane.LeftPane;
import com.hyperion.dashboard.pane.RightPane;
import com.hyperion.dashboard.uiobject.DisplaySpline;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.dashboard.uiobject.FieldObject;
import com.hyperion.dashboard.uiobject.Robot;
import com.hyperion.dashboard.uiobject.Waypoint;
import com.hyperion.motion.math.RigidBody;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

import io.socket.client.IO;
import io.socket.client.Socket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Main UI client class
 */

public class UICMain extends Application {

    public static MenuPane menuPane;
    public static FieldPane fieldPane;
    public static LeftPane leftPane;
    public static RightPane rightPane;
    public static VisualPane visualPane;
    public static Socket uiClient;

    public static DisplaySpline selectedSpline = null;
    public static Waypoint selectedWaypoint = null;
    public static List<FieldObject> fieldObjects = new ArrayList<>();
    public static Map<String, String> metrics = new HashMap<>();
    public static boolean isRobotOnField;

    public static String opModeID = "auto.blue.full";
    public static boolean isBuildingPaths;
    public static boolean isSimulating;

    public static List<FieldEdit> queuedEdits = new ArrayList<>();
    public static String constantsSave = "";
    public static boolean hasUnsavedChanges = false;

    public static void main(String[] args) {
        Constants.init(new File(System.getProperty("user.dir") + "/HyperiLibs/src/main/res/data/constants.json"));
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
        rightPane = new RightPane();
        leftPane = new LeftPane();
        visualPane = new VisualPane();
        all.getChildren().add(menuPane);
        all.getChildren().add(fieldPane);
        FlowPane.setMargin(fieldPane, new Insets(10));
        HBox hbox = new HBox();
        hbox.setBackground(Background.EMPTY);
        hbox.setSpacing(10);
        hbox.getChildren().add(leftPane);
        hbox.getChildren().add(rightPane);
        sideStuff.getChildren().add(hbox);
        sideStuff.getChildren().add(visualPane);

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
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.S && event.isControlDown()) {
                saveDashboard();
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    private static void startClient() {
        try {
            uiClient = IO.socket("http://" + Constants.getString("dashboard.net.hostIP") + ":" + Constants.getString("dashboard.net.port"));

            uiClient.on(Socket.EVENT_CONNECT, args -> {
                TextUtils.printSocketLog("UI", "SERVER", "connected");
            }).on(Socket.EVENT_DISCONNECT, args -> {
                TextUtils.printSocketLog("UI", "SERVER", "disconnected");
            }).on("constantsUpdated", args -> {
                try {
                    TextUtils.printSocketLog("SERVER", "UI", "constantsUpdated");
                    Constants.init(new JSONObject(args[0].toString()));
                    constantsSave = args[0].toString();
                    if (rightPane != null) {
                        rightPane.setConstantsDisplayText(Constants.root.toString(4));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).on("fieldEdited", args -> {
                TextUtils.printSocketLog("SERVER", "UI", "fieldEdited");
                readFieldEdits(args[0].toString());
            }).on("opModeEnded", args -> {
                TextUtils.printSocketLog("SERVER", "UI", "opModeEnded");
                Thread deleteRobotThread = new Thread(() -> {
                    long start = System.currentTimeMillis();
                    while (true) {
                        if (System.currentTimeMillis() - start >= 5000) {
                            Iterator<FieldObject> iter = fieldObjects.iterator();
                            while (iter.hasNext()) {
                                FieldObject o = iter.next();
                                if (o.id.equals("robot")) {
                                    o.removeDisplayGroup();
                                    iter.remove();
                                    break;
                                }
                            }
                            isRobotOnField = false;
                            break;
                        }
                    }
                });
                deleteRobotThread.start();
            }).on("unimetryUpdated", args -> {
                TextUtils.printSocketLog("SERVER", "UI", "unimetryUpdated");
                readUnimetry(args[0].toString());
                Platform.runLater(() -> rightPane.setMetricsDisplayText());
            });

            uiClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read in unimetry from json
    private static void readUnimetry(String json) {
        try {
            metrics = new LinkedHashMap<>();
            JSONArray dataArr = new JSONArray(json);
            for (int i = 0; i < dataArr.length(); i++) {
                JSONArray miniObj = dataArr.getJSONArray(i);
                metrics.put(miniObj.getString(0), miniObj.getString(1));
            }

            editUI(new FieldEdit("robot", isRobotOnField ? FieldEdit.Type.EDIT_BODY : FieldEdit.Type.CREATE,
                    new JSONArray(new RigidBody(metrics.get("Current")).toArray()).toString()));
            isRobotOnField = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read field edits from json
    public static void readFieldEdits(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                editUI(new FieldEdit(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Edit UI based on FieldEdit
    public static void editUI(FieldEdit edit) {
        try {
            FieldObject newObj = null;
            if (edit.type != FieldEdit.Type.DELETE && edit.type != FieldEdit.Type.EDIT_ID) {
                if (edit.id.contains("waypoint")) {
                    newObj = new Waypoint(edit.id, new JSONArray(edit.body));
                } else if (edit.id.contains("spline")) {
                    newObj = new DisplaySpline(edit.id, new JSONObject(edit.body));
                } else {
                    newObj = new Robot(new JSONArray(edit.body));
                    isRobotOnField = true;
                }
            }
            switch (edit.type) {
                case CREATE:
                    clearAllFieldObjectsWithID(edit.id);
                    fieldObjects.add(newObj);
                    newObj.addDisplayGroup();
                    break;
                case EDIT_BODY:
                    for (int i = 0; i < fieldObjects.size(); i++) {
                        if (fieldObjects.get(i).id.equals(edit.id)) {
                            if (edit.id.equals("robot")) {
                                ((Robot) fieldObjects.get(i)).rigidBody = new RigidBody(new JSONArray(edit.body));
                                fieldObjects.get(i).refreshDisplayGroup();
                            } else {
                                fieldObjects.get(i).removeDisplayGroup();
                                fieldObjects.set(i, newObj);
                                newObj.addDisplayGroup();
                            }
                            break;
                        }
                    }
                    break;
                case EDIT_ID:
                    for (int i = 0; i < fieldObjects.size(); i++) {
                        if (fieldObjects.get(i).id.equals(edit.id)) {
                            fieldObjects.get(i).id = edit.body;
                            fieldObjects.get(i).refreshDisplayGroup();
                            break;
                        }
                    }
                    break;
                case DELETE:
                    Iterator<FieldObject> iter2 = fieldObjects.iterator();
                    while (iter2.hasNext()) {
                        FieldObject next = iter2.next();
                        if (next.id.equals(edit.id)) {
                            next.removeDisplayGroup();
                            iter2.remove();
                            break;
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Queue field edits
    public static void queueFieldEdits(FieldEdit... fieldEdits) {
        queuedEdits.addAll(Arrays.asList(fieldEdits));
        changeSaveStatus(true);
    }

    // Send queued field edits
    public static void sendQueuedFieldEdits() {
        try {
            JSONArray arr = new JSONArray();
            for (FieldEdit edit : queuedEdits) {
                arr.put(edit.toJSONObject());
            }
            queuedEdits.clear();
            uiClient.emit("fieldEdited", arr.toString());
            TextUtils.printSocketLog("UI", "SERVER", "fieldEdited");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Set save indicator
    public static void changeSaveStatus(boolean hazUnsavedChanges) {
        hasUnsavedChanges = hazUnsavedChanges;
        Platform.runLater(() -> menuPane.title.setText("Hyperion Dashboard v" + Constants.getString("dashboard.version") + (hasUnsavedChanges ? " (*)" : "")));
    }

    // Save dashboard upon ctrl + s
    public void saveDashboard() {
        try {
            if (hasUnsavedChanges) {
                String newConstants = rightPane.constantsDisplay.getText();
                if (!TextUtils.condensedEquals(newConstants, constantsSave)) {
                    uiClient.emit("constantsUpdated", newConstants);
                    TextUtils.printSocketLog("UI", "SERVER", "constantsUpdated");
                }

                sendQueuedFieldEdits();
                changeSaveStatus(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearAllFieldObjectsWithID(String id) {
        Iterator<FieldObject> iter = fieldObjects.iterator();
        while (iter.hasNext()) {
            FieldObject next = iter.next();
            if (next.id.equals(id)) {
                next.removeDisplayGroup();
                iter.remove();
            }
        }
    }

}
