package com.hyperion.dashboard.net;

import com.hyperion.common.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.microedition.io.StreamConnection;

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

            sendMessage(Message.Event.CONNECTED, new JSONObject());
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
                            onConnected(msg);
                            break;
                        case DISCONNECTED:
                            onDisconnected(msg);
                            break;
                        case CONSTANTS_UPDATED:
                            onConstantsUpdated(msg);
                            break;
                        case FIELD_EDITED:
                            onFieldEdited(msg);
                            break;
                        case METRICS_UPDATED:
                            onMetricsUpdated(msg);
                            break;
                        case OPMODE_ENDED:
                            onOpModeEnded(msg);
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
    protected abstract void onConnected(Message msg) throws Exception;
    protected abstract void onDisconnected(Message msg) throws Exception;
    protected abstract void onConstantsUpdated(Message msg) throws Exception;
    protected abstract void onFieldEdited(Message msg) throws Exception;
    protected abstract void onMetricsUpdated(Message msg) throws Exception;
    protected abstract void onOpModeEnded(Message msg) throws Exception;

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

    public void sendMessage(Message.Event event, String jsonStr) {
        out.println(new Message(event, localDevice.getFriendlyName(), jsonStr).toString());
    }

    public void sendMessage(Message.Event event, JSONObject json) {
        sendMessage(event, json.toString());
    }

    public void sendMessage(Message.Event event, JSONArray json) {
        sendMessage(event, json.toString());
    }

    public void sendMessage(String event, JSONObject json) {
        out.println(new Message(event, localDevice.getFriendlyName(), json).toString());
    }

    public void sendMessage(String event, JSONArray json) {
        out.println(new Message(event, localDevice.getFriendlyName(), json).toString());
    }

    public void printBTLog(String message) {
        System.out.println("[BT -- " + localDevice.getFriendlyName() + " -- " + TextUtils.getFormattedDate() + "] " + message);
    }

}
