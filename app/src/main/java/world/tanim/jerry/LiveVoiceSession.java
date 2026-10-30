package world.tanim.jerry;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Separate, independently written Gemini 3.8 Live audio transport.
 * Explicit user Start/Stop only, foreground Activity ownership, no stealth listening.
 *
 * Audio wire format: PCM16 little-endian, input 16k mono, output 24k mono.
 * Session requires a key supplied by the user, and real device/network testing.
 */
public final class LiveVoiceSession {
    public interface Listener {
        void onStatus(String status);
        void onTranscript(String who, String text);
    }

    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
            .pingInterval(20, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    private final ExecutorService speakerThread = Executors.newSingleThreadExecutor();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean sessionReady = new AtomicBoolean(false);
    private volatile WebSocket socket;
    private volatile AudioRecord recorder;
    private volatile AudioTrack player;
    private volatile Thread captureThread;
    private volatile AudioManager audio;
    private volatile int oldMode = AudioManager.MODE_NORMAL;

    public LiveVoiceSession(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }
    public boolean isActive() { return active.get(); }

    public synchronized void start(String key) {
        if (key == null || key.trim().isEmpty()) {
            status("Set a Gemini API key in Settings first.");
            return;
        }
        if (active.get()) {
            status("Live voice is already running.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 &&
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            status("Microphone permission is required. Enable it in Android settings.");
            return;
        }
        active.set(true);
        sessionReady.set(false);
        status("Connecting to Gemini Live…");
        try {
            String url = "wss://generativelanguage.googleapis.com/ws/"
                    + "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key="
                    + URLEncoder.encode(key.trim(), "UTF-8");
            Request request = new Request.Builder().url(url).build();
            socket = client.newWebSocket(request, new WebSocketListener() {
                @Override public void onOpen(WebSocket ws, Response response) {
                    if (!active.get()) { ws.close(1000, "Stopped"); return; }
                    try {
                        JSONObject setup = new JSONObject()
                                .put("model", "models/gemini-3.8-live")
                                .put("responseModalities", new JSONArray().put("AUDIO"))
                                .put("systemInstruction", new JSONObject().put("parts",
                                        new JSONArray().put(new JSONObject().put("text",
                                                "You are jerry, an original friendly personal voice assistant "
                                                + "created for Tanim Boss. Speak in the user's language. "
                                                + "Be concise and conversational. "
                                                + "Do not claim to take any phone action unless an Android tool actually did it."))));
                        ws.send(new JSONObject().put("setup", setup).toString());
                        status("Live connected; waiting for setup.");
                    } catch (Exception e) { status("Setup failed: " + e.getMessage()); stop(); }
                }
                @Override public void onMessage(WebSocket ws, String text) {
                    if (!active.get()) return;
                    try {
                        JSONObject msg = new JSONObject(text);
                        if (msg.has("setupComplete") && sessionReady.compareAndSet(false, true)) {
                            startAudio();
                            status("Gemini Live voice connected. Speak naturally.");
                        }
                        JSONObject content = msg.optJSONObject("serverContent");
                        if (content != null) {
                            if (content.optBoolean("interrupted")) {
                                speakerThread.execute(() -> {
                                    AudioTrack track = player;
                                    if (track != null) {
                                        try { track.pause(); track.flush(); track.play(); }
                                        catch (IllegalStateException ignored) { }
                                    }
                                });
                            }
                            JSONObject input = content.optJSONObject("inputTranscription");
                            if (input != null) transcript("You", input.optString("text"));
                            JSONObject output = content.optJSONObject("outputTranscription");
                            if (output != null) transcript("jerry", output.optString("text"));
                            JSONObject turn = content.optJSONObject("modelTurn");
                            if (turn != null) {
                                JSONArray parts = turn.optJSONArray("parts");
                                if (parts != null) {
                                    for (int i = 0; i < parts.length(); i++) {
                                        JSONObject part = parts.optJSONObject(i);
                                        if (part == null) continue;
                                        JSONObject inline = part.optJSONObject("inlineData");
                                        if (inline != null && inline.has("data")) {
                                            String data = inline.optString("data");
                                            byte[] pcm = Base64.decode(data, Base64.DEFAULT);
                                            if (pcm.length > 0 && pcm.length < 2000000) {
                                                speakerThread.execute(() -> play(pcm));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (msg.has("goAway")) {
                            status("Gemini Live session is ending; start a new call to continue.");
                            stop();
                        }
                    } catch (Exception ex) {
                        status("Live response parse error: " + ex.getMessage());
                    }
                }
                @Override public void onFailure(WebSocket ws, Throwable t, Response response) {
                    int code = response == null ? 0 : response.code();
                    status("Gemini Live connection failed"
                            + (code > 0 ? " (HTTP " + code + ")" : "") + ": " + t.getMessage());
                    stop();
                }
                @Override public void onClosed(WebSocket ws, int code, String reason) {
                    status("Gemini Live closed (" + code + ").");
                    stop();
                }
            });
        } catch (Exception e) {
            status("Could not start Live: " + e.getMessage());
            stop();
        }
    }

    private void startAudio() {
        if (!active.get()) return;
        try {
            int inMin = AudioRecord.getMinBufferSize(16000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (inMin <= 0) throw new IllegalStateException("Microphone PCM16 unavailable");
            AudioRecord r = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(inMin, 6400));
            if (r.getState() != AudioRecord.STATE_INITIALIZED) {
                r.release();
                throw new IllegalStateException("Could not initialize microphone");
            }
            int outMin = AudioTrack.getMinBufferSize(24000,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (outMin <= 0) throw new IllegalStateException("Speaker PCM16 unavailable");
            AudioTrack t = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(24000)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(Math.max(outMin, 9600))
                    .setTransferMode(AudioTrack.MODE_STREAM).build();
            if (t.getState() != AudioTrack.STATE_INITIALIZED) {
                r.release(); t.release();
                throw new IllegalStateException("Could not initialize speaker");
            }
            recorder = r;
            player = t;
            audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                oldMode = audio.getMode();
                audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
            t.play();
            r.startRecording();
            captureThread = new Thread(() -> {
                byte[] frame = new byte[3200]; // 100ms of PCM 16kHz mono
                while (active.get() && sessionReady.get()) {
                    AudioRecord source = recorder;
                    WebSocket ws = socket;
                    if (source == null || ws == null) break;
                    int n;
                    try { n = source.read(frame, 0, frame.length); }
                    catch (Exception ex) { break; }
                    if (n <= 0) continue;
                    if (ws.queueSize() > 1500000) continue; // drop frames if connection is congested
                    try {
                        JSONObject pcm = new JSONObject()
                                .put("mimeType", "audio/pcm;rate=16000")
                                .put("data", Base64.encodeToString(Arrays.copyOf(frame, n),
                                        Base64.NO_WRAP));
                        ws.send(new JSONObject().put("realtimeInput",
                                new JSONObject().put("audio", pcm)).toString());
                    } catch (Exception e) { status("Audio upload failed: " + e.getMessage()); break; }
                }
            }, "jerry-live-microphone");
            captureThread.start();
        } catch (Exception e) {
            status("Could not start audio device: " + e.getMessage());
            stop();
        }
    }

    private void play(byte[] pcm) {
        if (!active.get()) return;
        AudioTrack track = player;
        if (track == null || track.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) return;
        int done = 0;
        while (done < pcm.length && active.get()) {
            int written = track.write(pcm, done, pcm.length - done, AudioTrack.WRITE_BLOCKING);
            if (written <= 0) break;
            done += written;
        }
    }
    public synchronized void stop() {
        if (!active.getAndSet(false)) return;
        sessionReady.set(false);
        AudioRecord oldRecord = recorder;
        recorder = null;
        if (oldRecord != null) {
            try { oldRecord.stop(); } catch (Exception ignored) { }
            oldRecord.release();
        }
        AudioTrack oldPlayer = player;
        player = null;
        speakerThread.execute(() -> {
            if (oldPlayer != null) {
                try { oldPlayer.pause(); oldPlayer.flush(); oldPlayer.release(); }
                catch (Exception ignored) { }
            }
        });
        AudioManager a = audio;
        audio = null;
        if (a != null) a.setMode(oldMode);
        WebSocket ws = socket;
        socket = null;
        if (ws != null) ws.close(1000, "User ended call");
        status("Live voice stopped.");
    }
    public synchronized void close() {
        stop();
        speakerThread.shutdown();
        client.dispatcher().executorService().shutdown();
    }
    private void status(String message) { main.post(() -> listener.onStatus(message)); }
    private void transcript(String who, String text) {
        if (!text.isEmpty()) main.post(() -> listener.onTranscript(who, text));
    }
}
