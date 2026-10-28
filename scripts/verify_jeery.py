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
require(license_path.exists(), "original source LICENSE missing")
license_text = license_path.read_text()
require("Tech GPTx" in license_text and "TAMCL" in license_text,
        "original license/credit modified")
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
for bad in ("TechGPT X", "Tech GPTx", "COMMUNITY &amp; DISCUSSION GROUPS",
            "btnJoinOfficialTelegram", "btnJoinInstagram", "btnJoinYoutube",
            "Open Source Coming Soon", "100% PRIVATE", "ZERO TRACKING"):
    require(bad not in privacy_ui, "Privacy Shield unrequested promo/claim remains: "+bad)
require("btnJoinOfficialTelegram" not in privacy_code, "Privacy source click handler remains")
require("Created and customized by: Tanim Boss" in identity_code, "Owner identity missing")
require("Original code and license credit: Tech GPTx" in identity_code,
        "Required original source UI credit missing")
require('appendLine("Developer:' not in identity_code,
        "Old developer wrongly shown as creator of jerry")
print("SOURCE CHECKS PASSED: Jerry UI, Tanim Boss creator, original license, preserved functionality and APK identity")
