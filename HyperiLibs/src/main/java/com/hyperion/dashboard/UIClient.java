package com.hyperion.dashboard;

import com.hyperion.common.Constants;
import com.hyperion.common.Options;
import com.hyperion.common.Utils;
import com.hyperion.dashboard.pane.DisplayPane;
import com.hyperion.dashboard.pane.FieldPane;
import com.hyperion.dashboard.pane.MenuPane;
import com.hyperion.dashboard.pane.OptionsPane;
import com.hyperion.dashboard.pane.TextPane;
import com.hyperion.dashboard.uiobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.FieldEdit;
import com.hyperion.dashboard.uiobject.FieldObject;
import com.hyperion.dashboard.uiobject.Robot;
import com.hyperion.dashboard.uiobject.Waypoint;
import com.hyperion.motion.math.RigidBody;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.socket.client.IO;
import io.socket.client.Socket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
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
    public static ArrayList<FieldObject> fieldObjects = new ArrayList<>();
    public static HashMap<String, String> unimetry = new HashMap<>();
    public static String config = "";
    public static boolean isRobotOnField;

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
                    if (textPane != null) {
                        textPane.setConstantsDisplayText(constants.root.toString(4));
                    }
                    constants.write();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).on("fieldEdited", args -> {
                Utils.printSocketLog("SERVER", "UI", "fieldEdited", options);
                readFieldEdits(args[0].toString());
            }).on("pathFound", args -> {
                Utils.printSocketLog("SERVER", "UI", "pathFound", options);
                try {
                    currentPath = new DisplaySpline(new JSONObject(args[0].toString()), constants);
                    currentPath.addDisplayGroup();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).on("pathCompleted", args -> {
                Utils.printSocketLog("SERVER", "UI", "pathCompleted", options);
                if (currentPath != null) {
                    currentPath.removeDisplayGroup();
                    currentPath = null;
                }
                readFieldEdits(args[0].toString());
            }).on("opModeEnded", args -> {
                Utils.printSocketLog("SERVER", "UI", "opModeEnded", options);
                if (currentPath != null) {
                    currentPath.removeDisplayGroup();
                    currentPath = null;
                }
                readFieldEdits(args[0].toString());
            }).on("unimetryUpdated", args -> {
                Utils.printSocketLog("SERVER", "UI", "unimetryUpdated", options);
                readUnimetry(args[0].toString());
                Platform.runLater(() -> textPane.setUnimetryDisplayText());
            }).on("configUpdated", args -> {
                Utils.printSocketLog("SERVER", "UI", "configUpdated", options);
                config = args[0].toString();
                if (optionsPane != null) {
                    optionsPane.setConfigDisplayText();
                }
                Utils.writeFile(config, new File(constants.RES_DATA_PREFIX + "/MainConfig.xml"));
            });

            uiClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read field edits from json
    public static void readFieldEdits(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                FieldEdit fieldEdit = new FieldEdit(arr.getJSONObject(i));
                editUI(fieldEdit);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Edit UI based on FieldEdit
    public static void editUI(FieldEdit edit) {
        try {
            FieldObject newObj = null;
            if (edit.type != FieldEdit.Type.DELETE) {
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
                    Iterator<FieldObject> iter = fieldObjects.iterator();
                    while (iter.hasNext()) {
                        FieldObject next = iter.next();
                        if (next.id.equals(edit.id)) {
                            next.removeDisplayGroup();
                            iter.remove();
                            break;
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Send field edit
    public static void sendFieldEdits(FieldEdit... fieldEdits) {
        try {
            JSONArray arr = new JSONArray();
            for (FieldEdit edit : fieldEdits) {
                arr.put(edit.toJSONObject());
            }
            uiClient.emit("fieldEdited", arr.toString());
            Utils.printSocketLog("UI", "SERVER", "fieldEdited", options);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            JSONObject dataObj = new JSONObject(json);
            for (int i = 0; i < dataObj.length(); i++) {
                JSONObject miniObj = dataObj.getJSONObject("" + i);
                unimetry.put(i + " " + miniObj.getString("token0"), miniObj.getString("token1"));
            }

            FieldEdit robotEdit = new FieldEdit("robot", FieldEdit.Type.EDIT_BODY, "");
            if (!isRobotOnField) {
                robotEdit.type = FieldEdit.Type.CREATE;
            }
            editUI(robotEdit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
