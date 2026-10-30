package world.tanim.jerry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Original user-initiated image/PDF vision: selected files are sent to Gemini
 * only after the user chooses a file and explicitly asks for analysis.
 * PDFs are bounded to the first three rendered pages.
 */
public final class VisionClient {
    public interface Callback {
        void complete(String text, boolean success);
    }
    private VisionClient() { }

    public static void analyze(Context context, Uri file, boolean pdf, String key,
                               Callback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text",
                        pdf
                        ? "You are jerry, assistant for Tanim Boss. Read and summarize this PDF "
                            + "using only the displayed pages. Explain what they say in the user's language. "
                            + "If there may be additional pages, clearly say only the first three were analyzed."
                        : "You are jerry, assistant for Tanim Boss. Read this selected image. "
                            + "Describe its contents, printed text, or any useful details in the user's language. "
                            + "Be precise and say if anything is unreadable."));
                int pages = pdf ? appendPdfParts(context, file, parts)
                                : appendImagePart(context, file, parts);
                if (pages < 1) throw new IllegalArgumentException("No readable image or PDF page");
                JSONObject payload = new JSONObject();
                payload.put("contents", new JSONArray().put(new JSONObject()
                    .put("role", "user").put("parts", parts)));
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                        + "gemini-3.8-flash:generateContent?key="
                        + URLEncoder.encode(key, "UTF-8"));
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                int status = conn.getResponseCode();
                StringBuilder body = new StringBuilder();
                try (InputStream stream = status < 400 ? conn.getInputStream() : conn.getErrorStream();
                     BufferedReader reader = new BufferedReader(
                         new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                }
                JSONObject reply = new JSONObject(body.toString());
                if (status >= 400) {
                    JSONObject error = reply.optJSONObject("error");
                    callback.complete("Gemini could not analyze: "
                            + (error == null ? ("HTTP " + status) : error.optString("message")), false);
                    return;
                }
                JSONArray choices = reply.optJSONArray("candidates");
                if (choices == null || choices.length() == 0)
                    throw new IllegalStateException("Gemini returned no image interpretation");
                JSONArray texts = choices.getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts");
                StringBuilder answer = new StringBuilder();
                for (int i = 0; i < texts.length(); i++)
                    answer.append(texts.getJSONObject(i).optString("text"));
                if (pdf) answer.append("\n\nScanned pages: ").append(pages).append(" (maximum 3).");
                callback.complete(answer.toString(), true);
            } catch (Exception ex) {
                callback.complete("Vision analysis unavailable: " + ex.getMessage(), false);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }, "jerry-vision").start();
    }

    private static int appendImagePart(Context context, Uri uri, JSONArray parts) throws Exception {
        Bitmap sampled;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IllegalArgumentException("Could not open image");
            BitmapFactory.decodeStream(stream, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0)
            throw new IllegalArgumentException("Unsupported image file");
        int sample = 1;
        while (options.outWidth / sample > 1400 || options.outHeight / sample > 1400)
            sample *= 2;
        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IllegalArgumentException("Could not reopen image");
            sampled = BitmapFactory.decodeStream(stream, null, options);
        }
        if (sampled == null) throw new IllegalArgumentException("Image decoder unavailable");
        try {
            appendBitmap(parts, sampled);
        } finally {
            sampled.recycle();
        }
        return 1;
    }

    private static int appendPdfParts(Context context, Uri uri, JSONArray parts) throws Exception {
        int count = 0;
        try (ParcelFileDescriptor descriptor =
                 context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (descriptor == null) throw new IllegalArgumentException("Could not open PDF");
            try (PdfRenderer renderer = new PdfRenderer(descriptor)) {
                int limit = Math.min(renderer.getPageCount(), 3);
                for (int n = 0; n < limit; n++) {
                    try (PdfRenderer.Page page = renderer.openPage(n)) {
                        int w = Math.max(1, page.getWidth());
                        int h = Math.max(1, page.getHeight());
                        float scale = Math.min(1.4f, Math.min(1100f / w, 1500f / h));
                        Bitmap bitmap = Bitmap.createBitmap(
                                Math.max(1, Math.round(w * scale)),
                                Math.max(1, Math.round(h * scale)), Bitmap.Config.ARGB_8888);
                        try {
                            bitmap.eraseColor(android.graphics.Color.WHITE);
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            parts.put(new JSONObject().put("text", "PDF page " + (n + 1)));
                            appendBitmap(parts, bitmap);
                            count++;
                        } finally {
                            bitmap.recycle();
                        }
                    }
                }
            }
        }
        return count;
    }
    private static void appendBitmap(JSONArray parts, Bitmap bitmap) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 78, bytes))
            throw new IllegalArgumentException("Image encoding failed");
        JSONObject inline = new JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP));
        parts.put(new JSONObject().put("inline_data", inline));
    }
}
