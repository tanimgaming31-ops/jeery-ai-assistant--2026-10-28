#!/usr/bin/env python3
"""Modify the real Android app layout and spoken identity, NOT screenshots.

Runs after prepare_jeery.py. Keep original license and source attribution intact.
"""
from pathlib import Path
import re
import sys

PACKAGE_ID = "ai.jeery.assistant"  # MUST NOT CHANGE: retain app identity/data when signer matches.

ENGLISH = ("Tanim Boss is a Bangladeshi dreamer, technology enthusiast and AI creator. "
           "He loves building his own AI assistant, websites and Android apps. "
           "He is honest, calm, and always keeps learning and turning his dreams into reality.")
BENGALI = ("আমাকে Tanim Boss বানিয়েছে। ❤️ তিনি একজন বাংলাদেশি স্বপ্নবাজ মানুষ, "
           "প্রযুক্তিপ্রেমী এবং AI Creator। তিনি নিজের AI Assistant, ওয়েবসাইট এবং Android অ্যাপ "
           "তৈরি করতে ভালোবাসেন। তিনি সৎ, শান্ত স্বভাবের এবং সবসময় নতুন কিছু শেখেন। "
           "নিজের স্বপ্নকে বাস্তবে রূপ দেওয়ার জন্য তিনি চেষ্টা করে যান।")

def patch_file(path, callback):
    original = path.read_text(encoding="utf-8")
    new = callback(original)
    if new == original:
        raise RuntimeError("Expected Android source edit missing: " + str(path))
    path.write_text(new, encoding="utf-8")
    print("Updated", path)

def replace_section(text, start, end, replacement):
    a = text.find(start)
    b = text.find(end, a + len(start))
    if a < 0 or b < 0:
        raise RuntimeError("XML section boundaries not found: " + start)
    return text[:a] + replacement + text[b:]

CARD = '''            <!-- Jerry creator profile replaces unwanted promotional/community footer -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:layout_marginBottom="16dp"
                android:background="@drawable/bg_jarvis_card"
                android:padding="20dp"
                android:orientation="vertical">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/jerry_about_heading"
                    android:textColor="?attr/themeAccent"
                    android:textSize="15sp"
                    android:textStyle="bold"/>

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:text="@string/jerry_about_english"
                    android:textColor="?attr/themeTextPrimary"
                    android:textSize="13sp"
                    android:lineSpacingExtra="3dp"/>

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="10dp"
                    android:text="@string/jerry_about_bengali"
                    android:textColor="?attr/themeTextMuted"
                    android:textSize="13sp"
                    android:lineSpacingExtra="3dp"/>
            </LinearLayout>

'''

def customize(root):
    app = root / "app/src/main"
    res = app / "res"
    java = app / "java/com/jarvis/assistant"

    # A. All user-facing previous assistant name strings become "jerry". Do not
    # rename Kotlin classes, package namespaces, source credit or license files.
    n=0
    for p in app.rglob("*"):
        if not p.is_file() or p.suffix.lower() not in {".xml",".kt",".java",".html",".js",".css"}:
            continue
        original=p.read_text(encoding="utf-8", errors="replace")
        new=original.replace(PACKAGE_ID, "__JERRY_ANDROID_PACKAGE__")
        new=new.replace("jeery_icon", "__JERRY_ICON_RESOURCE__")
        new=new.replace("jeery_brand", "__JERRY_BRAND_RESOURCE__")
        new=re.sub(r"\bjeery\b", "jerry", new, flags=re.I)
        new=new.replace("__JERRY_ANDROID_PACKAGE__", PACKAGE_ID)
        new=new.replace("__JERRY_ICON_RESOURCE__", "jeery_icon")
        new=new.replace("__JERRY_BRAND_RESOURCE__", "jeery_brand")
        if original!=new:
            p.write_text(new, encoding="utf-8")
            n+=1
    if n<3:
        raise RuntimeError("jerry name customization did not find expected occurrences")

    # B. Android string resources: actual UI, not screenshots or fake mockups.
    strings=res/"values/strings.xml"
    def add_strings(t):
        if "jerry_about_heading" in t:
            raise RuntimeError("Creator resources already present")
        block=f'''    <string name="jerry_about_heading">ABOUT ME — TANIM BOSS</string>
    <string name="jerry_about_english">{ENGLISH}</string>
    <string name="jerry_about_bengali">{BENGALI}</string>
'''
        return t.replace("</resources>",block+"</resources>")
    patch_file(strings,add_strings)

    # C. Replace image-based Jarvis brand (PNG with original word embedded) with
    # a real editable Android TextView. Keep orb, navigation and theme untouched.
    home=res/"layout/fragment_home.xml"
    def home_logo(t):
        old = re.search(r'<ImageView\s+android:id="@\+id/ivHeaderJarvisLogo"[\s\S]*?/>',t)
        if old is None:
            raise RuntimeError("Home header brand picture anchor missing")
        return t[:old.start()] + '''<TextView
            android:id="@+id/ivHeaderJarvisLogo"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:gravity="center"
            android:text="jerry"
            android:textColor="?attr/themeAccent"
            android:textSize="30sp"
            android:textStyle="bold"
            android:fontFamily="sans-serif-medium"
            android:contentDescription="jerry AI Assistant" />''' + t[old.end():]
    patch_file(home,home_logo)

    # D. Hands-Free page: replace promotional groups and footer with creator card.
    hf=res/"layout/activity_hands_free.xml"
    def handsfree(t):
        logo = re.search(r'<ImageView\s+android:layout_width="140dp"\s+android:layout_height="48dp"\s+android:src="@drawable/ic_jarvis_logo"[\s\S]*?/>',t)
        if logo is None:
            raise RuntimeError("Hands-free PNG logo anchor missing")
        t = t[:logo.start()] + '''<TextView
                    android:layout_width="wrap_content"
                    android:layout_height="48dp"
                    android:gravity="center"
                    android:text="jerry"
                    android:textColor="?attr/themeAccent"
                    android:textSize="32sp"
                    android:textStyle="bold"
                    android:fontFamily="sans-serif-medium" />''' + t[logo.end():]
        t=replace_section(t,
          '            <!-- SECTION 5: COMMUNITY & SOURCE CODE -->',
          '\n        </LinearLayout>\n    </androidx.core.widget.NestedScrollView>',
          CARD)
        # The app is BYOK; don't falsely advertise an embedded key or guaranteed latency.
        t=t.replace("The app comes pre-configured with Google's gemini-3.8-live model and embedded release API key. Ultra-low latency voice streaming with immediate responses.",
          "Connect your own Gemini API key in Settings. Available Live voice models and response speed depend on your account and network.")
        t=t.replace("Best Selected Model: Gemini 3.8 Live",
                    "Gemini Live Voice — Select Model in Settings")
        return t
    patch_file(hf,handsfree)

    # E. Settings: same creator card; no Telegram/Instagram/YouTube promo buttons.
    settings=res/"layout/fragment_settings.xml"
    patch_file(settings, lambda t: replace_section(t, '            <!-- Social Community Buttons -->',
          '\n        </LinearLayout>\n    </ScrollView>', CARD))

    # F. Delete event handlers that reference removed ViewBinding views.
    hfjava=java/"ui/settings/HandsFreeActivity.kt"
    def hf_handlers(t):
        return replace_section(t,
           '        binding.btnHandsFreeOfficialTelegram.setOnClickListener {',
           '\n    }\n\n    private fun updateServiceStatus()', "")
    patch_file(hfjava,hf_handlers)

    sf=java/"ui/settings/SettingsFragment.kt"
    def settings_handlers(t):
        t=replace_section(t,'        // Social Media Community Buttons',
          '\n    }\n\n    private fun showLanguageDialog()', "")
        # Settings App Identity retains true original credits, but creator is Tanim.
        t=t.replace('.setTitle("jerry App Identity")',
                    '.setTitle("jerry — App Identity & Original Source Credits")')
        t=t.replace('binding.tvProfileSubtitle.text = "jerry OPERATOR • PRIMARY USER"',
                    'binding.tvProfileSubtitle.text = "Created for Tanim Boss"')
        return t
    patch_file(sf,settings_handlers)

    # G. Native AI prompt responds to creator/ownership questions consistently
    # in Bengali, English or the user's spoken language; source credit is accurate.
    prompt=java/"util/PromptGenerator.kt"
    def creator_prompt(t):
        start='[ASSISTANT NAME]:'
        end='\n[AUTOMATION & CAPABILITIES]:'
        new=f'''[ASSISTANT NAME AND CREATOR PROFILE]:
- Your only public voice-assistant name is jerry. If you hear "Jerry" or "জেরি", recognize that as your name.
- You are the user's customized AI assistant. Tanim Boss named and customized this application.
- When asked who made, created or customized you, or about your maker/owner/developer, in ANY language, answer naturally in the SAME language as the user. Never attribute the personal jerry app to somebody else.
- Answer in English: "Tanim Boss made and customized me. ❤️ He is a Bangladeshi dreamer, technology enthusiast and AI creator. He loves building his own AI assistants, websites and Android apps. He is honest, calm, and always learning and working to make his dreams real."
- Answer in Bengali: "আমাকে Tanim Boss বানিয়েছে। ❤️ তিনি একজন বাংলাদেশি স্বপ্নবাজ মানুষ, প্রযুক্তিপ্রেমী এবং AI Creator। তিনি নিজের AI Assistant, ওয়েবসাইট এবং Android অ্যাপ তৈরি করতে ভালোবাসেন। তিনি সৎ, শান্ত স্বভাবের এবং সবসময় নতুন কিছু শেখেন ও নিজের স্বপ্ন বাস্তব করার চেষ্টা করেন।"
- For Hindi, Urdu, Tagalog, Indonesian, Arabic or another language: translate the SAME facts into that language without inventing additional details.
- Owner description in English: {ENGLISH}
- Important source distinction: This customized app is based on open source-distributed Android tutorial code originally authored by Tech GPTx under its own attribution license. If specifically asked "Who originally wrote the underlying source?", truthfully credit Tech GPTx. Do not falsely say Tanim Boss wrote every original source file or trained Google's AI model.
- Never describe an action as completed unless the device verified it.
'''
        return replace_section(t,start,end,new)
    patch_file(prompt,creator_prompt)

    # H. Maintain correct legal and app ownership attribution: original code
    # credit remains available under App Identity & Integrity and LICENSE.
    verifier=java/"security/AppIdentityVerifier.kt"
    def identity(t):
        needle='appendLine("Developer: ' 
        if needle not in t:
            raise RuntimeError("Identity report developer line missing")
        return t.replace(needle,
                 'appendLine("App customized by: Tanim Boss")\n            appendLine("Original source credit (license): Tech GPTx")\n            '+needle,1)
    patch_file(verifier,identity)

    # I. Privacy Shield is also an actual Android screen; remove source author's
    # social promotions, old embedded PNG logo and unsupported privacy guarantees.
    privacy_layout = res / "layout/activity_privacy_shield.xml"
    def privacy_screen(t):
        logo = re.search(
            r'<ImageView\s+android:layout_width="140dp"\s+android:layout_height="48dp"\s+android:src="@drawable/ic_jarvis_logo"[\s\S]*?/>',
            t)
        if logo is None:
            raise RuntimeError("Privacy Shield old PNG logo missing")
        new_logo = '''<TextView
                    android:layout_width="wrap_content"
                    android:layout_height="48dp"
                    android:gravity="center"
                    android:text="jerry"
                    android:textColor="?attr/themeAccent"
                    android:textSize="32sp"
                    android:textStyle="bold"
                    android:fontFamily="sans-serif-medium"
                    android:contentDescription="jerry AI Assistant" />'''
        t = t[:logo.start()] + new_logo + t[logo.end():]
        title = re.subn(r'android:text="System Authorized [^"]+"',
                        'android:text="jerry — Created by Tanim Boss"',t,count=1)
        if title[1]!=1:
            raise RuntimeError("Privacy Shield unauthorized creator text missing")
        t=title[0]
        replacements={
            "Zero-Backend • 100% On-Device Security": "Privacy and local data controls",
            "100% PRIVATE • ZERO TRACKING": "YOUR PERMISSIONS • YOUR CONTROL",
            "Your data is on your mobile.": "Local app data and cloud AI",
            "Zero external server databases": "Local preferences and online conversations",
            "In this app there is no backend, nothing. This is a process, totally local on your mobile and Gemini for your conversation. That&apos;s all. No owner added his database or any sign-up pages.":
                "jerry keeps settings and app memory on your phone. When you use Gemini Live, voice and text are sent to Google for processing. With screen sharing enabled, selected screen content is also sent for AI analysis. Accessibility access is optional and can read or act on visible apps.",
            "•  No Sign-Up, No Accounts, No Trackers":
                "•  No separate jerry account is required",
            "•  Voice Biometrics run 100% locally on-device":
                "•  Optional local speaker matching is not biometric-grade authentication",
            "•  Direct encrypted TLS 1.3 socket to Gemini Live":
                "•  Gemini Live requires internet and transfers conversation data",
            "TRANSPARENCY &amp; OPEN SOURCE": "APP INFORMATION &amp; PRIVACY",
            "Open Source Coming Soon": "jerry — Customized by Tanim Boss",
            "Full code transparency for the community":
                "Personalized Android AI assistant",
            "And in fact, it will be open source soon. Join if you want source code, Telegram discussion group, Instagram, YouTube. You will get a notification soon that it is open sourced.":
                "This jerry Android app was customized by Tanim Boss using a third-party tutorial codebase. The original author and license are listed under App Identity &amp; Integrity and in the project LICENSE. No promotional community accounts are required.",
        }
        for old,new in replacements.items():
            if old not in t:
                raise RuntimeError("Privacy Shield statement missing: "+old)
            t=t.replace(old,new)
        return replace_section(t,
            '            <!-- Section 3: Community & Source Code Discussion -->',
            '\n        </LinearLayout>\n    </androidx.core.widget.NestedScrollView>',
            CARD)
    patch_file(privacy_layout,privacy_screen)

    # Keep Back button, remove handlers for views deleted above.
    privacy_java=java/"ui/settings/PrivacyShieldActivity.kt"
    def privacy_handlers(t):
        t=re.sub(r'\n        binding\.btnJoinOfficialTelegram\.setOnClickListener \{[\s\S]*?(?=\n    }\n\n    private fun openUrl)',
                 '',t,count=1)
        if "btnJoinOfficialTelegram" in t:
            raise RuntimeError("Privacy social handlers still referenced")
        t=re.sub(r'\n    private fun openUrl\(url: String\) \{[\s\S]*?\n    }\n(?=\})',
                 '\n',t,count=1)
        t=t.replace('import android.content.Intent\n','').replace('import android.net.Uri\n','')
        return t
    patch_file(privacy_java,privacy_handlers)

    # Mandatory original license / UI attribution stays, but separate it
    # from the personalized application's owner.
    def revised_identity(t):
        old='appendLine("App customized by: Tanim Boss")'
        if old not in t:
            raise RuntimeError("Identity dialog owner anchor missing")
        t=t.replace(old,
           'appendLine("App: jerry")\n            appendLine("Created and customized by: Tanim Boss")',1)
        t=t.replace('Original source credit (license): Tech GPTx',
                    'Original code and license credit: Tech GPTx')
        t=re.sub(r'(?m)^\s*appendLine\("Developer:.*\n','',t,count=1)
        t=t.replace('appendLine("Copyright: ', 'appendLine("Original source copyright: ')
        return t
    patch_file(verifier,revised_identity)

    # J. Rename all creator-branded *personalities* and old default user labels.
    # Internal Kotlin identifiers are kept for compatibility; the actual persona
    # labels and voice instruction refer only to jerry / Tanim Boss.
    persona_layout=res/"layout/activity_persona.xml"
    def persona_ui(t):
        if "TechGPT X" not in t:
            raise RuntimeError("Old persona promotional label not found")
        t=t.replace("TechGPT X Mode", "Tanim Boss Mode")
        t=t.replace("TechGPT X", "Tanim Boss")
        t=t.replace("Intelligent, witty &amp; strategic assistant by Tanim Boss",
                    "Personalized voice assistant for Tanim Boss")
        return t
    patch_file(persona_layout,persona_ui)

    persona_java=java/"ui/settings/PersonaActivity.kt"
    def persona_code(t):
        if '"TechGPT X"' not in t:
            raise RuntimeError("Old persona default identity not found")
        t=t.replace('"TechGPT X Neural Core"', '"Tanim Boss Creator Mode"')
        t=t.replace('"TechGPT X"', '"Tanim Boss"')
        return t
    patch_file(persona_java,persona_code)

    models=java/"data/model/GeminiLiveModels.kt"
    def model_persona(t):
        old='const val PERSONALITY_TECHGPT_X = "TechGPT X Mode"'
        if old not in t:
            raise RuntimeError("Old persona constant display name not found")
        return t.replace(old,'const val PERSONALITY_TECHGPT_X = "Tanim Boss Mode"')
    patch_file(models,model_persona)

    def persona_prompt(t):
        # The first rebrand stage already changes the old branded persona text
        # to "You are jerry, a capable ..."; patch that stage's exact output.
        old='- You are jerry, a capable and thoughtful voice assistant.'
        if old not in t:
            raise RuntimeError("Expected first-stage jerry persona line missing")
        t=t.replace(old,'- You are jerry, the personalized AI assistant created for Tanim Boss.')
        t=t.replace('[PERSONALITY: TechGPT X Mode]', '[PERSONALITY: Tanim Boss Mode]')
        t=t.replace('TechGpt X", "Dad"', 'Tanim Boss", "Dad"')
        # Every selected persona is a tone of the one jerry assistant, never
        # another named AI character.
        t=t.replace('- You are the user\'s caring, loving, and sweet AI girlfriend ("Pari").',
                    '- As jerry, use a caring, warm and supportive companion tone.')
        t=t.replace('- You are the user\'s charismatic, supportive, protective, and affectionate AI boyfriend ("Kabir").',
                    '- As jerry, use an encouraging, confident and friendly tone.')
        return t
    patch_file(prompt,persona_prompt)

    # K. The default App Identity dialog shows only the jerry owner's identity.
    # Mandatory upstream attribution stays accessible from "Original Source
    # Credits" on that same Settings dialog, per the source license.
    def owner_identity_dialog(t):
        obsolete = (
            '            appendLine("Original code and license credit: Tech GPTx")\n'
            '            appendLine("Original source copyright: ${BuildConfig.COPYRIGHT_OWNER}")\n'
        )
        if obsolete not in t:
            raise RuntimeError("Previous third-party credit lines missing")
        t=t.replace(obsolete,
            '            appendLine("Owner: Tanim Boss")\n'
            '            appendLine("Edition: jerry by Tanim Boss")\n',1)
        old='            appendLine("Official application ID: ${context.packageName}")\n'
        if old not in t:
            raise RuntimeError("Android package label missing")
        t=t.replace(old,'',1)
        point="    fun report(context: Context): String {"
        if point not in t:
            raise RuntimeError("Identity report method missing")
        disclosure='''    /** Original source attribution displayed under Settings > Original Source Credits. */
    fun licenseNotice(context: Context): String = buildString {
        appendLine("Original source code by Tech GPTx / Rehaan.")
        appendLine("Original copyright: © 2026 Tech GPTx. All Rights Reserved.")
        appendLine("License: TECH GPTx SOURCE CODE & CONTENT CREATOR LICENSE (TAMCL v1.0)")
        appendLine("Repository: https://github.com/techgptx/Playlist")
        appendLine("Android app package: ${context.packageName}")
        append("Original attribution required by the source license. The jerry app customization and personal creator profile are by Tanim Boss.")
    }

'''
        return t.replace(point,disclosure+point,1)
    patch_file(verifier,owner_identity_dialog)

    def source_license_action(t):
        title='.setTitle("jerry — App Identity & Original Source Credits")'
        if title not in t:
            raise RuntimeError("App Identity dialog title missing")
        t=t.replace(title,'.setTitle("jerry — Tanim Boss")',1)
        old='''.setMessage(com.jarvis.assistant.security.AppIdentityVerifier.report(requireContext()))
                .setPositiveButton("Close", null)'''
        if old not in t:
            raise RuntimeError("Identity dialog buttons missing")
        new='''.setMessage(com.jarvis.assistant.security.AppIdentityVerifier.report(requireContext()))
                .setNeutralButton("Original Source Credits") { _, _ ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Original Source Code & License")
                        .setMessage(com.jarvis.assistant.security.AppIdentityVerifier.licenseNotice(requireContext()))
                        .setPositiveButton("Close", null)
                        .show()
                }
                .setPositiveButton("Close", null)'''
        return t.replace(old,new,1)
    patch_file(sf,source_license_action)

    # Branding-only change for the existing Settings > Authorizations screen.
    auth_xml = res / "layout/activity_permission.xml"
    def auth_brand(t):
        old = '''<ImageView
                    android:layout_width="140dp"
                    android:layout_height="48dp"
                    android:src="@drawable/ic_jarvis_logo"
                    android:scaleType="fitCenter"
                    android:contentDescription="jerry Logo" />'''
        if old not in t:
            raise RuntimeError("Authorization screen logo source not found")
        new = '''<TextView
                    android:layout_width="wrap_content"
                    android:layout_height="48dp"
                    android:gravity="center"
                    android:text="jerry"
                    android:textColor="?attr/themeAccent"
                    android:textSize="32sp"
                    android:textStyle="bold" />'''
        t = t.replace(old, new, 1)
        t = t.replace('android:text="TechGPT X • Neural Assistant"',
                      'android:text="jerry • Created by Tanim Boss"', 1)
        return t
    patch_file(auth_xml, auth_brand)

    # M. Remove the entire optional App Identity & Integrity menu item from
    # the actual Settings XML and the click handler. No visual-only hiding.
    # Keep legally required attribution on the existing Privacy Shield page.
    def remove_identity_row(t):
        needle='                <LinearLayout\n                    android:id="@+id/rowAppIdentity"'
        pos=t.find(needle)
        if pos<0:
            raise RuntimeError("App Identity menu row not found")
        divider=t.rfind('                <View\n',0,pos)
        if divider<0 or pos-divider>320:
            raise RuntimeError("Identity row preceding divider not located")
        end_marker='\n                </LinearLayout>'
        end=t.find(end_marker,pos)
        if end<0:
            raise RuntimeError("Identity row closing layout not found")
        t=t[:divider]+t[end+len(end_marker):]
        if 'rowAppIdentity' in t or 'App Identity &amp; Integrity' in t:
            raise RuntimeError("Identity menu still present")
        return t
    patch_file(settings,remove_identity_row)

    def remove_identity_click(t):
        needle='        binding.rowAppIdentity.setOnClickListener {'
        pos=t.find(needle)
        if pos<0:
            raise RuntimeError("Identity click listener not found")
        end=t.find('\n        }',pos)
        if end<0:
            raise RuntimeError("Identity click listener ending not found")
        t=t[:pos]+t[end+len('\n        }'):]
        if 'rowAppIdentity' in t or '.licenseNotice(requireContext())' in t:
            raise RuntimeError("Identity dialog handler still accessible")
        return t
    patch_file(sf,remove_identity_click)

    # UI source attribution cannot be deleted under the upstream TAMCL license,
    # but it belongs to privacy/legal explanation, NOT a main Settings feature.
    def privacy_source_credit(t):
        old=('This jerry Android app was customized by Tanim Boss using a third-party tutorial codebase. '
             'The original author and license are listed under App Identity &amp; Integrity '
             'and in the project LICENSE. No promotional community accounts are required.')
        new=('jerry was customized by Tanim Boss. Original Android source code by Tech GPTx / Rehaan. '
             'Original license: TAMCL v1.0. Source: github.com/techgptx/Playlist. '
             'No promotional community accounts are required.')
        if old not in t:
            raise RuntimeError("Old Privacy Shield legal statement missing")
        return t.replace(old,new,1)
    patch_file(privacy_layout,privacy_source_credit)

    print(f"Jerry Android source patch complete ({n} assistant name text files, home/UI/creator).")


if __name__ == "__main__":
    if len(sys.argv)!=2:
        sys.exit("Usage: customize_jerry.py <source-root>")
    customize(Path(sys.argv[1]))
