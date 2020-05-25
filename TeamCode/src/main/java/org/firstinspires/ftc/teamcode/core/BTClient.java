package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.IOUtils;
import com.hyperion.dashboard.net.BTEndpoint;
import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.dashboard.net.Message;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

public class BTClient extends BTEndpoint {

    public Hardware hw;

    public BTClient(Hardware hw) {
        super();
        this.hw = hw;
    }

    @Override
    public void btInit() throws Exception {
        UUID uuid = new UUID(Constants.getString("net.serviceUUID"), false);
        String serviceURL = discoveryAgent.selectService(uuid, ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
        conn = (StreamConnection) Connector.open(serviceURL);
    }

    @Override
    protected void onConnected(Message msg) throws Exception {
        printBTLog("Connected to device \"" + msg.sender + "\"");
    }

    @Override
    protected void onDisconnected(Message msg) throws Exception {
        printBTLog("Disconnected from device \"" + msg.sender + "\"");
    }

    @Override
    protected void onConstantsUpdated(Message msg) throws Exception {
        printBTLog("Constants updated from device \"" + msg.sender + "\"");

        Constants.init(new JSONObject(msg.json));
        Constants.write();
        hw.initFiles();
    }

    @Override
    protected void onFieldEdited(Message msg) {
        printBTLog("Field edited from device \"" + msg.sender + "\"");

        writeFieldEditsToFieldJSON(msg.json);
    }

    //////////////// UNUSED ////////////////

    @Override
    protected void onMetricsUpdated(Message msg) {

    }
    @Override
    protected void onOpModeEnded(Message msg) {

    }

    //////////////// MISCELLANEOUS ////////////////

    public void writeFieldEditsToFieldJSON(String json) {
        try {
            JSONObject field = new JSONObject(IOUtils.readFile(hw.fieldJSON));
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                FieldEdit edit = new FieldEdit(arr.getJSONObject(i));
                if (!edit.id.equals("robot")) {
                    String o = edit.id.contains("waypoint") ? "waypoints" : "splines";
                    JSONObject target = field.getJSONObject(o);
                    switch (edit.type) {
                        case CREATE:
                        case EDIT_BODY:
                            target.put(edit.id, o.equals("waypoints") ? new JSONArray(edit.body) : new JSONObject(edit.body));
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
            IOUtils.writeFile(field.toString(), hw.fieldJSON);
        } catch (Exception e) {
            e.printStackTrace();
        }
        hw.initFiles();
    }

}
