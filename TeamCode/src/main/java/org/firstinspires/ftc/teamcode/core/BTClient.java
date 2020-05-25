package org.firstinspires.ftc.teamcode.core;

import com.hyperion.common.Constants;
import com.hyperion.common.TextUtils;
import com.hyperion.dashboard.net.BTEndpoint;

import org.json.JSONObject;

import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

public class BTClient extends BTEndpoint {

    public BTClient() {
        super();
    }

    @Override
    public void btInit() throws Exception {
        UUID uuid = new UUID(Constants.getString("net.serviceUUID"), false);
        String serviceURL = discoveryAgent.selectService(uuid, ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
        conn = (StreamConnection) Connector.open(serviceURL);
    }

    @Override
    protected void onConnected(JSONObject json) throws Exception {
        TextUtils.printBTLog("Connected to device \"" + json.getString("friendlyName") + "\"");
    }

    @Override
    protected void onDisconnected(JSONObject json) {

    }

    @Override
    protected void onConstantsUpdated(JSONObject json) {

    }

    @Override
    protected void onFieldEdited(JSONObject json) {

    }

    //////////////// UNUSED ////////////////

    @Override
    protected void onMetricsUpdated(JSONObject json) {

    }
    @Override
    protected void onOpModeEnded(JSONObject json) {

    }

}
