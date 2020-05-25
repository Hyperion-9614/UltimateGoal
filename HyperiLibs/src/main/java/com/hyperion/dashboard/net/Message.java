package com.hyperion.dashboard.net;

import org.json.JSONArray;
import org.json.JSONObject;

public class Message {

    public Event event;
    public String sender;
    public String json;

    public Message(Event event, String sender, String jsonStr) {
        try {
            this.event = event;
            this.json = jsonStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Message(Event event, String sender, JSONObject json) {
        this.event = event;
        this.json = json.toString();
    }

    public Message(Event event, String sender, JSONArray json) {
        this.event = event;
        this.json = json.toString();
    }

    public Message(String from) {
        try {
            if (!from.isEmpty() && !from.replaceAll(" ", "").isEmpty()) {
                this.event = Enum.valueOf(Event.class, from.split(" ")[0]);
                this.sender = from.split(" ")[1];
                this.json = from.replaceFirst(event.toString() + " " + sender + " ", "");
            } else {
                this.event = Event.NULL;
                this.sender = null;
                this.json = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return event.toString() + " " + sender + " " + json.toString();
    }

    public enum Event {
        NULL, CONNECTED, DISCONNECTED, CONSTANTS_UPDATED, FIELD_EDITED, OPMODE_ENDED, METRICS_UPDATED
    }
}
