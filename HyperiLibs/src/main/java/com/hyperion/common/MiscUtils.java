package com.hyperion.common;

import com.hyperion.net.FieldEdit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class MiscUtils {

    public static void writeFieldEditsToFieldJSON(File toWrite, FieldEdit... edits) {
        try {
            JSONObject field = new JSONObject(IOUtils.readFile(toWrite));
            for (FieldEdit edit : edits) {
                if (!edit.id.equals("robot")) {
                    String o = "";
                    if (edit.id.contains("waypoint")) o = "waypoints";
                    else if (edit.id.contains("spline")) o = "splines";
                    else if (edit.id.contains("obstacle")) o = "obstacles";
                    JSONObject targ = field.getJSONObject(o);

                    switch (edit.type) {
                        case CREATE:
                        case EDIT_BODY:
                            targ.put(edit.id.toString(), (o.equals("waypoints")) ? new JSONArray(edit.body) : new JSONObject(edit.body));
                            break;
                        case EDIT_ID:
                            if (edit.id.contains("waypoint")) {
                                JSONArray wpArr = targ.getJSONArray(edit.id.toString());
                                targ.remove(edit.id.toString());
                                targ.put(edit.body, wpArr);
                            } else {
                                JSONObject splineObj = targ.getJSONObject(edit.id.toString());
                                targ.remove(edit.id.toString());
                                targ.put(edit.body, splineObj);
                            }
                            break;
                        case DELETE:
                            targ.remove(edit.id.toString());
                            break;
                    }
                    field.put(o, targ);
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
