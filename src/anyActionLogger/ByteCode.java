package anyActionLogger;

import java.io.*;
import java.util.ArrayList;

import arc.util.Log;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;

public class ByteCode {
    public static ArrayList<String> getSwears() {
        JSONObject data = get();
        if (data == null) {
            return null;
        }
        JSONArray teme;
        try {
            teme = data.getJSONArray("swear");
        } catch (org.json.JSONException e) {
            return null;
        }
        ArrayList<String> tempArr = new ArrayList<String>();
        if (teme != null) {
            for (int i = 0; i < teme.length(); i++) {
                tempArr.add(teme.getString(i));
            }
        }
        return tempArr;
    }

    public static JSONObject get() {
        try {
            File file = new File("config/aal/swears.cn");
            File path = new File("config/aal/");
            if (!path.isDirectory()) {
                Log.err("404 - could not find directory config/aal/");
                mkdir("config/aal");
                return null;
            }
            if (!file.exists()) {
                Log.err("404 - all/swears.cn" + " not found");
                make("swears", new JSONObject());
                Log.info("Generated new swears.cn");
                return null;
            }
            FileReader fr = new FileReader(file);
            StringBuilder builder = new StringBuilder();
            int i;
            while ((i = fr.read()) != -1) {
                builder.append((char) i);
            }
            return new JSONObject(new JSONTokener(builder.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String make(String fileName, JSONObject object) {
        try {
            File file = new File("config/aal/swears.cn");
            File path = new File("config/aal/");
            if (!path.isDirectory()) {
                Log.err("404 - could not find directory config/aal/");
                return null;
            }
            if (!file.exists()) file.createNewFile();
            FileWriter out = new FileWriter(file, false);
            PrintWriter pw = new PrintWriter(out);
            pw.println(object.toString(4));
            out.close();
            return "Done";
        } catch (IOException i) {
            i.printStackTrace();
            return "error: \n```" + i.getMessage().toString() + "\n```";
        }
    }

    public static boolean mkdir(String dirName) {
        File path = new File("config/aal/");
        if (!path.isDirectory()) {
            if (path.mkdir()) return true;
            return false;
        }
        return true;
    }
}
