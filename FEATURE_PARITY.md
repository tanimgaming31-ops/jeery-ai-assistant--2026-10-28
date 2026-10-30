# Independent jerry feature-parity checklist

| Feature | Independent code status | Device validation |
| --- | --- | --- |
| jerry/Tanim Boss identity and dark native UI | Implemented | Not yet tested |
| Gemini text responses with user-provided key | Implemented (REST) | Not yet tested |
| Push-to-talk and speech output | Implemented via Android services | Not yet tested |
| Gemini Live native audio conversation (foreground) | New WebSocket and PCM16 microphone/speaker code using Gemini 3.8 Live; Start/Stop controls | APK compile pending; real phone/API test needed |
| Background Live microphone call (opt-in) | Android 14 microphone foreground service, visible notification and explicit Stop action implemented; no hands-free wake-word | Compile and phone test pending |
| Local memory / JSON import and export | Implemented | Not yet tested |
| Installed-app search and launch | Implemented, launcher apps only | Not yet tested |
| YouTube search in browser/app | Implemented external search | Not yet tested |
| YouTube automatic video playback | Not implemented | Not tested |
| Camera open / flashlight / media volume | Implemented with permission checks | Not yet tested |
| Back / home voice actions | Implemented with user-enabled accessibility | Not yet tested |
| Voice screenshot saved to gallery | Implemented API 30+ with accessibility permission | Not yet tested |
| WhatsApp share draft | Implemented via supported Android share intent | Not yet tested |
| WhatsApp/IMO/Messenger contact lookup and auto-send | Not implemented | Not tested |
| Notification read and approved quick reply | Implemented using optional Android notification listener and per-message confirmation; supported notifications only | Not yet tested on phone |
| Basic notifications/reminders | Implemented; not guaranteed exact timing | Not yet tested |
| Background media, multi-app routines, macros | Not implemented | Not tested |
| User-selected image/screenshot analysis and PDF reading | Implemented via Gemini; requires file picker consent; PDFs limited to first 3 pages | Not yet tested on phone |
| Live screen AI vision and automatic OCR/document scanning | Not implemented | Not tested |
| QR / barcode scanner | Offline ML Kit scanner for user-selected images implemented | Compile and phone test pending |
| OCR from images | Offline ML Kit Latin-text reader for selected images implemented | Compile and phone test pending |
| File manager / clipboard | Android SAF open file, copy response, paste text implemented | Compile and phone test pending |
| 3D avatar / lip sync / emotion engine | Not implemented | Not tested |
| Security camera / face recognition | Not implemented | Not tested |
| Smart home, router, advanced file manager | Not implemented | Not tested |

The earlier app remains on main. This branch contains original independent
code; it does not carry forward the previous app's source or its associated
license. Exact behavior parity is a project goal, not a current claim.

The original app cannot be deleted or replaced on the strength of a passing
APK compile. A true A–Z feature-parity claim requires a per-feature Android
instrumentation and target-phone test report. No such report exists yet.
