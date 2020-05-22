package com.hyperion.dashboard.net;

public class Message {

    public Event event;
    public String json;

    public Message(Event event, String json) {
        this.event = event;
        this.json = json;
    }

    public Message(String from) {
        if (!from.isEmpty()) {
            this.event = Enum.valueOf(Event.class, from.split(" ")[0]);
            this.json = from.replaceFirst(event.toString() + " ", "");
        } else {
            this.event = Event.NULL;
        }
    }

    @Override
    public String toString() {
        return event.toString() + " " + json;
    }

    public enum Event {
        NULL, CONNECTED, DISCONNECTED, CONSTANTS_UPDATED, FIELD_EDITED, OPMODE_ENDED, METRICS_UPDATED
    }
}
