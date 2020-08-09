package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.MiscUtils;
import com.hyperion.dashboard.net.BTEndpoint;
import com.hyperion.dashboard.net.Message;

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

        MiscUtils.writeFieldEditsToFieldJSON(hw.fieldJSON, msg.json);
        Motion.init(hw);
    }

    //////////////// UNUSED ////////////////

    @Override
    protected void onMetricsUpdated(Message msg) {

    }
    @Override
    protected void onOpModeEnded(Message msg) {

    }

}
