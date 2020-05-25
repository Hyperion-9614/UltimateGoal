package com.hyperion.dashboard.net;

import com.hyperion.common.Constants;
import com.hyperion.common.TextUtils;

import org.json.JSONObject;

import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnectionNotifier;

public class BTServer extends BTEndpoint {

    public BTServer() {
        super();
    }

    @Override
    public void btInit() throws Exception {
        UUID serviceUUID = new UUID(Constants.getString("dashboard.net.serviceUUID"), false);
        String serviceURL = "btspp://localhost:" + serviceUUID.toString() + ";name=HyperionSPP";
        StreamConnectionNotifier notifier = (StreamConnectionNotifier) Connector.open(serviceURL);
        conn = notifier.acceptAndOpen();
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

    @Override
    protected void onMetricsUpdated(JSONObject json) {

    }

    @Override
    protected void onOpModeEnded(JSONObject json) {

    }

}
