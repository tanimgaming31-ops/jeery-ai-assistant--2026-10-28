package world.tanim.jerry;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Schedules inexact user reminders; does not request privileged exact alarms. */
public final class Reminders {
    private static final Pattern PATTERN = Pattern.compile(
        "^remind me in (\\d{1,5}) (minute|minutes|hour|hours) to (.+)$", Pattern.CASE_INSENSITIVE);
    private Reminders() {}
    public static String parseAndSchedule(Context context, String message) {
        Matcher m = PATTERN.matcher(message.trim());
        if (!m.matches()) return null;
        int number;
        try { number = Integer.parseInt(m.group(1)); }
        catch (NumberFormatException ex) { return "Invalid reminder time."; }
        if (number < 1) return "Reminder time must be positive.";
        boolean hours = m.group(2).toLowerCase(Locale.ROOT).startsWith("hour");
        long delay = number * (hours ? 3600000L : 60000L);
        String task = m.group(3).trim();
        int id = (int) (SystemClock.elapsedRealtime() & 0x7fffffff);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("message", task);
        intent.putExtra("id", id);
        PendingIntent pending = PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (mgr == null) return "Alarm service is not available.";
        try {
            mgr.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delay, pending);
        } catch (SecurityException e) {
            return "Android did not permit scheduling this reminder.";
        }
        return "Reminder scheduled for about " + number + " "
                + (hours ? "hour(s)" : "minute(s)") + ": " + task
                + ". Android may deliver it later to save battery.";
    }
}
