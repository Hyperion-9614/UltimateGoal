package com.hyperion.dashboard.net;

import com.hyperion.common.Constants;

import java.io.*;

import javax.bluetooth.*;
import javax.microedition.io.*;

public class BTServer extends BTEndpoint {

    @Override
    public void init() {
        try {
            UUID serviceUUID = new UUID(Constants.getString("dashboard.net.serviceUUID"), false);
            String serviceURL = "btspp://localhost:" + serviceUUID.toString() + ";name=HyperionSPP";
            StreamConnectionNotifier notifier = (StreamConnectionNotifier) Connector.open(serviceURL);
            conn = notifier.acceptAndOpen();

            in = new BufferedReader(new InputStreamReader(conn.openInputStream()));
            out = new PrintWriter(new OutputStreamWriter(conn.openOutputStream()));

            startMessageHandlerThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startMessageHandlerThread() {
        msgHandler = new Thread(() -> {
            try {
                Message msg;
                while ((msg = new Message(in.readLine())).event != Message.Event.NULL) {
                    switch (msg.event) {
                        case CONNECTED:
                            break;
                        case DISCONNECTED:
                            break;
                        case CONSTANTS_UPDATED:
                            break;
                        case FIELD_EDITED:
                            break;
                        case OPMODE_ENDED:
                            break;
                        case METRICS_UPDATED:
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        msgHandler.start();
    }

}
