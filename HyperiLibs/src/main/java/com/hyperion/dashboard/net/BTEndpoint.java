package com.hyperion.dashboard.net;

import org.json.*;

import java.io.*;

import javax.bluetooth.*;
import javax.microedition.io.*;

public abstract class BTEndpoint {

    public LocalDevice localDevice;
    public DiscoveryAgent discoveryAgent;

    public StreamConnection conn;
    public BufferedReader in = null;
    public PrintWriter out = null;

    public Thread msgHandler;

    public BTEndpoint() {
        try {
            localDevice = LocalDevice.getLocalDevice();
            discoveryAgent = localDevice.getDiscoveryAgent();

            btInit();
            in = new BufferedReader(new InputStreamReader(conn.openInputStream()));
            out = new PrintWriter(new OutputStreamWriter(conn.openOutputStream()));
            startMessageHandlerThread();

            JSONObject cObj = new JSONObject();
            cObj.put("friendlyName", localDevice.getFriendlyName());
            sendMessage(Message.Event.CONNECTED, cObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract void btInit() throws Exception;

    public void startMessageHandlerThread() {
        msgHandler = new Thread(() -> {
            try {
                Message msg;
                while ((msg = new Message(in.readLine())).event != Message.Event.NULL) {
                    switch (msg.event) {
                        case CONNECTED:
                            onConnected(msg.json);
                            break;
                        case DISCONNECTED:
                            onDisconnected(msg.json);
                            break;
                        case CONSTANTS_UPDATED:
                            onConstantsUpdated(msg.json);
                            break;
                        case FIELD_EDITED:
                            onFieldEdited(msg.json);
                            break;
                        case METRICS_UPDATED:
                            onMetricsUpdated(msg.json);
                            break;
                        case OPMODE_ENDED:
                            onOpModeEnded(msg.json);
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        msgHandler.start();
    }

    // Event handlers
    protected abstract void onConnected(JSONObject json) throws Exception;
    protected abstract void onDisconnected(JSONObject json);
    protected abstract void onConstantsUpdated(JSONObject json);
    protected abstract void onFieldEdited(JSONObject json);
    protected abstract void onMetricsUpdated(JSONObject json);
    protected abstract void onOpModeEnded(JSONObject json);

    public void close() {
        try {
            JSONObject dcObj = new JSONObject();
            dcObj.put("friendlyName", localDevice.getFriendlyName());
            sendMessage(Message.Event.DISCONNECTED, dcObj);

            conn.close();
            in.close();
            out.close();
            if (msgHandler.isAlive() && !msgHandler.isInterrupted())
                msgHandler.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message.Event event, JSONObject json) {
        out.println(new Message(event, json).toString());
    }

    public void sendMessage(Message.Event event, String jsonStr) {
        out.println(new Message(event, jsonStr).toString());
    }

}
