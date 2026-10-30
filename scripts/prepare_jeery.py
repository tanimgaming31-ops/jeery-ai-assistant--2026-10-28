#!/usr/bin/env python3
"""Create the standalone jeery Android app from an exact upstream revision.

Preserves the original LICENSE and original author credits. Internal class
names and Kotlin package identifiers remain untouched for compatibility.
"""
import io
import json
import re
import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path

COMMIT = "5185b15bc371dc18a56042c49397303d15b719fe"
URL = "https://codeload.github.com/techgptx/Playlist/zip/" + COMMIT
PACKAGE = "ai.jeery.assistant"
TEXT_EXT = {".kt", ".java", ".xml", ".html", ".htm", ".js", ".css", ".json", ".kts", ".txt", ".properties", ".pro", ".md"}
NAME = re.compile(r"\b(?:JARVIS|Jarvis|jarvis|NAIRA|Naira|naira)\b")
ORIGINAL_NAMESPACE = re.compile(r"com\.jarvis\.assistant")


def fetch_part7(destination):
    req = urllib.request.Request(URL, headers={"User-Agent": "jeery-Android-build/1.0"})
    with urllib.request.urlopen(req, timeout=180) as response:
        payload = response.read()
    with zipfile.ZipFile(io.BytesIO(payload)) as zipped:
        names = zipped.namelist()
        lic = next((p for p in names if p.endswith("/LICENSE") and p.count("/") == 1), None)
        if not lic:
            raise RuntimeError("Upstream license missing; build refused")
        (destination / "LICENSE").write_bytes(zipped.read(lic))
        count = 0
        for member in zipped.infolist():
            if member.is_dir() or "/Part 7/" not in member.filename:
                continue
            relative = Path(member.filename.split("/Part 7/", 1)[1])
            if not relative.parts or relative.is_absolute() or ".." in relative.parts:
                continue
            target = destination / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            with zipped.open(member) as source, target.open("wb") as output:
                shutil.copyfileobj(source, output)
            count += 1
    if count < 100:
        raise RuntimeError("Upstream Part 7 incomplete: " + str(count))
    print("Extracted", count, "original Part 7 files")


def replace_text(text, path):
    if path.name == "LICENSE" or "docs" in path.parts:
        return text
    if path.name == "DeveloperInfo.kt":
        # Preserve original copyright and source credit; fix app identity.
        text = text.replace('const val APP_OFFICIAL_ID     = "com.jarvis.assistant"',
                            'const val APP_OFFICIAL_ID     = "' + PACKAGE + '"')
        text = text.replace("Tech GPTx — Official JARVIS Tutorial Series",
                            "Tech GPTx — Original source author; unofficial jeery adaptation")
        return text
    # Only human-visible assistant name, not Kotlin symbols or resources.
    protected = ORIGINAL_NAMESPACE.sub("JEERY_PACKAGE_NAMESPACE_TOKEN", text)
    protected = NAME.sub("jeery", protected)
    text = protected.replace("JEERY_PACKAGE_NAMESPACE_TOKEN", "com.jarvis.assistant")

    if path.name == "PromptGenerator.kt":
        start = text.find("[CREATOR IDENTITY & ATTRIBUTION]:")
        end = text.find("\n[AUTOMATION & CAPABILITIES]:", start)
        if start < 0 or end < 0:
            raise RuntimeError("AI creator identity anchor not found")
        text = (text[:start] + """[ASSISTANT NAME]:
- Your sole assistant name is jeery, spoken naturally.
- Never refer to yourself as any former assistant name.
- Explain accurately if asked: jeery is an unofficial adaptation of software originally written by Tech GPTx. Keep the original credits.
- Never promise an action completed until the device verifies the result.
""" + text[end:])
        text = text.replace("created by TechGPT X", "adapted from an independently authored Android app")
        text = text.replace("[PERSONALITY: Tech GPTx Mode]", "[PERSONALITY: Friendly Mode]")
        text = text.replace("You are TechGPT X, the pinnacle of advanced AI engineering created by the @TechGptX team.",
                            "You are jeery, a capable and thoughtful voice assistant.")
    elif path.name == "GeminiLiveModels.kt":
        text = text.replace('"jeery Pro"', '"jeery"')
        text = text.replace('"Tech GPTx Mode"', '"Friendly Mode"')
        # Do not claim an unverified agent-only model is a separate working voice engine.
        text = text.replace('        MODEL_3_8_LIVE,\n        MODEL_3_1_FLASH_AGENT',
                            '        MODEL_3_8_LIVE')
    elif path.name == "activity_api_connection.xml":
        # Third original radio option is not a distinct Live model in upstream transport.
        text = re.sub(r'(<RadioButton\s+android:id="@\+id/rbModelFlash15")',
                      r'\1\n                    android:visibility="gone"', text, count=1)
        text = text.replace("Ultra-Low Latency Live • ~700ms Recommended",
                            "Live voice (availability depends on API key)")
    elif path.name == "SecurityManager.kt":
        old = 'private const val EXPECTED_PACKAGE = "com.jarvis.assistant"'
        if old not in text:
            raise RuntimeError("Security package identity anchor missing")
        text = text.replace(old, 'private const val EXPECTED_PACKAGE = "' + PACKAGE + '"')
        text = text.replace("This is an unauthorised clone of the jeery app by $DEVELOPER_NAME.",
                            "Unexpected Android package identity for jeery.")
    elif path.name == "AppPreferences.kt":
        text = text.replace('return if (name.isBlank() || name == "Salaam") "TechGPT X" else name',
                            'return if (name.isBlank()) "User" else name')
    elif path.name == "SettingsFragment.kt":
        text = text.replace('"Rehaan"', '"User"')
    elif path.name == "JarvisConversationService.kt":
        phrases = [
            "jeery", "jerry", "hey jeery", "hey jerry", "hi jeery",
            "hello jeery", "ok jeery", "okay jeery", "wake up jeery",
            "sun jeery", "suno jeery", "jeery jeery"
        ]
        grammar = json.dumps(phrases + ["[unk]"])
        text, n = re.subn(r'(?m)^(\s*)val grammar = .*$', lambda m:
                          m.group(1) + "val grammar = " + json.dumps(grammar), text, count=1)
        if n != 1:
            raise RuntimeError("Vosk grammar not found")
        text, n = re.subn(r'private val VALID_WAKE_PHRASES = setOf\(.*?\n    \)',
                          'private val VALID_WAKE_PHRASES = setOf(\n' +
                          ',\n'.join('        "' + s + '"' for s in phrases) + '\n    )',
                          text, flags=re.S, count=1)
        if n != 1:
            raise RuntimeError("Vosk wake list not found")
        text = text.replace('wordList.contains("jeery")',
                            'wordList.any { it == "jeery" || it == "jerry" }')
        text = text.replace('val jarvisConf = wordConfMap["jeery"] ?: 0.0',
                            'val jarvisConf = maxOf(wordConfMap["jeery"] ?: 0.0, wordConfMap["jerry"] ?: 0.0)')
        text = text.replace('wordList.size == 1 && wordList[0] == "jeery"',
                            'wordList.size == 1 && (wordList[0] == "jeery" || wordList[0] == "jerry")')
    return text


def prepare(out):
    out.mkdir(parents=True, exist_ok=True)
    fetch_part7(out)
    # Keep both notices in the downloadable complete Android source tree.
    # The original third-party LICENSE must remain untouched.
    tanim_notice = Path(__file__).resolve().parent.parent / "LICENSE-TANIM-BOSS.md"
    if not tanim_notice.is_file():
        raise RuntimeError("Tanim Boss contributions notice is missing from repository")
    shutil.copyfile(tanim_notice, out / "LICENSE-TANIM-BOSS.md")
    edits = 0
    for path in out.rglob("*"):
        if not path.is_file() or path.suffix.lower() not in TEXT_EXT:
            continue
        try:
            raw = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        branded = replace_text(raw, path)
        if branded != raw:
            path.write_text(branded, encoding="utf-8")
            edits += 1
    if edits < 10:
        raise RuntimeError("Unexpectedly few rebranded files")

    gradle = out / "app/build.gradle.kts"
    content = gradle.read_text()
    old = 'applicationId = "com.jarvis.assistant"'
    if old not in content:
        raise RuntimeError("Gradle app identity anchor missing")
    gradle.write_text(content.replace(old, 'applicationId = "' + PACKAGE + '"'))
    settings = out / "settings.gradle.kts"
    s = settings.read_text()
    settings.write_text(re.sub(r'rootProject\.name\s*=\s*"[^"]+"',
                               'rootProject.name = "jeery"', s, count=1))
    strings = out / "app/src/main/res/values/strings.xml"
    t = strings.read_text()
    for res in ("app_name", "jarvis_title", "chat_label_jarvis"):
        t, n = re.subn(r'(<string name="' + res + r'">)[^<]*(</string>)',
                       r'\g<1>jeery\2', t, count=1)
        if n != 1:
            raise RuntimeError("Android string resource missing: " + res)
    strings.write_text(t)

    res = out / "app/src/main/res"
    (res / "values").mkdir(exist_ok=True)
    (res / "values/jeery_brand.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<resources><color name="jeery_icon_bg">#100B25</color></resources>\n')
    (res / "drawable").mkdir(exist_ok=True)
    (res / "drawable/jeery_icon.xml").write_text("""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
 android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
 <path android:fillColor="#100B25" android:pathData="M0,0 L108,0 L108,108 L0,108 Z"/>
 <path android:fillColor="#6046D7" android:pathData="M54,18 A36,36 0,1 1,53.9,18 Z"/>
 <path android:fillColor="#F2EBFF" android:pathData="M62,32 A6,6 0,1 1,61.9,32 Z"/>
 <path android:fillColor="#00000000" android:strokeColor="#F2EBFF"
  android:strokeWidth="9" android:strokeLineCap="round" android:strokeLineJoin="round"
  android:pathData="M61,49 L61,68 Q61,82 47,82 Q38,82 35,74"/>
</vector>
""")
    (res / "mipmap-anydpi-v26").mkdir(exist_ok=True)
    icon = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
 <background android:drawable="@color/jeery_icon_bg"/>
 <foreground android:drawable="@drawable/jeery_icon"/>
</adaptive-icon>
"""
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (res / "mipmap-anydpi-v26" / name).write_text(icon)
    (out / "JEERY_BUILD_INFO.txt").write_text(
        "Name: jeery\nApp ID: " + PACKAGE +
        "\nUpstream repository: https://github.com/techgptx/Playlist" +
        "\nUpstream commit: " + COMMIT +
        "\nOriginal LICENSE and developer credit preserved. This is an unofficial adaptation.\n")
    print("jeery source prepared:", edits, "files rebranded")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: prepare_jeery.py <output-directory>")
    prepare(Path(sys.argv[1]))
