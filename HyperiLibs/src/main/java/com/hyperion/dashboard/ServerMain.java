package com.hyperion.dashboard;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.hyperion.common.Constants;
import com.hyperion.common.Options;
import com.hyperion.common.Utils;
import com.hyperion.dashboard.uiobject.FieldEdit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Runs dashboard server socket
 */
public class ServerMain {

    public static SocketIOServer server;
    public static ArrayList<SocketIOClient> dashboardClients = new ArrayList<>();
    public static SocketIOClient rcClient;
    public static Constants constants;
    public static Options options;

    public static void main(String[] args) {
        constants = new Constants(new File(System.getProperty("user.dir") + "/HyperiLibs/src/main/res/data/constants.json"));
        options = new Options(new File(constants.RES_DATA_PREFIX + "/options.json"));

        try {
            Configuration configuration = new Configuration();
            constants.HOST_IP = InetAddress.getLocalHost().getHostAddress();
            constants.write();
            configuration.setHostname(constants.HOST_IP);
            configuration.setPort(constants.PORT);
            server = new SocketIOServer(configuration);

            server.addConnectListener(client -> {
                String address = client.getRemoteAddress().toString()
                                 .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                                 .replace("/", "");
                String type;
                if (rcClient == null && address.equals(constants.RC_IP)) {
                    rcClient = client;
                    type = "RC";
                } else {
                    dashboardClients.add(client);
                    type = "UI";
                }
                Utils.printSocketLog(type, "SERVER", address + " connected", options);

                client.sendEvent("constantsUpdated", Utils.readDataJSON("constants", constants));
                Utils.printSocketLog("SERVER", type, "constantsUpdated", options);

                client.sendEvent("optionsUpdated", Utils.readDataJSON("options", constants));
                Utils.printSocketLog("SERVER", type, "optionsUpdated", options);

                client.sendEvent("configUpdated", Utils.readFile(new File(constants.RES_DATA_PREFIX + "/MainConfig.xml")));
                Utils.printSocketLog("SERVER", type, "configUpdated", options);

                client.sendEvent("fieldEdited", readDashboardAsFieldEdits(Utils.readDataJSON("field", constants)).toString());
                Utils.printSocketLog("SERVER", type, "fieldEdited", options);
            });

            server.addDisconnectListener(client -> {
                String address = client.getRemoteAddress().toString()
                        .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                        .replace("/", "");
                if (rcClient != null && address.equals(constants.RC_IP)) {
                    rcClient = null;
                    Utils.printSocketLog("RC", "SERVER", "disconnected", options);
                } else {
                    Utils.printSocketLog("UI", "SERVER", "disconnected", options);
                    dashboardClients.remove(client);
                }
            });

            server.addEventListener("fieldEdited", String.class, (client, data, ackRequest) -> {
                String address = client.getRemoteAddress().toString()
                        .substring(0, client.getRemoteAddress().toString().indexOf(":"))
                        .replace("/", "");
                String type = (rcClient != null && address.equals(constants.RC_IP)) ? "RC" : "UI";
                Utils.printSocketLog(type, "SERVER", "fieldEdited", options);

                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("fieldEdited", data);
                    Utils.printSocketLog("SERVER", "UI", "fieldEdited", options);
                }
                if (rcClient != null) {
                    rcClient.sendEvent("fieldEdited", data);
                    Utils.printSocketLog("SERVER", "RC", "fieldEdited", options);
                }

                writeEditsToFieldJSON(data);
            });

            server.addEventListener("opModeEnded", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("opModeEnded", data);
                    Utils.printSocketLog("SERVER", "UI", "opModeEnded", options);
                }
            });

            server.addEventListener("unimetryUpdated", String.class, (client, data, ackRequest) -> {
                Utils.printSocketLog("RC", "SERVER", "unimetryUpdated", options);

                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("unimetryUpdated", data);
                    Utils.printSocketLog("SERVER", "UI", "unimetryUpdated", options);
                }
            });

            server.addEventListener("constantsUpdated", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("constantsUpdated", data);
                    Utils.printSocketLog("SERVER", "UI", "constantsUpdated", options);
                }
                if (rcClient != null) {
                    rcClient.sendEvent("constantsUpdated", data);
                    Utils.printSocketLog("SERVER", "RC", "constantsUpdated", options);
                }

                constants.read(new JSONObject(data));
                constants.write();
            });

            server.addEventListener("configUpdated", String.class, (client, data, ackRequest) -> {
                for (SocketIOClient dashboardClient : dashboardClients) {
                    dashboardClient.sendEvent("configUpdated", data);
                    Utils.printSocketLog("SERVER", "UI", "configUpdated", options);
                }
                if (rcClient != null) {
                    rcClient.sendEvent("configUpdated", data);
                    Utils.printSocketLog("SERVER", "RC", "configUpdated", options);
                }
            });

            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read in waypoints & splines from json as JSONArray of field edits
    public static JSONArray readDashboardAsFieldEdits(String json) {
        JSONArray arr = new JSONArray();
        try {
            JSONObject root = new JSONObject(json);
            JSONObject waypointsObject = root.getJSONObject("waypoints");
            JSONObject splinesObject = root.getJSONObject("splines");

            for (Iterator keys = waypointsObject.keys(); keys.hasNext();) {
                String key = keys.next().toString();
                arr.put(new FieldEdit(key, FieldEdit.Type.CREATE, waypointsObject.getJSONArray(key).toString()).toJSONObject());
            }
            for (Iterator keys = splinesObject.keys(); keys.hasNext();) {
                String key = keys.next().toString();
                arr.put(new FieldEdit(key, FieldEdit.Type.CREATE, splinesObject.getJSONObject(key).toString()).toJSONObject());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    public static void writeEditsToFieldJSON(String json) {
        try {
            JSONObject field = new JSONObject(Utils.readDataJSON("field", constants));
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                FieldEdit edit = new FieldEdit(arr.getJSONObject(i));
                if (!edit.id.equals("robot")) {
                    String o = edit.id.contains("waypoint") ? "waypoints" : "splines";
                    JSONObject target = field.getJSONObject(o);
                    switch (edit.type) {
                        case CREATE:
                        case EDIT_BODY:
                            target.put(edit.id, (edit.id.contains("waypoint") ? new JSONArray(edit.body) : new JSONObject(edit.body)));
                            break;
                        case EDIT_ID:
                            if (edit.id.contains("waypoint")) {
                                JSONArray wpArr = target.getJSONArray(edit.id);
                                target.remove(edit.id);
                                target.put(edit.body, wpArr);
                            } else {
                                JSONObject splineObj = target.getJSONObject(edit.id);
                                target.remove(edit.id);
                                target.put(edit.body, splineObj);
                            }
                            break;
                        case DELETE:
                            target.remove(edit.id);
                            break;
                    }
                    field.put(o, target);
                }
            }
            Utils.writeDataJSON(field.toString(), "field", constants);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
