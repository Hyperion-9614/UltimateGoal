package com.hyperion.net;

import com.hyperion.common.Constants;
import com.hyperion.common.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class NetEP {

    public ServerSocket serverSocket;
    public Socket clientSocket;
    public BufferedReader in = null;
    public PrintWriter out = null;

    public Thread initThread;
    public Thread msgHandler;

    public Message.Sender sender;

    public NetEP() {
        initThread = new Thread(() -> {
            try {
                init();
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                startMessageHandlerThread();
                sendMessage(Message.Event.CONNECTED, new JSONObject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        if (Constants.getBoolean("dashboard.isDebugging"))
            initThread.start();
    }

    public abstract void init() throws Exception;

    private void startMessageHandlerThread() {
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
            sendMessage(Message.Event.DISCONNECTED, new JSONObject());

            if (isValid()) {
                in.close();
                out.close();
                clientSocket.close();
                serverSocket.close();
            }

            if (msgHandler != null && msgHandler.isAlive() && !msgHandler.isInterrupted())
                msgHandler.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message msg) {
        if (isValid())
            out.println(msg.toString());
        else
            netLog(LogLevel.WARN, "Socket invalid when attempting to send message: " + msg.toPrettyString(20));
    }

    public void sendMessage(Message.Event event, String jsonStr) {
        sendMessage(new Message(event, sender, jsonStr));
    }

    public void sendMessage(Message.Event event, JSONObject json) {
        sendMessage(new Message(event, sender, json.toString()));
    }

    public void sendMessage(Message.Event event, JSONArray json) {
        sendMessage(new Message(event, sender, json.toString()));
    }

    public void netLog(LogLevel lvl, String message) {
        System.out.println("[NET." + lvl.toString() + " -- " + sender + " -- " + TextUtils.getFormattedDate() + "] " + message);
    }

    public boolean isValid() {
        return Constants.getBoolean("dashboard.isDebugging")
                && serverSocket != null && !serverSocket.isClosed()
                && clientSocket != null && !clientSocket.isClosed()
                && in != null && out != null;
    }

    public enum LogLevel {
        INFO, WARN, ERROR
    }

}
