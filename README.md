# jerry AI Assistant — Independent Android implementation

**Project:** jerry AI Assistant by **TaNiM world (Tanim Boss)**  
**Branch:** `feature/independent-jerry-source`  
**Publisher / developer branding:** TaNiM world (Tanim Boss)  
**Copyright for independently authored code:** © 2026 TaNiM world (Tanim Boss)  
**Status:** Independent implementation in progress; NOT feature-equivalent yet.

This independent implementation carries TaNiM world branding without the earlier source's original author branding. External dependencies retain their own legal terms.  

The **existing stable jerry app on `main` is unchanged**. This branch has an
independently authored Android app under app/ rather than downloading or
modifying previous project source. Original app functionality is not
represented as implemented unless code for it exists here and tests confirm it.

## Current implementation
- Android native Java screen with English labels, editable API key and owner
  identity, typed chat, and push-to-talk via Android speech recognition
- Gemini generateContent response over HTTPS, optional Android text-to-speech
- Local memory and user-initiated JSON memory export/import
- Open installed apps by name, open web search, search YouTube, launch camera,
  torch control where hardware/permissions allow, media volume
- Voice commands for back/home and a screenshot using an explicitly enabled
  accessibility service on supported Android versions
- Android local reminder notifications (notification permission required)
- Opt-in Android notification reading and per-message confirmation for supported quick replies
- User-selected screenshot/photo AI reading and PDF vision for the first 3 pages (explicit consent, Gemini API key required)
- Direct Gemini 3.8 Live audio sessions with user-activated microphone and Stop button; opt-in Android microphone foreground service with persistent notification and Stop action (not device-tested)
- Offline ML Kit QR/barcode scanning and Latin printed-text recognition from files the user selects
- Android Storage Access Framework file selection, safe open and explicit clipboard copy/paste

## Not implemented or not feature-parity verified
Continuous hands-free wake-word, Gemini Live audio-to-audio, continuous screen-reading AI,
contact-by-name WhatsApp/IMO/Messenger auto-send, cloned-app launching,
automated YouTube playback, smart-home/router actions, OCR/document scan,
offline understanding, complex macros and routines, 3D/avatar lip sync,
full background conversation.
See FEATURE_PARITY.md. **Do not replace the main app until these have been
independently reimplemented and tested on the target phone.**

## Android phone-only build
GitHub Actions workflow `Independent jerry Android APK` runs on this branch.
A successful workflow provides a debug APK artifact. This independent preview
uses package `ai.jeery.assistant.clean` to avoid replacing the original app,
and does not automatically migrate its stored memory. Never uninstall the
working app to test an unfinished preview.

Build uses Java 17, Android SDK 34 and Gradle; no computer required.
