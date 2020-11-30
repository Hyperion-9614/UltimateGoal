package com.hyperion.dashboard.uiobject.fieldobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;
import com.hyperion.common.TextUtils;
import com.hyperion.dashboard.Dashboard;
import com.hyperion.net.FieldEdit;
import com.hyperion.dashboard.uiobject.Simulator;
import com.hyperion.motion.math.Pose;

import org.json.JSONArray;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class Waypoint extends FieldObject {

    public Pose pose;
    public boolean renderID;
    public boolean initSelected;
    public DisplaySpline parentSpline;

    public ImageView imgView;
    public TextField idField;
    public Text info;
    public Rectangle selectRect;

    private long startDragTime;
    private double dragDx, dragDy;

    public Waypoint(ID id, double x, double y, double theta, DisplaySpline parentSpline, boolean renderID, boolean initSelected) {
        this.id = id;
        this.pose = new Pose(x, y, theta);
        this.renderID = renderID;
        this.parentSpline = parentSpline;
        this.initSelected = initSelected;
        createDisplayGroup();
    }

    public Waypoint(ID id, Pose pose, DisplaySpline parentSpline, boolean renderID, boolean initSelected) {
        this(id, pose.x, pose.y, pose.theta, parentSpline, renderID, initSelected);
    }

    public Waypoint(ID id, JSONArray wpArr) {
        this(id, wpArr.getDouble(0), wpArr.getDouble(1), wpArr.getDouble(2), null, true, true);
    }

    public void createDisplayGroup() {
        try {
            displayGroup = new Group();
            selection = new Group();

            imgView = new ImageView(Constants.getFile("img", "waypoint.png").toURI().toURL().toString());
            imgView.setFitWidth(Constants.getDouble("dashboard.gui.sizes.waypoint"));
            imgView.setFitHeight(Constants.getDouble("dashboard.gui.sizes.waypoint"));
            displayGroup.getChildren().add(imgView);

            if (renderID) {
                ID dispID = parentSpline != null ? parentSpline.id : id;
                idField = new TextField(dispID.get(4).equals(" ") ? "" : dispID.get(4));
                idField.setStyle("-fx-background-radius: 0; -fx-text-inner-color: rgba(255, 255, 255, 1.0); -fx-background-color: rgba(0, 0, 0, 0.6); -fx-focus-color: transparent;");
                idField.setMinWidth(17);
                idField.setMaxWidth(150);

                idField.setPrefWidth(TextUtils.stringWidth(idField) + 17);
                idField.textProperty().addListener((observable, oldTextString, newTextString) ->
                    idField.setPrefWidth(TextUtils.stringWidth(idField) + 17));
                idField.setTextFormatter(new TextFormatter<>(change -> {
                    if (!change.isContentChange()) {
                        return change;
                    }
                    String txt = change.getControlNewText();
                    if (txt.contains(".")) return null;
                    return change;
                }));

                idField.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        ID oldID = parentSpline == null ? id : parentSpline.id;
                        ID newID = new ID(oldID);
                        newID.set(4, idField.getText());
                        Dashboard.editField(new FieldEdit(oldID, FieldEdit.Type.EDIT_ID, newID));
                        Dashboard.fieldPane.requestFocus();
                    }
                });
                idField.setOnMousePressed(event -> {
                    Dashboard.fieldPane.select(this);
                    idField.requestFocus();
                });

                displayGroup.getChildren().add(idField);
            }

            info = new Text();
            info.setFill(Color.WHITE);
            info.setVisible(false);
            displayGroup.getChildren().add(info);

            Color selectColor = Color.hsb(360.0 * Math.random(), 1.0, 1.0);
            selectRect = new Rectangle(Dashboard.fieldPane.robotSize, Dashboard.fieldPane.robotSize);
            selectRect.setStroke(selectColor);
            selectRect.setStrokeWidth(2);
            selectRect.setFill(new Color(selectColor.getRed(), selectColor.getGreen(), selectColor.getBlue(), 0.3));
            selection.getChildren().add(selectRect);

            setListeners();
            refreshDisplayGroup();
            if (renderID) idField.toFront();
            info.toFront();
            imgView.toFront();

            if (initSelected)
                Dashboard.fieldPane.select(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setListeners() {
        displayGroup.setOnMouseClicked((event -> {
            try {
                if (event.getButton() == MouseButton.SECONDARY) {
                    if (parentSpline != null) {
                        parentSpline.spline.waypoints.remove(parentSpline.waypoints.indexOf(this));
                        parentSpline.waypoints.remove(this);
                        parentSpline.spline.endPath();
                        removeDisplayGroup();
                        if (parentSpline.waypoints.size() == 0) {
                            Dashboard.editField(new FieldEdit(parentSpline.id, FieldEdit.Type.DELETE, "{}"));
                            Dashboard.fieldPane.select(null);
                        } else {
                            if (Dashboard.fieldPane.selectedWP != null) {
                                Dashboard.fieldPane.selectedWP.id.set(5, Math.min(Integer.parseInt(Dashboard.fieldPane.selectedWP.id.get(5)), parentSpline.spline.waypoints.size() - 1));
                            }
                            Dashboard.editField(new FieldEdit(parentSpline.id, FieldEdit.Type.EDIT_BODY, parentSpline.spline.writeJSON().toString()));
                        }
                    } else {
                        Dashboard.editField(new FieldEdit(id, FieldEdit.Type.DELETE, "{}"));
                        Dashboard.fieldPane.select(null);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        displayGroup.setOnMousePressed((event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (Dashboard.simulator.state == Simulator.State.SELECTING) {
                    if (parentSpline != null) {
                        Dashboard.simulator.simulants[0] = parentSpline;
                        Dashboard.simulator.simulants[1] = parentSpline;
                    } else {
                        if (Dashboard.simulator.isSelectingFirst) {
                            Dashboard.simulator.simulants[0] = this;
                            Dashboard.simulator.isSelectingFirst = false;
                        } else {
                            Dashboard.simulator.simulants[1] = this;
                            Dashboard.simulator.isSelectingFirst = true;
                        }
                    }
                    Dashboard.leftPane.simText.setText("Selected " + Dashboard.simulator.selectionStr());
                } else {
                    startDragTime = System.currentTimeMillis();
                    dragDx = imgView.getLayoutX() - event.getSceneX();
                    dragDy = imgView.getLayoutY() - event.getSceneY();
                    Dashboard.fieldPane.select(this);
                }
            }
        }));

        displayGroup.setOnMouseDragged((event -> {
            double newX = event.getSceneX() + dragDx;
            double newY = event.getSceneY() + dragDy;

            Rectangle rect = new Rectangle(newX, newY, Constants.getDouble("dashboard.gui.sizes.waypoint"), Constants.getDouble("dashboard.gui.sizes.waypoint"));
            Rectangle rectX = new Rectangle(newX, imgView.getLayoutY(), Constants.getDouble("dashboard.gui.sizes.waypoint"), Constants.getDouble("dashboard.gui.sizes.waypoint"));
            Rectangle rectY = new Rectangle(imgView.getLayoutX(), newY, Constants.getDouble("dashboard.gui.sizes.waypoint"), Constants.getDouble("dashboard.gui.sizes.waypoint"));
            boolean[] intersects = Dashboard.fieldPane.getWBBIntersects(rect, rectX, rectY);
            if (intersects[0]) imgView.setLayoutX(newX);
            if (intersects[1]) imgView.setLayoutY(newY);
            pose = Dashboard.fieldPane.viewToPose(imgView, Constants.getDouble("dashboard.gui.sizes.waypoint"));
            refreshDisplayGroup();
        }));

        displayGroup.setOnMouseReleased((event -> {
            try {
                if (System.currentTimeMillis() - startDragTime > Constants.getInt("dashboard.gui.dragTime") && event.getButton() == MouseButton.PRIMARY) {
                    if (parentSpline != null) {
                        parentSpline.spline.waypoints.get(parentSpline.waypoints.indexOf(this)).setPose(pose);
                        parentSpline.spline.endPath();
                        Dashboard.editField(new FieldEdit(parentSpline.id, FieldEdit.Type.EDIT_BODY, parentSpline.spline.writeJSON()));
                    } else {
                        Dashboard.editField(new FieldEdit(id, FieldEdit.Type.EDIT_BODY, new JSONArray(pose.toArray())));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public void addDisplayGroup() {
        Platform.runLater(() -> {
            if (renderID) {
                ID dispID = parentSpline != null ? parentSpline.id : id;
                idField.setText(dispID.get(4).equals(" ") ? "" : dispID.get(4));
            }
            if (parentSpline != null && parentSpline.id.sub(0, 3).equals(Dashboard.opModeID.toString())) {
                parentSpline.displayGroup.getChildren().add(displayGroup);
            } else if (id.sub(0, 3).equals(Dashboard.opModeID.toString())) {
                Dashboard.fieldPane.getChildren().add(displayGroup);
            }
            displayGroup.toFront();
        });
    }

    public void refreshDisplayGroup() {
        double[] display = Dashboard.fieldPane.poseToDisplay(pose, Constants.getDouble("dashboard.gui.sizes.waypoint"));
        imgView.relocate(display[0], display[1]);
        imgView.setRotate(display[2]);

        if (renderID) {
            ID dispID = parentSpline != null ? parentSpline.id : id;
            idField.setText(dispID.get(4).equals(" ") ? "" : dispID.get(4));
            idField.relocate(display[0] + Constants.getDouble("dashboard.gui.sizes.waypoint") + 3, display[1] - 24);
        }

        info.setText(pose.toString().replace(" | ", "\n")
                                    .replace("°", "\u00B0")
                                    .replace("θ", "\u03F4".toLowerCase()));
        info.relocate(display[0] + Constants.getDouble("dashboard.gui.sizes.waypoint") + 3, display[1] + Constants.getDouble("dashboard.gui.sizes.waypoint") - 21);

        selection.relocate(display[0] + Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0 - Dashboard.fieldPane.robotSize / 2.0,
                           display[1] + Constants.getDouble("dashboard.gui.sizes.waypoint") / 2.0 - Dashboard.fieldPane.robotSize / 2.0);
        selection.setRotate(display[2]);
    }

    public void removeDisplayGroup() {
        Platform.runLater(() -> {
            if (parentSpline != null) {
                parentSpline.displayGroup.getChildren().remove(displayGroup);
            } else {
                Dashboard.fieldPane.getChildren().remove(displayGroup);
            }
        });
    }

    public void randomizeSelectColor() {
        Color selectColor = Color.hsb(360.0 * Math.random(), 1.0, 1.0);
        selectRect.setStroke(selectColor);
        selectRect.setFill(new Color(selectColor.getRed(), selectColor.getGreen(), selectColor.getBlue(), 0.3));
    }

    public String toString() {
        return id + " " + pose;
    }

}
