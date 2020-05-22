package com.hyperion.dashboard.net;

import java.io.BufferedReader;
import java.io.PrintWriter;

import javax.microedition.io.StreamConnection;

public abstract class BTEndpoint {

    public StreamConnection conn;
    public BufferedReader in = null;
    public PrintWriter out = null;
    public Thread msgHandler;

    public abstract void init();
    public abstract void startMessageHandlerThread();

    public void close() {
        try {
            conn.close();
            in.close();
            out.close();
            if (msgHandler.isAlive() && !msgHandler.isInterrupted())
                msgHandler.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message) {
        out.println(message.toString());
    }

}
