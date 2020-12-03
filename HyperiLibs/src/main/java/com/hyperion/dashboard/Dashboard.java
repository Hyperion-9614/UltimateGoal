package com.hyperion.dashboard;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.IOUtils;
import com.hyperion.common.MiscUtils;
import com.hyperion.net.DBSocket;
import com.hyperion.net.FieldEdit;
import com.hyperion.net.Message;
import com.hyperion.dashboard.pane.FieldPane;
import com.hyperion.dashboard.pane.LeftPane;
import com.hyperion.dashboard.pane.MenuPane;
import com.hyperion.dashboard.pane.RightPane;
import com.hyperion.dashboard.pane.VisualPane;
import com.hyperion.dashboard.uiobject.fieldobject.DisplaySpline;
import com.hyperion.dashboard.uiobject.fieldobject.FieldObject;
import com.hyperion.dashboard.uiobject.fieldobject.ObstacleObj;
import com.hyperion.dashboard.uiobject.fieldobject.PathPoint;
import com.hyperion.dashboard.uiobject.fieldobject.Robot;
import com.hyperion.dashboard.uiobject.Simulator;
import com.hyperion.dashboard.uiobject.fieldobject.Waypoint;
import com.hyperion.motion.math.RigidBody;
import com.hyperion.motion.pathplanning.Obstacle;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
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

public class Dashboard extends Application {

    public static MenuPane menuPane;
    public static FieldPane fieldPane;
    public static LeftPane leftPane;
    public static RightPane rightPane;
    public static VisualPane visualPane;
    public static Simulator simulator;
    public static DBSocket dbSocket;

    public static List<FieldObject> fieldObjects = new ArrayList<>();
    public static Map<String, String> metrics = new HashMap<>();
    public static boolean isRobotOnField;
    public static int numPathPoints;

    public static ID opModeID = new ID("auto.blue.full");
    public static boolean isBuildingPaths;
    public static String constantsSave = "";
    public static boolean isLoaded = false;

    public static void main(String[] args) {
        Constants.init(new File(System.getProperty("user.dir") + "/HyperiLibs/src/main/res/data/constants.json"));
        dbSocket = new DBSocket();
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
        simulator = new Simulator();
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
            isBuildingPaths = event.isShiftDown();
            if (event.getCode() == KeyCode.ESCAPE) {
                if (simulator.state == Simulator.State.SELECTING) {
                    simulator.state = Simulator.State.INACTIVE;
                    leftPane.simulate.setText("Select\nSimulants");
                    leftPane.simText.setText("");
                }
            }
        });
        scene.setOnKeyReleased(event -> isBuildingPaths = event.isShiftDown());

        if (!isLoaded) {
            loadUIFromFieldJson();
            isLoaded = true;
        }

        stage.setScene(scene);
        stage.show();
    }

    public static void readUnimetry(String json) {
        try {
            metrics = new LinkedHashMap<>();
            JSONArray dataArr = new JSONArray(json);
            for (int i = 0; i < dataArr.length(); i++) {
                JSONArray miniObj = dataArr.getJSONArray(i);
                metrics.put(miniObj.getString(0), miniObj.getString(1));
            }

            editField(new FieldEdit(new ID("robot"), isRobotOnField ? FieldEdit.Type.EDIT_BODY : FieldEdit.Type.CREATE,
                      new JSONArray(new RigidBody(metrics.get("Current")).toArray()).toString()));
            isRobotOnField = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void editField(boolean shouldSave, FieldEdit... edits) {
        try {
            JSONArray editsArr = new JSONArray();
            for (FieldEdit edit : edits) {
                switch (edit.type) {
                    case CREATE:
                        FieldObject newObj;
                        if (edit.id.contains("spline")) {
                            newObj = new DisplaySpline(edit.id, new JSONObject(edit.body));
                        } else if (edit.id.contains("waypoint")) {
                            newObj = new Waypoint(edit.id, new JSONArray(edit.body));
                        } else if (edit.id.contains("pathPoint")) {
                            newObj = new PathPoint(edit.id, new JSONArray(edit.body));
                            numPathPoints++;
                        } else if (edit.id.contains("obstacle")) {
                            if (edit.id.contains("rect")) {
                                newObj = new ObstacleObj.Rect(edit.id, new JSONObject(edit.body));
                            } else {
                                newObj = new ObstacleObj.Circle(edit.id, new JSONObject(edit.body));
                            }
                            ArrayList<Obstacle> toAddTo = (edit.id.contains("fixed") ? fieldPane.fixedObstacles : fieldPane.dynamicObstacles);
                            toAddTo.add(((ObstacleObj) newObj).obstacle);
                        } else {
                            newObj = new Robot(new ID("robot.realtime"), new JSONArray(edit.body));
                            isRobotOnField = true;
                        }

                        fieldObjects.add(newObj);
                        if (!(newObj instanceof PathPoint) || leftPane.showPathingGrid.isSelected())
                            newObj.addDisplayGroup();
                        break;
                    case EDIT_BODY:
                        for (int i = 0; i < fieldObjects.size(); i++) {
                            if (fieldObjects.get(i).id.equals(edit.id)) {
                                if (fieldObjects.get(i) instanceof Robot) {
                                    ((Robot) fieldObjects.get(i)).rB = new RigidBody(new JSONArray(edit.body));
                                    fieldObjects.get(i).refreshDisplayGroup();
                                } else if (fieldObjects.get(i) instanceof DisplaySpline) {
                                    fieldObjects.get(i).refreshDisplayGroup();
                                } else if (fieldObjects.get(i) instanceof Waypoint) {
                                    ((Waypoint) fieldObjects.get(i)).randomizeSelectColor();
                                    Dashboard.fieldPane.select((Waypoint) fieldObjects.get(i));
                                }
                                break;
                            }
                        }
                        break;
                    case EDIT_ID:
                        for (int i = 0; i < fieldObjects.size(); i++) {
                            if (fieldObjects.get(i).id.equals(edit.id)) {
                                fieldObjects.get(i).id = new ID(edit.body);
                                fieldObjects.get(i).refreshDisplayGroup();
                                if (fieldObjects.get(i) instanceof DisplaySpline) {
                                    int j = Integer.parseInt(fieldPane.selectedWP.id.get(5));
                                    fieldPane.select(((DisplaySpline) fieldObjects.get(i)).waypoints.get(j));
                                } else if (fieldObjects.get(i) instanceof Waypoint) {
                                    ((Waypoint) fieldObjects.get(i)).randomizeSelectColor();
                                    Dashboard.fieldPane.select((Waypoint) fieldObjects.get(i));
                                }
                                break;
                            }
                        }
                        break;
                    case DELETE:
                        Iterator<FieldObject> iter = fieldObjects.iterator();
                        while (iter.hasNext()) {
                            FieldObject next = iter.next();
                            if (next.id.equals(edit.id)) {
                                if (next.id.contains("pathPoint"))
                                    numPathPoints--;
                                next.removeDisplayGroup();
                                iter.remove();
                                break;
                            }
                        }
                        break;
                }

                if (!edit.id.equals("robot") && shouldSave) {
                    editsArr.put(edit.toJSONObject());
                }
            }
            if (shouldSave) {
                MiscUtils.writeFieldEditsToFieldJSON(Constants.getFile("data", "field.json"), edits);
                if (dbSocket.isValid())
                    dbSocket.sendMessage(Message.Event.FIELD_EDITED, editsArr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void editField(FieldEdit... edits) {
        editField(true, edits);
    }

    public static void loadUIFromFieldJson() {
        JSONObject fieldJSON = new JSONObject(IOUtils.readFile(Constants.getFile("data", "field.json")));
        for (String id : fieldJSON.getJSONObject("waypoints").keySet()) {
            editField(false, new FieldEdit(new ID(id), FieldEdit.Type.CREATE, fieldJSON.getJSONObject("waypoints").getJSONArray(id).toString()));
        }
        for (String id : fieldJSON.getJSONObject("splines").keySet()) {
            editField(false, new FieldEdit(new ID(id), FieldEdit.Type.CREATE, fieldJSON.getJSONObject("splines").getJSONObject(id).toString()));
        }
        for (String id : fieldJSON.getJSONObject("obstacles").keySet()) {
            JSONObject body = fieldJSON.getJSONObject("obstacles").getJSONObject(id);
            editField(false, new FieldEdit(new ID(id), FieldEdit.Type.CREATE, body.toString()));
        }

        leftPane.resetPathingGrid();
        fieldPane.select(null);
    }

}