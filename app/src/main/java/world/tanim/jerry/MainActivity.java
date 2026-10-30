package world.tanim.jerry;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

/**
 * A native, independent Android implementation.
 * Push-to-talk, not an uninterrupted microphone/background call.
 */
public final class MainActivity extends Activity {
    private static final int VOICE_REQUEST = 100;
    private static final int EXPORT_MEMORY = 101;
    private static final int IMPORT_MEMORY = 102;
    private static final int NOTIFY_REQUEST = 103;
    private static final int CHOOSE_IMAGE = 105;
    private static final int CHOOSE_PDF = 106;
    private static final int LIVE_AUDIO_REQUEST = 107;
    private static final int SCAN_QR = 108;
    private static final int OFFLINE_OCR = 109;
    private static final int BACKGROUND_LIVE_REQUEST = 110;
    private static final int PICK_FILE = 111;
    private static final int BG = Color.rgb(12, 10, 25);
    private static final int PURPLE = Color.rgb(139, 92, 246);

    private SharedPreferences settings;
    private MemoryStore memory;
    private TextToSpeech voice;
    private LiveVoiceSession live;
    private TextView status;
    private TextView answer;
    private EditText question;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        settings = getSharedPreferences("jerry_independent_settings", MODE_PRIVATE);
        memory = new MemoryStore(this);
        live = new LiveVoiceSession(this, new LiveVoiceSession.Listener() {
            @Override public void onStatus(String message) {
                if (status != null) status.setText(message);
            }
            @Override public void onTranscript(String who, String message) {
                if (answer != null) answer.setText(who + ": " + message);
            }
        });
        buildInterface();
        voice = new TextToSpeech(this, code -> {
            if (code == TextToSpeech.SUCCESS && voice != null)
                voice.setLanguage(Locale.getDefault());
        });
    }
    private void buildInterface() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout area = new LinearLayout(this);
        area.setOrientation(LinearLayout.VERTICAL);
        area.setPadding(dp(20), dp(30), dp(20), dp(32));
        scroll.addView(area);
        TextView brand = label("jerry", 36, PURPLE, true);
        area.addView(brand);
        area.addView(label("AI Assistant  •  Tanim Boss", 15, 0xffcfbeff, false));
        area.addView(label("Independent preview • Main app is untouched", 12, 0xff9e9bb9, false));

        status = label("Ready. Enter your Gemini API key to start.", 14, 0xffc4badc, false);
        addSpace(area, 20);
        area.addView(status);

        answer = label("Say or type something. Examples: open WhatsApp, volume up, "
                + "remember that I like music, remind me in 10 minutes to drink water.", 16,
                Color.WHITE, false);
        answer.setPadding(dp(16), dp(18), dp(16), dp(18));
        GradientDrawable panel = new GradientDrawable();
        panel.setColor(0xff211c3b);
        panel.setCornerRadius(dp(18));
        answer.setBackground(panel);
        addSpace(area, 14);
        area.addView(answer, new LinearLayout.LayoutParams(-1, -2));

        question = new EditText(this);
        question.setSingleLine(false);
        question.setMinLines(2);
        question.setMaxLines(5);
        question.setTextColor(Color.WHITE);
        question.setHintTextColor(0xffa69eb9);
        question.setHint("Ask jerry or enter an Android voice command...");
        question.setPadding(dp(14), dp(14), dp(14), dp(14));
        addSpace(area, 18);
        area.addView(question, new LinearLayout.LayoutParams(-1, -2));
        area.addView(button("Send / Run command", () -> send(question.getText().toString())));
        area.addView(button("Speak to jerry (push to talk)", this::recognize));
        area.addView(button("Start Gemini Live audio call", this::startLive));
        area.addView(button("End Gemini Live call", () -> {
            if (live != null) live.stop();
        }));
        area.addView(button("Start background Gemini Live (visible notification)",
                this::startLiveBackground));
        area.addView(button("Stop background Gemini Live", () -> {
            stopService(new Intent(this, LiveVoiceForegroundService.class));
            show("Requested background Live microphone service to stop.", false);
        }));
        area.addView(button("Set / Replace Gemini API key", this::configureKey));
        area.addView(button("Analyze selected photo / screenshot", () -> chooseDocument(false)));
        area.addView(button("Read PDF (first 3 pages)", () -> chooseDocument(true)));
        area.addView(button("Scan QR / barcode from image (offline)", () -> chooseLocalVision(SCAN_QR)));
        area.addView(button("Read printed text from image (offline)", () -> chooseLocalVision(OFFLINE_OCR)));
        area.addView(button("Copy last jerry response", this::copyAnswer));
        area.addView(button("Paste clipboard into command", this::pasteClipboard));
        area.addView(button("Open a document with Android file picker", this::chooseAnyFile));
        area.addView(button("My saved memories", () -> show(memory.summary(), false)));
        area.addView(button("Export memory JSON", this::exportMemory));
        area.addView(button("Import memory JSON", this::importMemory));
        area.addView(button("Enable notification reading and replies", () -> {
            new AlertDialog.Builder(this)
                .setTitle("Optional notification access")
                .setMessage("Android will ask you to enable jerry Independent notification access. "
                    + "This allows the app to read notifications only when you request it. "
                    + "Replies require you to confirm the recipient app and message each time. "
                    + "You can turn this off in Android settings.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open Settings", (d, w) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } catch (Exception ex) {
                        show("Cannot open notification access settings: " + ex.getMessage(), false);
                    }
                }).show();
        }));
        area.addView(button("Enable Android accessibility actions", () -> {
            try { PhoneActions.execute(this, "open accessibility settings"); }
            catch (Exception e) { show("Could not open Android settings: " + e.getMessage(), false); }
        }));
        area.addView(button("Notification permission / settings", () -> {
            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFY_REQUEST);
            } else {
                show("Notifications are permitted or controlled by Android settings.", false);
            }
        }));
        area.addView(button("Show supported commands", this::showCommands));
        addSpace(area, 18);
        area.addView(label("This is a new implementation, not a renamed copy. "
                + "Continuous wake word, Gemini Live, contact auto-send, AI vision and avatar animation "
                + "are NOT yet implemented. The original jerry app remains unchanged.", 12,
                0xffaaa3bf, false));
        setContentView(scroll);
    }

    private void send(String raw) {
        String request = raw == null ? "" : raw.trim();
        if (request.isEmpty()) return;
        question.setText("");
        status.setText("Request: " + request);
        String low = request.toLowerCase(Locale.ROOT);
        try {
            if (low.startsWith("remember that ") || request.startsWith("মনে রাখো ")) {
                String note = low.startsWith("remember that ")
                        ? request.substring("remember that ".length()).trim()
                        : request.substring("মনে রাখো ".length()).trim();
                if (note.isEmpty()) { show("Tell me what to remember.", false); return; }
                memory.remember(note);
                show("Saved locally: " + note, true);
                return;
            }
            if (low.equals("what do you remember") || low.equals("show memory")
                    || low.equals("তুমি কি মনে রেখেছো")) {
                show(memory.summary(), true);
                return;
            }
            if (low.equals("delete all memories")) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete all local memories?")
                        .setMessage("This action cannot be undone unless you exported a backup.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            try {
                                memory.importJson("{\"format\":\"jerry-independent-memory-v1\",\"notes\":[]}");
                                show("All local notes deleted.", false);
                            } catch (JSONException e) { show("Could not delete: " + e.getMessage(), false); }
                        }).show();
                return;
            }
            if (low.equals("read notifications") || low.equals("show notifications")) {
                show(JerryNotificationListener.summarize(), false);
                return;
            }
            if (low.startsWith("reply latest notification ")) {
                String response = request.substring("reply latest notification ".length()).trim();
                if (response.isEmpty()) {
                    show("Please specify a reply message.", false);
                    return;
                }
                JerryNotificationListener.ReplyCandidate candidate =
                    JerryNotificationListener.findLatestReplyable();
                if (candidate == null) {
                    show("No active replyable notification found. Enable notification access; "
                            + "some apps do not allow notification quick reply.", false);
                    return;
                }
                new AlertDialog.Builder(this)
                    .setTitle("Approve notification reply?")
                    .setMessage("App: " + candidate.app + "\nConversation: " + candidate.title
                            + "\nPrevious notification: " + candidate.excerpt
                            + "\n\nYour message: " + response
                            + "\n\nOnly tap Send if this is the intended recipient.")
                    .setNegativeButton("Cancel", (d, w) ->
                            show("Notification reply cancelled. Nothing was sent.", false))
                    .setPositiveButton("Send", (d, w) ->
                            show(JerryNotificationListener.sendApprovedReply(candidate, response), false))
                    .show();
                return;
            }
            String reminder = Reminders.parseAndSchedule(this, request);
            if (reminder != null) {
                show(reminder, true);
                if (Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFY_REQUEST);
                }
                return;
            }
            String action = PhoneActions.execute(this, request);
            if (action != null) { show(action, true); return; }
        } catch (Exception e) {
            show("Command failed: " + e.getMessage(), false);
            return;
        }
        String key = settings.getString("gemini_key", "");
        if (key.isEmpty()) {
            show("No Gemini key saved. Tap 'Set / Replace Gemini API key' to chat.", false);
            return;
        }
        show("Contacting Gemini...", false);
        GeminiClient.ask(key, memory.summary(), request, (result, success) ->
                runOnUiThread(() -> show(result, success)));
    }

    private void configureKey() {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setHint("Enter your Gemini API key");
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(dp(22), dp(8), dp(22), 0);
        wrap.addView(field, new LinearLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(this)
                .setTitle("Private Gemini key")
                .setMessage("Stored locally in app-private preferences. Sent only to Google's Gemini API "
                        + "during requests; not to a custom server. Avoid exporting keys.")
                .setView(wrap)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Remove", (d, w) -> {
                    settings.edit().remove("gemini_key").apply();
                    show("Gemini key removed.", false);
                })
                .setPositiveButton("Save", (d, w) -> {
                    String entered = field.getText().toString().trim();
                    if (!entered.isEmpty()) {
                        settings.edit().putString("gemini_key", entered).apply();
                        show("Gemini API key saved locally.", false);
                    }
                }).show();
    }
    private void startLiveBackground() {
        String key = settings.getString("gemini_key", "");
        if (key.isEmpty()) {
            show("Save your own Gemini API key first.", false);
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                    BACKGROUND_LIVE_REQUEST);
            return;
        }
        try {
            if (live != null && live.isActive()) live.stop();
            Intent start = new Intent(this, LiveVoiceForegroundService.class)
                    .setAction(LiveVoiceForegroundService.ACTION_START);
            startForegroundService(start); // User-visible Activity and explicit tap.
            show("Starting background Live call. Android will display a microphone notification "
                    + "with a Stop action.", false);
        } catch (Exception ex) {
            show("Android denied background Live call: " + ex.getMessage(), false);
        }
    }
    private void startLive() {
        String key = settings.getString("gemini_key", "");
        if (key.isEmpty()) {
            show("Set your Gemini API key first to start Live voice.", false);
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, LIVE_AUDIO_REQUEST);
            return;
        }
        if (voice != null) voice.stop();
        if (LiveVoiceForegroundService.isRunning()) {
            stopService(new Intent(this, LiveVoiceForegroundService.class));
        }
        live.start(key);
    }
    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] grants) {
        super.onRequestPermissionsResult(code, perms, grants);
        if (code == BACKGROUND_LIVE_REQUEST) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED)
                startLiveBackground();
            else show("Microphone permission declined; background call not started.", false);
        }
        if (code == LIVE_AUDIO_REQUEST) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED)
                startLive();
            else show("Microphone permission declined. Live audio will not start.", false);
        }
    }
    private void recognize() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to jerry");
            startActivityForResult(intent, VOICE_REQUEST);
        } catch (Exception ex) {
            show("Android speech recognition is unavailable: " + ex.getMessage(), false);
        }
    }
    private void copyAnswer() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            show("Android clipboard unavailable.", false);
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("jerry reply", answer.getText()));
        show("jerry reply copied to Android clipboard.", false);
    }
    private void pasteClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            show("Clipboard is empty or unavailable.", false);
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        question.setText(text == null ? "" : text.toString());
        show("Clipboard text pasted into command box. Review before sending.", false);
    }
    private void chooseAnyFile() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        startActivityForResult(picker, PICK_FILE);
    }
    private void chooseLocalVision(int mode) {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("image/*");
        try {
            startActivityForResult(picker, mode);
        } catch (Exception ex) {
            show("Cannot open image chooser: " + ex.getMessage(), false);
        }
    }
    private void chooseDocument(boolean pdf) {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType(pdf ? "application/pdf" : "image/*");
        try {
            startActivityForResult(picker, pdf ? CHOOSE_PDF : CHOOSE_IMAGE);
        } catch (Exception ex) {
            show("Cannot open the Android file picker: " + ex.getMessage(), false);
        }
    }

    private void exportMemory() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "jerry-memory.json");
        startActivityForResult(i, EXPORT_MEMORY);
    }
    private void importMemory() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i, IMPORT_MEMORY);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == VOICE_REQUEST) {
            ArrayList<String> possibilities = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (possibilities != null && !possibilities.isEmpty()) send(possibilities.get(0));
            return;
        }
        Uri uri = data.getData();
        if (uri == null) return;
        if (requestCode == PICK_FILE) {
            String name = "Selected document";
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (column >= 0) name = cursor.getString(column);
                }
            } catch (Exception ignored) { }
            final String label = name;
            final Uri selected = uri;
            new AlertDialog.Builder(this)
                .setTitle("Selected file: " + label)
                .setMessage("Android's file manager granted access to this chosen file only. "
                    + "Open it with a compatible installed app? The file is not uploaded to jerry.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open", (d, w) -> {
                    try {
                        Intent open = new Intent(Intent.ACTION_VIEW, selected);
                        open.setDataAndType(selected, getContentResolver().getType(selected));
                        open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(open, "Open selected file"));
                    } catch (Exception ex) {
                        show("No compatible app to open selected file: " + ex.getMessage(), false);
                    }
                }).show();
            return;
        }
        if (requestCode == SCAN_QR) {
            LocalVisionTools.scanBarcode(this, uri,
                (message, success) -> runOnUiThread(() -> show(message, false)));
            return;
        }
        if (requestCode == OFFLINE_OCR) {
            LocalVisionTools.extractText(this, uri,
                (message, success) -> runOnUiThread(() -> show(message, false)));
            return;
        }
        if (requestCode == CHOOSE_IMAGE || requestCode == CHOOSE_PDF) {
            String apiKey = settings.getString("gemini_key", "");
            if (apiKey.isEmpty()) {
                show("Set your Gemini API key before analyzing an image or PDF.", false);
                return;
            }
            boolean pdf = requestCode == CHOOSE_PDF;
            new AlertDialog.Builder(this)
                .setTitle(pdf ? "Analyze selected PDF?" : "Analyze selected image?")
                .setMessage("The selected " + (pdf ? "PDF (first 3 pages)" : "image")
                    + " will be sent to Google's Gemini API for analysis. "
                    + "Only continue if you consent and the file contains no information "
                    + "you do not want to share.")
                .setNegativeButton("Cancel", (d, w) ->
                    show("File analysis cancelled. Nothing was uploaded.", false))
                .setPositiveButton("Analyze", (d, w) -> {
                    show("Analyzing selected file with Gemini...", false);
                    VisionClient.analyze(this, uri, pdf, apiKey, (message, success) ->
                        runOnUiThread(() -> show(message, success)));
                })
                .show();
            return;
        }
        try {
            if (requestCode == EXPORT_MEMORY) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new IllegalStateException("File not writable");
                    out.write(memory.exportJson().getBytes(StandardCharsets.UTF_8));
                }
                show("Memory saved to the document you selected.", false);
            } else if (requestCode == IMPORT_MEMORY) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IllegalStateException("Cannot read file");
                    byte[] bytes = new byte[4096];
                    int read;
                    while ((read = in.read(bytes)) != -1) {
                        if (buffer.size() > 1024 * 1024)
                            throw new IllegalStateException("Memory pack is too large");
                        buffer.write(bytes, 0, read);
                    }
                }
                String json = buffer.toString("UTF-8");
                new AlertDialog.Builder(this)
                        .setTitle("Replace saved memory?")
                        .setMessage("Import will replace current notes. Export them first if needed.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Restore", (d, w) -> {
                            try {
                                memory.importJson(json);
                                show("Memory restored from selected file.", false);
                            } catch (Exception e) { show("Invalid memory pack: " + e.getMessage(), false); }
                        }).show();
            }
        } catch (Exception e) {
            show("File action failed: " + e.getMessage(), false);
        }
    }

    private void showCommands() {
        show("COMMANDS\n\nopen WhatsApp / open YouTube / open Camera\n"
                + "open Instagram / open [installed app name]\n"
                + "youtube search cute cats\nsearch weather in Dubai\n"
                + "torch on / torch off\nvolume up / volume down\n"
                + "back / go home / take screenshot\n"
                + "whatsapp message Hello there (draft only)\n"
                + "read notifications\nreply latest notification Hello (confirm before sending)\n"
                + "Use buttons to analyze a photo, screenshot or PDF (Gemini consent required)\n"
                + "remember that I like Bengali\nwhat do you remember\n"
                + "remind me in 10 minutes to drink water\n"
                + "remind me in 2 hours to take a break\n\n"
                + "A Gemini key is needed for general conversational questions. "
                + "These commands may require device permissions. Other features "
                + "from the previous jerry app are not yet available.", false);
    }
    private void show(String message, boolean say) {
        status.setText("jerry • " + (say ? "Ready" : "Info"));
        answer.setText(message);
        if (say && voice != null && (live == null || !live.isActive())
                && message.length() < 2000) {
            voice.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jerry-reply");
        }
    }
    private Button button(String name, Runnable action) {
        Button b = new Button(this);
        b.setText(name);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xff34294d);
        bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.topMargin = dp(10);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }
    private void addSpace(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }
    private int dp(int n) {
        return Math.round(getResources().getDisplayMetrics().density * n);
    }
    @Override protected void onDestroy() {
        if (live != null) live.close();
        if (voice != null) voice.shutdown();
        super.onDestroy();
    }
}
