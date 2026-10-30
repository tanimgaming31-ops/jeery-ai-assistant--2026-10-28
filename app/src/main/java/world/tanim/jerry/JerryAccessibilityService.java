package world.tanim.jerry;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import android.hardware.HardwareBuffer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import java.io.OutputStream;

public final class JerryAccessibilityService extends AccessibilityService {
    private static volatile JerryAccessibilityService instance;
    @Override protected void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }
    public static boolean global(int action) {
        JerryAccessibilityService service = instance;
        return service != null && service.performGlobalAction(action);
    }
    public static boolean screenshot() {
        JerryAccessibilityService service = instance;
        if (service == null || Build.VERSION.SDK_INT < 30) return false;
        service.takeScreenshot(Display.DEFAULT_DISPLAY, service.getMainExecutor(),
                new AccessibilityService.TakeScreenshotCallback() {
            @Override public void onSuccess(AccessibilityService.ScreenshotResult image) {
                HardwareBuffer buffer = image.getHardwareBuffer();
                Bitmap hardware = null;
                Bitmap saved = null;
                try {
                    hardware = Bitmap.wrapHardwareBuffer(buffer, image.getColorSpace());
                    if (hardware == null) throw new IllegalStateException("Screenshot unavailable");
                    saved = hardware.copy(Bitmap.Config.ARGB_8888, false);
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME,
                            "jerry_" + System.currentTimeMillis() + ".png");
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/jerry");
                    values.put(MediaStore.Images.Media.IS_PENDING, 1);
                    Uri destination = service.getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (destination == null) throw new IllegalStateException("Gallery destination failed");
                    try (OutputStream out = service.getContentResolver().openOutputStream(destination)) {
                        if (out == null || !saved.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                            throw new IllegalStateException("Could not save screenshot");
                        }
                    }
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    service.getContentResolver().update(destination, values, null, null);
                    toast(service, "Screenshot saved in Pictures/jerry");
                } catch (Exception ex) {
                    toast(service, "Screenshot failed: " + ex.getMessage());
                } finally {
                    if (saved != null) saved.recycle();
                    if (buffer != null) buffer.close();
                }
            }
            @Override public void onFailure(int errorCode) {
                toast(service, "Screenshot rejected by Android: " + errorCode);
            }
        });
        return true;
    }
    /** Execute only after the user explicitly requests the particular action. */
    public static String tapText(String target) {
        JerryAccessibilityService service = instance;
        if (service == null) return "Enable jerry Independent accessibility service first.";
        if (target == null || target.trim().isEmpty()) return "Provide a visible button label to tap.";
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return "Android did not expose an accessible active window.";
        List<AccessibilityNodeInfo> candidates = root.findAccessibilityNodeInfosByText(target);
        if (candidates.isEmpty()) return "No visible element contains that label: " + target;
        for (AccessibilityNodeInfo node : candidates) {
            AccessibilityNodeInfo candidate = node;
            for (int depth = 0; candidate != null && depth < 7; depth++) {
                if (candidate.isVisibleToUser() && candidate.isClickable()) {
                    boolean clicked = candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return clicked ? "Android accepted a tap on a matching visible control."
                                   : "Android rejected the tap action.";
                }
                candidate = candidate.getParent();
            }
        }
        return "Matching text found, but no clickable control is exposed.";
    }

    /** Type only into the actual user-focused editable field. No implicit send. */
    public static String typeIntoFocusedInput(String content) {
        JerryAccessibilityService service = instance;
        if (service == null) return "Enable jerry Independent accessibility service first.";
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return "No accessible active window.";
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused == null || !focused.isEditable()) {
            return "Focus an editable text field in the target app first.";
        }
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, content);
        boolean changed = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        return changed ? "Android accepted text entry in the focused field (not sent)."
                       : "Android blocked setting text in this app.";
    }

    /** A requested gesture, never a claim that a third-party app acted on it. */
    public static String swipe(boolean up) {
        JerryAccessibilityService service = instance;
        if (service == null) return "Enable jerry Independent accessibility service first.";
        DisplayMetrics size = service.getResources().getDisplayMetrics();
        float x = size.widthPixels * 0.50f;
        float y1 = size.heightPixels * (up ? 0.78f : 0.28f);
        float y2 = size.heightPixels * (up ? 0.28f : 0.78f);
        Path path = new Path();
        path.moveTo(x, y1);
        path.lineTo(x, y2);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 430)).build();
        boolean queued = service.dispatchGesture(gesture, null, null);
        return queued ? "Swipe gesture queued with Android. Check the target app."
                      : "Android rejected the swipe gesture.";
    }

    private static void toast(JerryAccessibilityService service, String text) {
        Toast.makeText(service, text, Toast.LENGTH_LONG).show();
    }
}
