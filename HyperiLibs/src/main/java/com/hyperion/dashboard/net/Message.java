package com.hyperion.dashboard.net;

import org.json.JSONObject;

public class Message {

    public Event event;
    public JSONObject json;

    public Message(Event event, String jsonStr) {
        try {
            this.event = event;
            this.json = new JSONObject(jsonStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Message(Event event, JSONObject json) {
        this.event = event;
        this.json = json;
    }

    public Message(String from) {
        try {
            if (!from.isEmpty() && !from.replaceAll(" ", "").isEmpty()) {
                this.event = Enum.valueOf(Event.class, from.split(" ")[0]);
                this.json = new JSONObject(from.replaceFirst(event.toString() + " ", ""));
            } else {
                this.event = Event.NULL;
                this.json = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return event.toString() + " " + json.toString();
    }

    public enum Event {
        NULL, CONNECTED, DISCONNECTED, CONSTANTS_UPDATED, FIELD_EDITED, OPMODE_ENDED, METRICS_UPDATED
    }
}
