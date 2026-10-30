package world.tanim.jerry;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.provider.ContactsContract;
import java.util.ArrayList;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

/** Only Android APIs with explicit user permissions or user-visible intents. */
public final class PhoneActions {
    private PhoneActions() { }
    public static String execute(Activity activity, String input) {
        String text = input.trim();
        String low = text.toLowerCase(Locale.ROOT);
        try {
            if (low.equals("go home") || low.equals("home screen")) {
                return JerryAccessibilityService.global(AccessibilityService.GLOBAL_ACTION_HOME)
                        ? "Home screen action sent." : accessibilityRequired(activity);
            }
            if (low.equals("go back") || low.equals("back")) {
                return JerryAccessibilityService.global(AccessibilityService.GLOBAL_ACTION_BACK)
                        ? "Back action sent." : accessibilityRequired(activity);
            }
            if (low.equals("take screenshot") || low.equals("screenshot")
                    || low.equals("স্ক্রিনশট")) {
                return JerryAccessibilityService.screenshot()
                        ? "Taking a screenshot; check gallery notification."
                        : accessibilityRequired(activity) + " Android 11+ is required.";
            }
            if (low.equals("torch on") || low.equals("flashlight on")
                    || low.equals("torch off") || low.equals("flashlight off")) {
                if (Build.VERSION.SDK_INT >= 23 &&
                    activity.checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, 50);
                    return "Allow Camera permission and repeat torch command.";
                }
                boolean on = low.endsWith("on");
                CameraManager cameras = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                for (String id : cameras.getCameraIdList()) {
                    Boolean hasFlash = cameras.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (Boolean.TRUE.equals(hasFlash)) {
                        cameras.setTorchMode(id, on);
                        return on ? "Flashlight enabled." : "Flashlight disabled.";
                    }
                }
                return "No available flashlight.";
            }
            if (low.matches("(?:set )?volume \\d{1,3}%?")) {
                String digits = low.replaceAll("[^0-9]", "");
                int pct = Integer.parseInt(digits);
                if (pct > 100) return "Volume percentage must be between 0 and 100.";
                AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                int maximum = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audio.setStreamVolume(AudioManager.STREAM_MUSIC,
                        Math.round(maximum * pct / 100f), AudioManager.FLAG_SHOW_UI);
                int now = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                return "Media volume set to " + Math.round(100f * now / maximum) + "%.";
            }
            if (low.matches("(?:set )?brightness \\d{1,3}%?")) {
                int pct = Integer.parseInt(low.replaceAll("[^0-9]", ""));
                if (pct > 100) return "Brightness percentage must be between 0 and 100.";
                if (!Settings.System.canWrite(activity)) {
                    Intent permission = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                            Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(permission);
                    return "Grant Modify system settings to jerry and repeat the brightness command.";
                }
                Settings.System.putInt(activity.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                boolean wrote = Settings.System.putInt(activity.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, Math.round(255f * pct / 100));
                if (!wrote) return "Android refused to update brightness.";
                int saved = Settings.System.getInt(activity.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, -1);
                return "Brightness set to approximately " + Math.round(saved * 100f / 255f) + "%.";
            }
            if (low.startsWith("call ") && low.length() > 5) {
                return openContactDialer(activity, text.substring(5).trim());
            }
            if (low.equals("volume up") || low.equals("volume down")) {
                AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        low.endsWith("up") ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI);
                return "Media volume updated.";
            }
            if (low.equals("open camera")) {
                activity.startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
                return "Opening camera.";
            }
            if (low.startsWith("youtube search ")) {
                launchUrl(activity, "https://www.youtube.com/results?search_query="
                        + URLEncoder.encode(text.substring(15), "UTF-8"));
                return "Opening YouTube search results (not autoplay).";
            }
            if (low.startsWith("search ")) {
                launchUrl(activity, "https://www.google.com/search?q="
                        + URLEncoder.encode(text.substring(7), "UTF-8"));
                return "Opening Google search results.";
            }
            if (low.startsWith("whatsapp message ")) {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, text.substring(17));
                share.setPackage("com.whatsapp");
                activity.startActivity(share);
                return "WhatsApp draft opened. Review recipient and tap Send yourself.";
            }
            if (low.startsWith("open ")) {
                String app = low.substring(5).trim();
                if (app.equals("accessibility settings")) {
                    activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    return "Opening Android accessibility settings.";
                }
                if (app.equals("notification settings")) {
                    activity.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName()));
                    return "Opening notification settings.";
                }
                if (app.equals("youtube")) {
                    Intent youtube = activity.getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
                    if (youtube != null) { activity.startActivity(youtube); return "Opening YouTube."; }
                }
                return launchByName(activity, app);
            }
        } catch (ActivityNotFoundException | SecurityException ex) {
            return "Android could not open that action: " + ex.getMessage();
        } catch (Exception ex) {
            return "Phone action failed: " + ex.getClass().getSimpleName()
                    + " — " + ex.getMessage();
        }
        return null;
    }
    private static String openContactDialer(Activity activity, String query) {
        if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 116);
            return "Allow Contacts permission and repeat the call command.";
        }
        ArrayList<String> display = new ArrayList<>();
        ArrayList<String> numbers = new ArrayList<>();
        try (Cursor cursor = activity.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + query + "%"},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC")) {
            if (cursor == null) return "Contacts provider unavailable.";
            while (cursor.moveToNext() && display.size() < 20) {
                String name = cursor.getString(0);
                String number = cursor.getString(1);
                if (name != null && number != null && !number.isEmpty()) {
                    display.add(name + " — " + number);
                    numbers.add(number);
                }
            }
        } catch (SecurityException error) {
            return "Android denied access to contacts.";
        }
        if (display.isEmpty()) return "No contact found with name: " + query;
        activity.runOnUiThread(() ->
            new AlertDialog.Builder(activity)
                .setTitle("Choose contact to dial")
                .setItems(display.toArray(new String[0]), (dialog, which) -> {
                    Intent dialer = new Intent(Intent.ACTION_DIAL,
                            Uri.fromParts("tel", numbers.get(which), null));
                    activity.startActivity(dialer);
                })
                .setNegativeButton("Cancel", null)
                .show());
        return "Found " + display.size() + " matching contacts. Select one to open the dialer; "
                + "Android will NOT automatically place the call.";
    }
    private static String accessibilityRequired(Activity activity) {
        activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        return "Enable jerry Independent accessibility service in Android settings first.";
    }
    private static void launchUrl(Activity activity, String url) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
    private static String launchByName(Activity activity, String app) {
        PackageManager pm = activity.getPackageManager();
        Intent filter = new Intent(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(filter, 0);
        String partial = null;
        for (ResolveInfo item : apps) {
            String name = item.loadLabel(pm).toString().toLowerCase(Locale.ROOT);
            String pkg = item.activityInfo.packageName;
            if (name.equals(app) || pkg.equals(app)) {
                Intent intent = pm.getLaunchIntentForPackage(pkg);
                if (intent != null) {
                    activity.startActivity(intent);
                    return "Opening " + item.loadLabel(pm) + ".";
                }
            }
            if (partial == null && name.contains(app)) partial = pkg;
        }
        if (partial != null) {
            Intent intent = pm.getLaunchIntentForPackage(partial);
            if (intent != null) {
                activity.startActivity(intent);
                return "Opening matching installed app.";
            }
        }
        return "App not found among Android-visible installed launcher apps: " + app;
    }
}
