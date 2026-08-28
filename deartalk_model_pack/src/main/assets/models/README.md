# DearTalk On-Device AI Models

This directory contains on-device SLM model weights delivered via Google Play Asset Delivery (PAD) install-time delivery.

## Supported Model Formats:
- `.litertlm`: Google LiteRT-LM Compiled Model (e.g., `model.litertlm`, `gemma-2b-it.litertlm`)
- `.bin`: Google MediaPipe / LiteRT Weight File (e.g., `model.bin`, `gemma-2b-it-cpu-int4.bin`)
- `.task`: MediaPipe GenAI Task Binary

## Placement:
Place your pre-quantized on-device model here prior to building the release AAB package.
Google Play Store will deliver this asset pack directly during app installation without requiring `INTERNET` permission.
