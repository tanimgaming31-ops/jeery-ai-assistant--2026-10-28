package world.tanim.jerry;

import android.content.Context;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.List;

/**
 * On-device ML Kit scanning for user-selected images.
 * No Gemini key or uploading file content is needed.
 */
public final class LocalVisionTools {
    public interface Callback { void result(String text, boolean success); }
    private LocalVisionTools() { }

    public static void scanBarcode(Context context, Uri uri, Callback cb) {
        try {
            InputImage image = InputImage.fromFilePath(context, uri);
            BarcodeScanner scanner = BarcodeScanning.getClient();
            scanner.process(image)
                .addOnSuccessListener(result -> {
                    StringBuilder output = new StringBuilder();
                    for (Barcode barcode : result) {
                        String value = barcode.getRawValue();
                        if (value != null && !value.isEmpty()) {
                            if (output.length() != 0) output.append("\n");
                            output.append(value);
                        }
                    }
                    cb.result(output.length() == 0
                        ? "No readable QR or barcode found in this image."
                        : "Recognized QR/barcode contents:\n" + output, output.length() > 0);
                })
                .addOnFailureListener(e -> cb.result("Barcode scan error: " + e.getMessage(), false))
                .addOnCompleteListener(task -> scanner.close());
        } catch (Exception error) {
            cb.result("Cannot read selected image: " + error.getMessage(), false);
        }
    }

    public static void extractText(Context context, Uri uri, Callback cb) {
        try {
            InputImage image = InputImage.fromFilePath(context, uri);
            TextRecognizer reader = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            reader.process(image)
                .addOnSuccessListener(result -> {
                    String text = result.getText();
                    if (text == null || text.trim().isEmpty()) {
                        cb.result("No readable Latin-script printed text found in the selected image.", false);
                    } else {
                        cb.result("Text recognized locally on your phone:\n" + text.trim(), true);
                    }
                })
                .addOnFailureListener(e -> cb.result("OCR failed: " + e.getMessage(), false))
                .addOnCompleteListener(task -> reader.close());
        } catch (Exception error) {
            cb.result("Cannot read selected image: " + error.getMessage(), false);
        }
    }
}
