package com.hyperion.net;

import org.json.JSONArray;
import org.json.JSONObject;

public class Message {

    public Event event;
    public Sender sender;
    public String json;

    public Message(Event event, Sender sender, String jsonStr) {
        try {
            this.event = event;
            this.sender = sender;
            this.json = jsonStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Message(Event event, Sender sender, JSONObject json) {
        this.event = event;
        this.sender = sender;
        this.json = json.toString();
    }

    public Message(Event event, Sender sender, JSONArray json) {
        this.event = event;
        this.sender = sender;
        this.json = json.toString();
    }

    public Message(String event, Sender sender, JSONObject json) {
        this(Enum.valueOf(Event.class, event.split(" ")[0]), sender, json);
    }

    public Message(String event, Sender sender, JSONArray json) {
        this(Enum.valueOf(Event.class, event.split(" ")[0]), sender, json);
    }

    public Message(String from) {
        try {
            if (!from.isEmpty() && !from.replaceAll(" ", "").isEmpty()) {
                this.event = Enum.valueOf(Event.class, from.split(" ")[0]);
                this.sender = Enum.valueOf(Sender.class, from.split(" ")[1]);
                this.json = from.replaceFirst(event.toString() + " " + sender.toString() + " ", "");
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
        return event.toString() + " " + sender.toString() + " " + json;
    }

    public enum Event {
        NULL, CONNECTED, DISCONNECTED, CONSTANTS_UPDATED, FIELD_EDITED, OPMODE_ENDED, METRICS_UPDATED
    }

    public enum Sender {
        DASHBOARD, RC
    }

}
