# jerry — Android phone acceptance checklist

Run these tests on the installed `jerry.apk`. A successful GitHub Actions build verifies compilation, **not** full feature operation.

| # | Test | Expected result |
|---|---|---|
| 1 | Install APK | App installs without replacing Maya/Zoya/other apps |
| 2 | Launch jerry | No crash or package/clone error; launcher label is **jeery** |
| 3 | Settings and chat | Only **jerry** is assistant name; legally required original developer credits may remain |
| 4 | Add Gemini API key and Test | Connected/failed accurately reported |
| 5 | Activate microphone | Recording begins; microphone indicator shows |
| 6 | Speak Bengali | AI answers audibly (if model supports Bengali) |
| 7 | Interrupt assistant | AI speech stops; new speech accepted |
| 8 | Background/standby | Foreground service remains per Android restrictions |
| 9 | Say **hey jerry** (also accepts **hey jeery**) | Wake word detected (pronunciation recognized via Vosk) |
| 10 | Store and recall memory | Memory survives restart |
| 11 | Turn torch on/off | Torch really switches |
| 12 | Set volume 30% | Actual system media volume changes |
| 13 | Set brightness 40% | Screen brightness changes if permission granted |
| 14 | Open Chrome | Android launches correct app |
| 15 | Call a saved contact | Right contact resolved; calling consent respected |
| 16 | WhatsApp message draft | Right contact and text prepared; verify recipient before send |
| 17 | Play YouTube video | Requested result really plays, not merely search screen |
| 18 | Start screen sharing | Android consent requested and screen captured |
| 19 | Ask what is on screen | Current UI observed; do not accept stale guesses |
| 20 | Voice tap / scroll / type | Accessibility responds only to approved command |
| 21 | Try floating avatar | Can show/drag/dismiss if permission granted |
| 22 | Stop assistant | Microphone, screen capture and overlay stop appropriately |

**Be cautious with Accessibility and screen-sharing permissions.** The original software's automation flows can fail after third-party app updates. Do not test destructive actions on important accounts.

For any failure send: Android version, selected Gemini model, exact spoken command, result, permission state and a screenshot or error message.

## How to obtain an APK

- **Actions** → **Build jerry Android APK** → open the latest green-check run → **Artifacts** → **jerry-android-apk**.
- If there is a manually triggered successful run, its `jerry.apk` is also in **Releases**.
- APK is a **debug test build**, not phone-tested and not signed for production.
