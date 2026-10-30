package world.tanim.jerry;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/** A new, independently implemented REST text client; not Gemini Live audio. */
public final class GeminiClient {
    public interface Callback { void onResult(String answer, boolean success); }
    private GeminiClient() {}
    public static void ask(String key, String memory, String question, Callback callback) {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                URL u = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                        + URLEncoder.encode(key, "UTF-8"));
                c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(20000);
                c.setReadTimeout(45000);
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                c.setDoOutput(true);
                JSONObject system = new JSONObject();
                system.put("parts", new JSONArray().put(new JSONObject().put("text",
                        "You are jerry, an original personal assistant built for Tanim Boss. "
                        + "Reply in the user's language, in a friendly, practical tone. "
                        + "Do not claim phone features you have not actually used. "
                        + "Here are local notes the user opted to share: " + memory)));
                JSONObject user = new JSONObject()
                    .put("role", "user")
                    .put("parts", new JSONArray().put(new JSONObject().put("text", question)));
                JSONObject payload = new JSONObject()
                    .put("system_instruction", system)
                    .put("contents", new JSONArray().put(user));
                try (OutputStream os = c.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                int status = c.getResponseCode();
                InputStream stream = status < 400 ? c.getInputStream() : c.getErrorStream();
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                }
                JSONObject reply = new JSONObject(body.toString());
                if (status >= 400) {
                    String reason = reply.optJSONObject("error") != null
                            ? reply.getJSONObject("error").optString("message") : "HTTP " + status;
                    callback.onResult("Gemini request failed: " + reason, false);
                } else {
                    JSONArray candidates = reply.optJSONArray("candidates");
                    if (candidates == null || candidates.length() == 0) {
                        callback.onResult("Gemini returned no answer.", false); return;
                    }
                    JSONArray parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts");
                    StringBuilder answer = new StringBuilder();
                    for (int i = 0; i < parts.length(); i++) {
                        String text = parts.getJSONObject(i).optString("text");
                        if (!text.isEmpty()) answer.append(text);
                    }
                    callback.onResult(answer.length() > 0 ? answer.toString()
                            : "Gemini returned no text response.", answer.length() > 0);
                }
            } catch (Exception e) {
                callback.onResult("Gemini unavailable: " + e.getClass().getSimpleName()
                        + " — " + e.getMessage(), false);
            } finally {
                if (c != null) c.disconnect();
            }
        }, "jerry-gemini-request").start();
    }
}
