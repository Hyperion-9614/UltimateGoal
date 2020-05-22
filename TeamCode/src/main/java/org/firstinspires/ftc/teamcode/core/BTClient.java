package org.firstinspires.ftc.teamcode.core;

import com.hyperion.dashboard.net.*;

public class BTClient extends BTEndpoint {

    @Override
    public void init() {
        try {
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
