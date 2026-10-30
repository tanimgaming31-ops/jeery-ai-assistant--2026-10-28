package world.tanim.jerry;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "jerry_local_reminders";
    @Override public void onReceive(Context context, Intent intent) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "jerry reminders", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent home = new Intent(context, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(context, 0, home,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String message = intent.getStringExtra("message");
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("jerry reminder")
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build();
        try {
            manager.notify(intent.getIntExtra("id", 1), notification);
        } catch (SecurityException ignored) {
            // Android 13+ POST_NOTIFICATIONS must be granted for notifications.
        }
    }
}
