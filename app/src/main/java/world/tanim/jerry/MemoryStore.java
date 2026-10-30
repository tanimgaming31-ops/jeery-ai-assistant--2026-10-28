package world.tanim.jerry;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Local notes; data never leaves this device unless user explicitly exports it. */
public final class MemoryStore {
    private final SharedPreferences prefs;
    public MemoryStore(Context c) {
        prefs = c.getSharedPreferences("jerry_independent_memory", Context.MODE_PRIVATE);
    }
    public void remember(String note) throws JSONException {
        JSONArray notes = notes();
        notes.put(note);
        prefs.edit().putString("notes", notes.toString()).apply();
    }
    public JSONArray notes() throws JSONException {
        return new JSONArray(prefs.getString("notes", "[]"));
    }
    public String summary() {
        try {
            JSONArray notes = notes();
            if (notes.length() == 0) return "No saved notes yet.";
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < notes.length(); i++) {
                result.append(i + 1).append(". ").append(notes.optString(i)).append("\n");
            }
            return result.toString().trim();
        } catch (JSONException e) {
            return "Saved notes could not be read.";
        }
    }
    public String exportJson() throws JSONException {
        JSONObject out = new JSONObject();
        out.put("format", "jerry-independent-memory-v1");
        out.put("notes", notes());
        return out.toString(2);
    }
    public void importJson(String raw) throws JSONException {
        JSONObject doc = new JSONObject(raw);
        if (!"jerry-independent-memory-v1".equals(doc.optString("format"))) {
            throw new JSONException("Wrong memory pack format");
        }
        JSONArray restored = doc.getJSONArray("notes");
        for (int i = 0; i < restored.length(); i++) restored.getString(i);
        prefs.edit().putString("notes", restored.toString()).apply();
    }
}
