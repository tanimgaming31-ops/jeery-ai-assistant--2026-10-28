# jerry — Android AI Voice Assistant

An **unofficial independent build** of the [Tech GPTx Android Assistant Part 7](https://github.com/techgptx/Playlist/tree/main/Part%207), using the personal assistant display name **jerry**. Original author and licensing information must stay intact.

## Android phone installation

Open **Actions → Build jerry Android APK → Run workflow**. When a run succeeds, download the `jerry-android-apk` artifact and extract `jerry.apk`. Any successful build publishes a direct-download test APK to **Releases**.

After installing, enter your own Gemini API key in Settings. Grant permissions individually and test microphone, wake word, accessibility automation and screen sharing on your phone.

**Status:** Build preparation; a successful GitHub Actions build only demonstrates compilation, not guaranteed phone behavior. WhatsApp send, YouTube autoplay, background voice and AI model availability require device tests. No existing Maya, Zoya or other personal assistant project is modified.

Original attribution: © 2026 Tech GPTx / Rehaan. See the upstream [LICENSE](https://github.com/techgptx/Playlist/blob/main/LICENSE), which is preserved in the derived source. This repository is **not** an official Tech GPTx distribution.


## Direct APK link and source

When the GitHub Actions run finishes successfully, open [Releases](https://github.com/tanimgaming31-ops/jeery-ai-assistant--2026-10-28/releases) and download **jerry.apk** from the newest test release. The complete adapted Android source is uploaded as the **jeery-source** artifact in the successful Actions run. Until then, no APK is claimed to be built or tested.

[Android phone feature checklist](TEST_CHECKLIST.md)

## jerry UI and Tanim Boss ownership (2026-10-28)

This is a **real Android XML/Kotlin change**, not a screenshot or visual mock-up.

- The old wordmark image on Home and Hands-Free has been replaced by an editable text header **jerry**.
- Promotional Telegram, Instagram and YouTube community links and creator-branding footer have been removed from the Settings and Hands-Free app screens.
- Both screens now show the in-app **ABOUT ME — TANIM BOSS** card with Bangla and English information provided by the app owner.
- The spoken AI instruction identifies the app as customized and named by **Tanim Boss**. For "Who made you?" it answers in the user's spoken language, including English and Bangla responses, while giving accurate original source attribution when specifically asked.
- **Original developer's license and attribution remain preserved** in LICENSE, the source metadata and the App Identity & Integrity section, as required by the upstream source license.
- **Same Android package ID** `ai.jeery.assistant` remains to avoid creating a second app; changing the display name does not change the package ID.
- The new APK has to be built and independently phone-tested. Because GitHub Actions signs debug APKs with ephemeral keys, an existing installation may reject an in-place update even when package ID matches. **Back up personal data before uninstalling** any installed version.

[New jerry Android Actions builds](https://github.com/tanimgaming31-ops/jeery-ai-assistant--2026-10-28/actions)

## Privacy Shield and ownership cleanup

The application's **real Android Privacy & Data Shield** screen is changed:
- Its image-based former wordmark is replaced with the editable **jerry** header.
- The hero identifies the personal app as **Created by Tanim Boss**.
- The former author's Telegram, Instagram, YouTube and community promotions are removed.
- The incorrect promises of "100% PRIVATE", "ZERO TRACKING" and "Open Source Coming Soon" are replaced with accurate notes about local memory, permissions and Gemini cloud processing.
- **ABOUT ME — TANIM BOSS** is included on the Privacy Shield as well as Settings and Hands-Free pages.
- All named voice personalities remain **jerry**, including affectionate and encouraging conversation styles.
- The App Identity dialog primarily identifies **jerry**, created/customized by **Tanim Boss**. Its distinct **Original source/license credit** remains because the original TAMCL license explicitly prohibits removing author attribution from UI credits.
- Kotlin internal file/class identifiers and the Android app ID remain unchanged to preserve functionality.

**Android signing note:** Each GitHub Actions debug APK may use a different signature. Before uninstalling a previous build, export important memories/settings where possible, because uninstalling deletes local app data. A different signing key generally prevents an in-place update.

