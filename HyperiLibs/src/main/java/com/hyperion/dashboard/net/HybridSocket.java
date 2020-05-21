package com.hyperion.dashboard.net;

public class HybridSocket {

    public boolean isRC;

    public HybridSocket(boolean isRC) {
        this.isRC = isRC;
    }

    public enum EventType {
        CONNECTED, DISCONNECTED, CONSTANTS_UPDATED, FIELD_EDITED, OPMODE_ENDED, METRICS_UPDATED
    }

}
