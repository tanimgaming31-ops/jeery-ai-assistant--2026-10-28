import unittest
from pathlib import Path

from scripts.prepare_jeery import replace_text, PACKAGE, NAME, personalize_gradle_metadata


class JeeryBrandTests(unittest.TestCase):
    def test_internal_identifiers_survive(self):
        content = 'package com.jarvis.assistant\nclass JarvisApp {} // Jarvis speaks\n'
        result = replace_text(content, Path("JarvisApp.kt"))
        self.assertIn("com.jarvis.assistant", result)
        self.assertIn("class JarvisApp", result)
        self.assertIn("// jeery speaks", result)

    def test_security_id_matches_new_app(self):
        old = 'private const val EXPECTED_PACKAGE = "com.jarvis.assistant"'
        result = replace_text(old, Path("SecurityManager.kt"))
        self.assertIn(PACKAGE, result)
        self.assertNotIn('EXPECTED_PACKAGE = "com.jarvis.assistant"', result)

    def test_creator_prompt_and_name(self):
        old = """You are JARVIS.
[CREATOR IDENTITY & ATTRIBUTION]:
- Old brand narrative
[AUTOMATION & CAPABILITIES]:
- inspect screen"""
        result = replace_text(old, Path("PromptGenerator.kt"))
        self.assertIn("Your sole assistant name is jeery", result)
        self.assertIn("[AUTOMATION & CAPABILITIES]", result)
        self.assertNotIn("[CREATOR IDENTITY & ATTRIBUTION]", result)

    def test_wake_word_phonetic_alias_and_confidence(self):
        old = """val grammar = "[\\"jarvis\\",\\"hey jarvis\\"]"
    private val VALID_WAKE_PHRASES = setOf(
        "jarvis",
        "hey jarvis"
    )
    val isMatch = wordList.contains("jarvis")
    val jarvisConf = wordConfMap["jarvis"] ?: 0.0
    if (wordList.size == 1 && wordList[0] == "jarvis") {}"""
        result = replace_text(old, Path("JarvisConversationService.kt"))
        self.assertIn('"hey jerry"', result)
        self.assertIn('wordConfMap["jerry"]', result)
        self.assertIn('wordList.any { it == "jeery" || it == "jerry" }', result)

    def test_license_not_modified(self):
        text = "Copyright Tech GPTx. JARVIS is the original assistant."
        self.assertEqual(replace_text(text, Path("LICENSE")), text)

    def test_persona_brand_changed(self):
        result = replace_text('const val PERSONALITY_ASSISTANT = "Jarvis Pro"\n',
                              Path("GeminiLiveModels.kt"))
        self.assertIn('"jeery"', result)


    def test_jerry_license_ownership_and_upstream_terms(self):
        repo = Path(__file__).resolve().parents[1]
        main = (repo / "LICENSE").read_text(encoding="utf-8")
        notice = (repo / "LICENSE-TANIM-BOSS.md").read_text(encoding="utf-8")
        self.assertTrue(main.startswith("TANIM BOSS — JERRY AI ASSISTANT PROJECT LICENSE"))
        self.assertIn("Copyright © 2026 Tanim Boss", main)
        self.assertIn("6. ORIGINAL UPSTREAM SOURCE LICENSE — REPRODUCED VERBATIM", main)
        self.assertIn("TECH GPTx SOURCE CODE & CONTENT CREATOR LICENSE (TAMCL v1.0)", main)
        self.assertIn('You may NOT remove "Tech GPTx" attribution', main)
        self.assertIn("JERRY AI ASSISTANT — TANIM BOSS", notice)
        self.assertIn("does not override", notice)
        self.assertNotIn("MAYA AI ASSISTANT", main.upper())
        self.assertNotIn("MAYA AI ASSISTANT", notice.upper())

    def test_jerry_app_creator_metadata_preserves_upstream_credit(self):
        upstream_gradle = (
            'val developerName = configuredValue("JARVIS_DEVELOPER_NAME")?.ifBlank { null } ?: "Tech GPTx"\\n'
            'val copyrightOwner = configuredValue("JARVIS_COPYRIGHT_OWNER")?.ifBlank { null } ?: "Tech GPTx"\\n'
        )
        result = personalize_gradle_metadata(upstream_gradle)
        self.assertIn('?: "Tanim Boss"', result)
        self.assertIn('Tanim Boss (original Jerry additions); Tech GPTx (upstream source)', result)
        self.assertNotIn('?: "Tech GPTx"', result)
        with self.assertRaises(RuntimeError):
            personalize_gradle_metadata('not an expected upstream Gradle file')

    def test_docs_point_to_actual_jerry_apk(self):
        repo = Path(__file__).resolve().parents[1]
        workflow = (repo / ".github/workflows/build-jeery.yml").read_text(encoding="utf-8")
        checklist = (repo / "TEST_CHECKLIST.md").read_text(encoding="utf-8")
        self.assertIn("name: Build jerry Android APK", workflow)
        self.assertIn("name: jerry-android-apk", workflow)
        self.assertIn("dist/jerry.apk", workflow)
        self.assertIn("jerry-android-apk", checklist)
        self.assertIn("jerry.apk", checklist)


if __name__ == "__main__":
    unittest.main()
