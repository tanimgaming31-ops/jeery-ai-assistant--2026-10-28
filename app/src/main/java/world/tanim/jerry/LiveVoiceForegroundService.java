package world.tanim.jerry;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Visible, user-initiated foreground microphone session.
 * Starts only from a foreground Activity, never silently at boot.
 * Notification Stop button always terminates microphone capture.
 */
public final class LiveVoiceForegroundService extends Service {
    public static final String ACTION_START = "world.tanim.jerry.action.START_LIVE";
    public static final String ACTION_STOP = "world.tanim.jerry.action.STOP_LIVE";
    private static final String CHANNEL = "jerry_live_voice_channel";
    private static final int NOTIFICATION_ID = 8005;
    private LiveVoiceSession voice;
    private static volatile boolean running = false;
    public static boolean isRunning() { return running; }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL, "jerry live microphone", NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "" : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!ACTION_START.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission required for Live voice",
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            Notification active = buildNotification("Connecting Gemini Live; microphone active");
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, active,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, active);
            }
        } catch (RuntimeException ex) {
            Toast.makeText(this, "Android did not permit microphone service: "
                    + ex.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (voice != null && voice.isActive()) return START_NOT_STICKY;
        String key = getSharedPreferences("jerry_independent_settings", MODE_PRIVATE)
                .getString("gemini_key", "");
        if (key == null || key.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        voice = new LiveVoiceSession(this, new LiveVoiceSession.Listener() {
            @Override public void onStatus(String state) {
                NotificationManager manager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.notify(NOTIFICATION_ID, buildNotification(state));
                if (state.contains("connection failed") || state.contains("could not")
                        || state.contains("stopped") || state.contains("closed")) {
                    stopSelf();
                }
            }
            @Override public void onTranscript(String who, String text) {
                // No saved transcript by default. Keep private conversation off disk.
            }
        });
        running = true;
        voice.start(key);
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String status) {
        Intent stop = new Intent(this, LiveVoiceForegroundService.class)
                .setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 44, stop,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent home = new Intent(this, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(this, 45, home,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("jerry — Live Voice")
                .setContentText(status)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop microphone", stopAction)
                .build();
    }

    @Override public void onDestroy() {
        running = false;
        if (voice != null) {
            voice.close();
            voice = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
