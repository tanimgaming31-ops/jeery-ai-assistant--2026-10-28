#!/usr/bin/env python3
"""Change *only* the public assistant name on the already-customized V25 app.

Run after prepare_jeery.py, customize_jerry.py and verify_jeery.py.
Do not change Kotlin identifiers, resources, internal package/app ID, icons,
design, memory database, third-party attribution, or license files.
"""
from pathlib import Path
import hashlib
import re
import sys

PACKAGE_ID = "ai.jeery.assistant"
SUPPORTED_TEXT = {".xml", ".kt", ".java", ".html", ".htm", ".js", ".css", ".json"}
NAME = re.compile(r"\b(?:jerry|jeery)\b", flags=re.IGNORECASE)
URL = re.compile(r"https?://[^\s\"'<>]+")
RESOURCE_REF = re.compile(r"@(?:string|drawable|color|id|layout|mipmap|style|xml|anim|array|font)/[A-Za-z0-9_]+")

def checksum(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def display_label(match):
    raw = match.group()
    if raw.isupper():
        return "MAYA"
    if raw[:1].isupper():
        return "Maya"
    return "maya"

def protect_and_rename(original):
    # Tokens are deliberately not words that may be matched by NAME.
    preserved = {}
    def protect(value):
        key = "ZZ_KEEP_TOKEN_%06d_ZZ" % len(preserved)
        preserved[key] = value
        return key

    text = original.replace(PACKAGE_ID, protect(PACKAGE_ID))
    text = URL.sub(lambda m: protect(m.group()), text)
    text = RESOURCE_REF.sub(lambda m: protect(m.group()), text)
    text = NAME.sub(display_label, text)
    for key, value in preserved.items():
        text = text.replace(key, value)
    return text

def rewrite(root):
    app = root / "app" / "src" / "main"
    if not app.is_dir():
        raise SystemExit("Android source tree missing")
    license_file = root / "LICENSE"
    if not license_file.exists():
        raise SystemExit("Original license must remain")
    license_before = checksum(license_file)
    manifest_file = app / "AndroidManifest.xml"
    manifest_before = checksum(manifest_file)
    source_files_before = sorted(str(p.relative_to(app)) for p in app.rglob("*.kt"))
    if len(source_files_before) < 40:
        raise SystemExit("The original V25 Kotlin files appear incomplete")

    edits = []
    for p in app.rglob("*"):
        if not p.is_file() or p.suffix.lower() not in SUPPORTED_TEXT:
            continue
        if p.name == "LICENSE" or p.name.startswith("LICENSE-"):
            continue
        try:
            original = p.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        updated = protect_and_rename(original)
        # The Android installed title is capitalized, while wake-word speech
        # comparisons remain lowercase ("maya", "hey maya").
        if p == app / "res" / "values" / "strings.xml":
            for key in ("app_name", "jarvis_title", "chat_label_jarvis"):
                updated, count = re.subn(
                    rf'(<string name="{key}">)maya(</string>)',
                    r"\g<1>Maya\2", updated, count=1)
                if count != 1:
                    raise SystemExit("Missing name string resource: " + key)
        if p in [
            app / "res" / "layout" / n for n in
            ("fragment_home.xml", "activity_hands_free.xml",
             "activity_privacy_shield.xml", "activity_permission.xml")
        ]:
            updated = updated.replace('android:text="maya"', 'android:text="Maya"')
        if updated != original:
            p.write_text(updated, encoding="utf-8")
            edits.append(str(p.relative_to(root)))

    if len(edits) < 5:
        raise SystemExit("Unexpectedly few name changes")
    if checksum(license_file) != license_before:
        raise SystemExit("The original legal license was changed")
    if checksum(manifest_file) != manifest_before:
        raise SystemExit("The Android manifest was changed; this must be name-only")
    source_files_after = sorted(str(p.relative_to(app)) for p in app.rglob("*.kt"))
    if source_files_before != source_files_after:
        raise SystemExit("Kotlin features removed/renamed")

    expected = (root / "app" / "build.gradle.kts").read_text(encoding="utf-8")
    if 'applicationId = "ai.jeery.assistant"' not in expected:
        raise SystemExit("App ID changed; this would alter the installed app")
    strings = (app / "res/values/strings.xml").read_text(encoding="utf-8")
    if '<string name="app_name">Maya</string>' not in strings:
        raise SystemExit("Installed Android app name did not change")
    prompt = (app / "java/com/jarvis/assistant/util/PromptGenerator.kt").read_text(encoding="utf-8")
    if "Your only public voice-assistant name is maya" not in prompt:
        raise SystemExit("AI speaking name did not change")
    wake = (app / "java/com/jarvis/assistant/background/JarvisConversationService.kt").read_text(encoding="utf-8")
    if '"hey maya"' not in wake or 'wordConfMap["maya"]' not in wake:
        raise SystemExit("Wake-word name not changed")
    if '"hey jerry"' in wake or '"hey jeery"' in wake:
        raise SystemExit("Old wake word still active")

    (root / "MAYA_NAME_ONLY_CHANGE.txt").write_text(
        "Maya — display and voice assistant name only.\n"
        "Derived from the unchanged jerry V25 app layout and Kotlin components.\n"
        "App ID: ai.jeery.assistant (intentionally unchanged).\n"
        "Original license and source credits retained unchanged.\n"
        "Kotlin sources count: " + str(len(source_files_after)) + "\n"
        "Text files rebranded: " + str(len(edits)) + "\n"
        "No device tests were performed.\n", encoding="utf-8")
    print("MAYA NAME CHECK PASSED",
          "files_changed:", len(edits),
          "kotlin_sources_preserved:", len(source_files_after))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: rebrand_maya.py <prepared-android-root>")
    rewrite(Path(sys.argv[1]))
