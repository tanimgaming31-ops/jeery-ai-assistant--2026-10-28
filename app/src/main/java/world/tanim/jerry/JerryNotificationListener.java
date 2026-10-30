package world.tanim.jerry;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.app.RemoteInput;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Independently authored, opt-in notification reader and user-approved reply.
 * Never sends anything automatically without an affirmative confirmation in UI.
 */
public final class JerryNotificationListener extends NotificationListenerService {
    private static volatile JerryNotificationListener active;

    public static final class ReplyCandidate {
        public final String app;
        public final String title;
        public final String excerpt;
        private final Notification.Action action;
        private final RemoteInput[] remoteInputs;

        private ReplyCandidate(String app, String title, String excerpt,
                               Notification.Action action, RemoteInput[] remoteInputs) {
            this.app = app;
            this.title = title;
            this.excerpt = excerpt;
            this.action = action;
            this.remoteInputs = remoteInputs;
        }
        private boolean send(JerryNotificationListener service, String message) {
            try {
                Bundle results = new Bundle();
                for (RemoteInput input : remoteInputs) {
                    if (input.getAllowFreeFormInput()) results.putCharSequence(input.getResultKey(), message);
                }
                if (results.isEmpty()) return false;
                Intent fillIn = new Intent();
                RemoteInput.addResultsToIntent(remoteInputs, fillIn, results);
                action.actionIntent.send(service, 0, fillIn);
                return true; // OS accepted the action, not delivery confirmation.
            } catch (PendingIntent.CanceledException | SecurityException error) {
                return false;
            }
        }
    }

    public static boolean enabled() { return active != null; }

    @Override public void onListenerConnected() { super.onListenerConnected(); active = this; }
    @Override public void onListenerDisconnected() {
        if (active == this) active = null;
        super.onListenerDisconnected();
    }
    @Override public void onDestroy() {
        if (active == this) active = null;
        super.onDestroy();
    }
    @Override public void onNotificationPosted(StatusBarNotification notification) { }
    @Override public void onNotificationRemoved(StatusBarNotification notification) { }

    private static StatusBarNotification[] latest() {
        JerryNotificationListener listener = active;
        if (listener == null) return new StatusBarNotification[0];
        try {
            StatusBarNotification[] all = listener.getActiveNotifications();
            if (all == null) return new StatusBarNotification[0];
            Arrays.sort(all, Comparator.comparingLong(StatusBarNotification::getPostTime).reversed());
            return all;
        } catch (SecurityException | IllegalStateException exception) {
            return new StatusBarNotification[0];
        }
    }

    private static String label(CharSequence value) {
        if (value == null) return "";
        String s = value.toString().replace("\n", " ").trim();
        return s.length() > 180 ? s.substring(0, 180) + "…" : s;
    }
    public static String summarize() {
        if (!enabled()) {
            return "Enable jerry Independent notification access in Android's Notification Access settings. "
                 + "No notifications are collected until you explicitly grant access.";
        }
        StringBuilder result = new StringBuilder("Current Android notifications:\n");
        int count = 0;
        for (StatusBarNotification sbn : latest()) {
            if (sbn.getPackageName().equals("ai.jeery.assistant.clean")) continue;
            Notification n = sbn.getNotification();
            if (n == null || n.extras == null) continue;
            String title = label(n.extras.getCharSequence(Notification.EXTRA_TITLE));
            String body = label(n.extras.getCharSequence(Notification.EXTRA_TEXT));
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(body)) continue;
            result.append(++count).append(". ").append(sbn.getPackageName())
                    .append(" — ").append(title).append(": ").append(body).append("\n");
            if (count >= 15) break;
        }
        return count == 0 ? "No readable active notifications." : result.toString().trim();
    }
    public static ReplyCandidate findLatestReplyable() {
        for (StatusBarNotification sbn : latest()) {
            Notification n = sbn.getNotification();
            if (n == null || n.actions == null) continue;
            for (Notification.Action action : n.actions) {
                if (action == null || action.actionIntent == null) continue;
                RemoteInput[] inputs = action.getRemoteInputs();
                if (inputs == null || inputs.length == 0) continue;
                boolean freeform = false;
                for (RemoteInput ri : inputs) if (ri.getAllowFreeFormInput()) freeform = true;
                if (freeform) return new ReplyCandidate(sbn.getPackageName(),
                        label(n.extras.getCharSequence(Notification.EXTRA_TITLE)),
                        label(n.extras.getCharSequence(Notification.EXTRA_TEXT)),
                        action, inputs);
            }
        }
        return null;
    }
    public static String sendApprovedReply(ReplyCandidate candidate, String message) {
        JerryNotificationListener listener = active;
        if (listener == null) return "Notification access is not active.";
        if (candidate == null || message == null || message.trim().isEmpty())
            return "No valid reply action or message.";
        return candidate.send(listener, message.trim())
                ? "Android accepted the quick-reply action. Message delivery is not guaranteed."
                : "Android rejected the reply action. This notification may not support replies.";
    }
}
