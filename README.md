<p align="center">
  <img src="assets/tress-app-icon.png" width="112" alt="Tress Logo" />
</p>

<h1 align="center">Tress</h1>

<p align="center">
  <strong>Private, offline-first on-device AI workspace for Android</strong><br/>
  Powered by LiteRT-LM, Gemma, Qwen, DeepSeek &amp; modular Agent Skills — wrapped in a minimal, fast Bento &amp; Liquid Glass UI.
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-18181b.svg" alt="License" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%2012%2B-18181b.svg" alt="Android 12+" />
  <img src="https://img.shields.io/badge/Inference-100%25%20On--Device-18181b.svg" alt="On-Device AI" />
</p>

---

## Overview

**Tress** runs open-weight multimodal Large Language Models directly on your phone's CPU/GPU/NPU—with zero cloud lock-in, zero analytics trackers, and full offline privacy.

It reimagines local AI on Android with:
- **Bento Workspace & Neutral UI**: Compact 2-column Bento grid with restrained shadcn/ui-inspired Zinc & Slate contrast, theme presets, and custom appearance controls.
- **Persistent Chat History & Sessions**: Resume previous conversations seamlessly across AI Chat, Test Chat, and Agent Skills.
- **Multimodal & Voice Ready**: Image understanding, audio clips, on-device speech recognition (ASR) dictation, text-to-speech playback, and camera capture directly inside a floating glass composer.
- **Modular Agent Skills & MCP**: Extend local models with tools, web/local workflows, Prompt Lab parameter tuning (`temperature`, `top-k`), and on-device benchmarking.

---

## Screenshots

<p align="center">
  <img src="assets/screenshots/home-bento.jpg" width="210" alt="Home Bento Workspace" />
  <img src="assets/screenshots/prompt-lab-grid.jpg" width="210" alt="Model Hub Grid" />
  <img src="assets/screenshots/chat-composer.jpg" width="210" alt="Chat Attachment Composer" />
  <img src="assets/screenshots/chat-dark.jpg" width="210" alt="Dark Mode Chat" />
</p>

<details>
<summary>More screens</summary>

<p align="center">
  <img src="assets/screenshots/ai-chat-models.jpg" width="210" alt="AI Chat Models" />
  <img src="assets/screenshots/agent-skills.jpg" width="210" alt="Agent Skills" />
  <img src="assets/screenshots/mobile-actions.jpg" width="210" alt="Mobile Actions" />
  <img src="assets/screenshots/menu-drawer.jpg" width="210" alt="Navigation Menu" />
</p>

</details>

---

## Brand Assets

High-resolution logo files exported in [`assets/`](assets/):
- [`assets/tress-logo.png`](assets/tress-logo.png) — 1024×1024 transparent mark
- [`assets/tress-app-icon.png`](assets/tress-app-icon.png) — 1024×1024 rounded app icon
- [`assets/tress-logo.svg`](assets/tress-logo.svg) — Scalable vector mark

---

## Core Capabilities

- **AI Chat & Multimodal Ask**: Chat with `Gemma 4`, `Gemma 3n`, `Qwen 2.5`, and `DeepSeek-R1` distilled models locally with image & audio inputs.
- **Agent Skills**: Connect local LLMs to structured tools and modular skills (`skills/`).
- **Prompt Lab**: Rapid single-turn evaluation across models with configurable decoding settings.
- **Instant Media Search & Video Moment Finder**: Semantic search over local media using on-device embeddings.
- **Audio Scribe & XR Language Teacher**: Real-time local transcription, translation, and interactive language study.
- **Hardware Benchmarking**: Measure prefill/decode tokens per second and latency directly on device.

---

## Building Locally

### Requirements
- Android Studio / JDK 21
- Android SDK (compileSdk 37 / Android 12+ target device)

### Build & Test
```bash
cd Android/src
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

---

## License

Apache License 2.0 — see [`LICENSE`](LICENSE) for details.
