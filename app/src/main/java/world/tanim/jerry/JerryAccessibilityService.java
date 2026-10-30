package world.tanim.jerry;

import android.accessibilityservice.AccessibilityService;
import android.content.ContentValues;
import android.graphics.Bitmap;
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
    private static void toast(JerryAccessibilityService service, String text) {
        Toast.makeText(service, text, Toast.LENGTH_LONG).show();
    }
}
