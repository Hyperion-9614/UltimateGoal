package com.hyperion.net;

import com.hyperion.common.ID;

import org.json.JSONObject;

public class FieldEdit {

    public ID id;
    public Type type;
    public String body;

    public FieldEdit(JSONObject obj) {
        try {
            this.id = new ID(obj.getString("id"));
            this.type = Enum.valueOf(Type.class, obj.getString("type"));
            this.body = obj.getString("body");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FieldEdit(ID id, Type type, Object body) {
        this.id = id;
        this.type = type;
        this.body = body.toString();
    }

    public JSONObject toJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id.toString());
            obj.put("type", type.name());
            obj.put("body", body);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    public String toString() {
        return toJSONObject().toString();
    }

    public enum Type {
        CREATE, EDIT_BODY, EDIT_ID, DELETE
    }

}
