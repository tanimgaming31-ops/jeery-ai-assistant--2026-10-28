import unittest
from pathlib import Path

from scripts.prepare_jeery import replace_text, PACKAGE, NAME


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


if __name__ == "__main__":
    unittest.main()
