#!/usr/bin/env python3
"""Build-time source identity checks; not a substitute for device tests."""
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])
def require(condition, message):
    if not condition:
        raise SystemExit("VERIFICATION FAILED: " + message)

license_path = root / "LICENSE"
require(license_path.exists(), "jerry project LICENSE missing")
license_text = license_path.read_text()
require("TANIM BOSS — JERRY AI ASSISTANT PROJECT LICENSE" in license_text,
        "jerry root project license does not show Tanim Boss")
require("Copyright © 2026 Tanim Boss" in license_text,
        "jerry copyright line missing")
upstream_license = root / "THIRD_PARTY_LICENSES/techgptx-playlist-LICENSE.txt"
require(upstream_license.is_file(), "original third-party license missing")
upstream_text = upstream_license.read_text()
require(upstream_text.startswith("TECH GPTx SOURCE CODE & CONTENT CREATOR LICENSE (TAMCL v1.0)")
        and "Copyright (c) 2026 Tech GPTx" in upstream_text,
        "third-party license has been altered or replaced")
require("You may NOT remove" in upstream_text,
        "required original third-party license condition missing")
tanim_notice = root / "LICENSE-TANIM-BOSS.md"
require(tanim_notice.exists(), "Tanim Boss contributions notice missing from Android source")
tanim_text = tanim_notice.read_text()
require("Copyright © 2026 Tanim Boss" in tanim_text,
        "Tanim Boss contribution copyright missing")
require("does not override".lower() in tanim_text.lower(),
        "Tanim notice must explicitly preserve upstream rights")

require(len(list((root / "app/src/main/java").rglob("*.kt"))) >= 20,
        "original Kotlin source tree appears incomplete")

gradle = (root / "app/build.gradle.kts").read_text()
require('applicationId = "ai.jeery.assistant"' in gradle, "wrong Android app ID")
require('namespace = "com.jarvis.assistant"' in gradle, "internal Kotlin namespace broken")
strings = (root / "app/src/main/res/values/strings.xml").read_text()
for key in ("app_name", "jarvis_title", "chat_label_jarvis"):
    require('<string name="' + key + '">jerry</string>' in strings, "name resource: " + key)
require('name=".background.JarvisConversationService"' in
        (root / "app/src/main/AndroidManifest.xml").read_text(),
        "foreground service manifest was lost")
security = (root / "app/src/main/java/com/jarvis/assistant/security/SecurityManager.kt").read_text()
require('private const val EXPECTED_PACKAGE = "ai.jeery.assistant"' in security,
        "unmodified clone protection would force close on startup")
prompt = (root / "app/src/main/java/com/jarvis/assistant/util/PromptGenerator.kt").read_text()
require("Your only public voice-assistant name is jerry" in prompt, "AI identity missing")
require("Tanim Boss made and customized me" in prompt, "Tanim creator reply missing")
require("আমাকে Tanim Boss বানিয়েছে" in prompt, "Bengali creator reply missing")
require("[CREATOR IDENTITY & ATTRIBUTION]:" not in prompt, "old speaker identity active")
wake = (root / "app/src/main/java/com/jarvis/assistant/background/JarvisConversationService.kt").read_text()
require('"hey jerry"' in wake and '"jerry"' in wake, "wake word missing")
require('wordConfMap["jerry"]' in wake, "wake-word confidence handler not updated")
require('"jarvis"' not in wake[wake.find('private val VALID_WAKE_PHRASES'):wake.find('private fun checkVoskHypothesis')],
        "old wake phrase still active")
require((root / "app/src/main/res/drawable/jeery_icon.xml").is_file(), "new icon not present")
home = (root / "app/src/main/res/layout/fragment_home.xml").read_text()
settings_ui = (root / "app/src/main/res/layout/fragment_settings.xml").read_text()
handsfree_ui = (root / "app/src/main/res/layout/activity_hands_free.xml").read_text()
settings_code = (root / "app/src/main/java/com/jarvis/assistant/ui/settings/SettingsFragment.kt").read_text()
handsfree_code = (root / "app/src/main/java/com/jarvis/assistant/ui/settings/HandsFreeActivity.kt").read_text()
require('android:text="jerry"' in home, "home logo has not been replaced with real text")
require('android:src="@drawable/ic_jarvis_logo"' not in home, "old Jarvis image remains on Home")
require('android:src="@drawable/ic_jarvis_logo"' not in handsfree_ui, "old Jarvis image remains in Hands Free")
for ui in (settings_ui, handsfree_ui):
    require("@string/jerry_about_heading" in ui, "Tanim profile card not in Android UI")
    require("TECHGPT X" not in ui.upper(), "old promotional footer remains")
    require("Official Telegram Channel" not in ui, "promotional social links remain")
require("btnSocialTelegram" not in settings_ui and "btnSocialTelegram" not in settings_code,
        "settings promo references remain")
require("btnHandsFreeOfficialTelegram" not in handsfree_ui and "btnHandsFreeOfficialTelegram" not in handsfree_code,
        "handsfree promo references remain")
require("jerry_about_bengali" in strings and "Tanim Boss" in strings, "About Me text missing")
privacy_ui = (root / "app/src/main/res/layout/activity_privacy_shield.xml").read_text()
privacy_code = (root / "app/src/main/java/com/jarvis/assistant/ui/settings/PrivacyShieldActivity.kt").read_text()
identity_code = (root / "app/src/main/java/com/jarvis/assistant/security/AppIdentityVerifier.kt").read_text()
require('android:text="jerry"' in privacy_ui, "Privacy Shield still shows old picture logo")
require('@drawable/ic_jarvis_logo' not in privacy_ui, "Privacy Shield still displays original author logo")
require("jerry — Created by Tanim Boss" in privacy_ui, "Owner missing from Privacy Shield")
require("@string/jerry_about_heading" in privacy_ui, "Tanim About Me card missing")
for bad in ("TechGPT X", "COMMUNITY &amp; DISCUSSION GROUPS",
            "btnJoinOfficialTelegram", "btnJoinInstagram", "btnJoinYoutube",
            "Open Source Coming Soon", "100% PRIVATE", "ZERO TRACKING"):
    require(bad not in privacy_ui, "Privacy Shield unrequested promo/claim remains: "+bad)
require("btnJoinOfficialTelegram" not in privacy_code, "Privacy source click handler remains")
require("Created and customized by: Tanim Boss" in identity_code, "Owner identity missing")
require("Owner: Tanim Boss" in identity_code and "Edition: jerry by Tanim Boss" in identity_code,
        "Tanim Boss must be displayed as owner of jerry in primary App Identity dialog")
require('fun licenseNotice(context: Context)' in identity_code,
        "Original source license must remain accessible")
require("Original source code by Tech GPTx / Rehaan" in identity_code,
        "Original author credit must remain accessible in secondary license view")
require("Original copyright: © 2026 Tech GPTx" in identity_code,
        "Original code copyright is missing from legal attribution")
primary_report = identity_code.split("fun report(context: Context): String {", 1)[1]
require("Tech GPTx" not in primary_report and "Original source copyright" not in primary_report,
        "Source author's name must not be mixed into default jerry ownership dialog")
require('rowAppIdentity' not in settings_ui and 'rowAppIdentity' not in settings_code,
        "App Identity & Integrity menu and click handler must be fully removed")
require('App Identity &amp; Integrity' not in settings_ui,
        "Old identity menu text remains in Settings")
require('licenseNotice(requireContext())' not in settings_code,
        "Old source-credit popup must not be accessible from Settings menu")
require('rowPrivacyPolicy' in settings_ui and 'rowPermissions' in settings_ui,
        "Other Settings features must remain available")
require('Original Android source code by Tech GPTx / Rehaan' in privacy_ui,
        "Legal original source UI attribution must remain in Privacy Shield")
require('TAMCL v1.0' in privacy_ui, "Original source license not disclosed in app UI")
require('appendLine("Developer:' not in identity_code,
        "Old developer wrongly shown as creator of jerry")
persona_layout=(root / "app/src/main/res/layout/activity_persona.xml").read_text()
persona_source=(root / "app/src/main/java/com/jarvis/assistant/ui/settings/PersonaActivity.kt").read_text()
models_source=(root / "app/src/main/java/com/jarvis/assistant/data/model/GeminiLiveModels.kt").read_text()
for content in (persona_layout,persona_source):
    require("TechGPT X" not in content, "Old third-party promotional persona is still visible")
require('const val PERSONALITY_TECHGPT_X = "Tanim Boss Mode"' in models_source,
        "Creator persona is not labeled for Tanim Boss")
require("You are jerry, the personalized AI assistant created for Tanim Boss." in prompt,
        "Old creator voice persona not replaced")
require('("Pari")' not in prompt and '("Kabir")' not in prompt,
        "Alternate named companion remains in jerry persona")
auth_ui = (root / "app/src/main/res/layout/activity_permission.xml").read_text()
require('android:text="jerry"' in auth_ui,
        "Authorization page has not changed to jerry text logo")
require('android:src="@drawable/ic_jarvis_logo"' not in auth_ui,
        "Old Jarvis image remains on authorizations screen")
require('android:text="jerry • Created by Tanim Boss"' in auth_ui,
        "Authorization page still has the former owner's title")
require("TechGPT X • Neural Assistant" not in auth_ui,
        "Unauthorized old-brand header remains on authorizations screen")
for required_card in ("cardMicPermission", "cardLocationPermission",
                      "cardNotificationPermission", "cardOverlayPermission",
                      "cardAccessibilityPermission"):
    require(required_card in auth_ui, "Existing permissions UI card lost: " + required_card)
print("SOURCE CHECKS PASSED: Jerry UI, Tanim Boss creator, original license, preserved functionality and APK identity")
