package com.hyperion.common;

import com.hyperion.dashboard.net.FieldEdit;
import com.hyperion.motion.math.Vector2D;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class MiscUtils {

    public static void writeFieldEditsToFieldJSON(File toWrite, FieldEdit... edits) {
        try {
            JSONObject field = new JSONObject(IOUtils.readFile(toWrite));
            for (FieldEdit edit : edits) {
                if (!edit.id.equals("robot")) {
                    String o = edit.id.contains("waypoint") ? "waypoints" : "splines";
                    JSONObject target = field.getJSONObject(o);
                    switch (edit.type) {
                        case CREATE:
                        case EDIT_BODY:
                            target.put(edit.id.toString(), o.equals("waypoints") ? new JSONArray(edit.body) : new JSONObject(edit.body));
                            break;
                        case EDIT_ID:
                            if (edit.id.contains("waypoint")) {
                                JSONArray wpArr = target.getJSONArray(edit.id.toString());
                                target.remove(edit.id.toString());
                                target.put(edit.body, wpArr);
                            } else {
                                JSONObject splineObj = target.getJSONObject(edit.id.toString());
                                target.remove(edit.id.toString());
                                target.put(edit.body, splineObj);
                            }
                            break;
                        case DELETE:
                            target.remove(edit.id.toString());
                            break;
                    }
                    field.put(o, target);
                }
            }
            IOUtils.writeFile(field.toString(), toWrite);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeFieldEditsToFieldJSON(File toWrite, String edits) {
        JSONArray editsJsonArr = new JSONArray(edits);
        FieldEdit[] editsArr = new FieldEdit[editsJsonArr.length()];
        for (int i = 0; i < editsArr.length; i++) {
            editsArr[i] = new FieldEdit(editsJsonArr.getJSONObject(i));
        }
        writeFieldEditsToFieldJSON(toWrite, editsArr);
    }

}
