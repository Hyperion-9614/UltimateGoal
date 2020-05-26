package com.hyperion.common;

import org.json.*;

import java.io.*;

public class IOUtils {

    public static String readDataJSON(String fileName) {
        return readFile(Constants.getFile("data", fileName + ".json"));
    }

    public static String readFile(File file) {
        try {
            return readStream(new FileInputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String readStream(InputStream stream) {
        String json = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                json += line;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public static void writeDataJSON(String json, String fileName) {
        try {
            writeFile(new JSONObject(json).toString(4), Constants.getFile("data", fileName + ".json"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String toWrite, File file) {
        try {
            writeStream(toWrite, new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeStream(String toWrite, OutputStream stream) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
            writer.write(toWrite);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
