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
    require('<string name="' + key + '">jeery</string>' in strings, "name resource: " + key)
require('name=".background.JarvisConversationService"' in
        (root / "app/src/main/AndroidManifest.xml").read_text(),
        "foreground service manifest was lost")
security = (root / "app/src/main/java/com/jarvis/assistant/security/SecurityManager.kt").read_text()
require('private const val EXPECTED_PACKAGE = "ai.jeery.assistant"' in security,
        "unmodified clone protection would force close on startup")
prompt = (root / "app/src/main/java/com/jarvis/assistant/util/PromptGenerator.kt").read_text()
require("Your sole assistant name is jeery" in prompt, "AI identity missing")
require("[CREATOR IDENTITY & ATTRIBUTION]:" not in prompt, "old speaker identity active")
wake = (root / "app/src/main/java/com/jarvis/assistant/background/JarvisConversationService.kt").read_text()
require('"hey jerry"' in wake and '"jeery"' in wake, "wake word aliases missing")
require('wordConfMap["jerry"]' in wake, "wake-word confidence handler not updated")
require('"jarvis"' not in wake[wake.find('private val VALID_WAKE_PHRASES'):wake.find('private fun checkVoskHypothesis')],
        "old wake phrase still active")
require((root / "app/src/main/res/drawable/jeery_icon.xml").is_file(), "new icon not present")
print("SOURCE CHECKS PASSED: jeery identity, preserved features, license, wake-word and icons")
