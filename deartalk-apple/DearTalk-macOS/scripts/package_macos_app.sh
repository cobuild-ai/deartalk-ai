#!/usr/bin/env bash
set -e

# ==============================================================================
# DearTalk macOS Standalone .app Package Builder
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_DIR="${PROJECT_DIR}/build"
APP_NAME="DearTalk"
APP_BUNDLE="${BUILD_DIR}/${APP_NAME}.app"
CONTENTS_DIR="${APP_BUNDLE}/Contents"
MACOS_DIR="${CONTENTS_DIR}/MacOS"
RESOURCES_DIR="${CONTENTS_DIR}/Resources"
MODELS_DIR="${RESOURCES_DIR}/models"

echo "=================================================="
echo "🚀 [DearTalk macOS Standalone .app Packaging]"
echo "=================================================="

# 1. Build optimized release binary
echo "📦 [1/5] Compiling Swift Package in release mode..."
cd "${PROJECT_DIR}"
swift build -c release

BIN_PATH="$(swift build -c release --show-bin-path)"
EXECUTABLE="${BIN_PATH}/DearTalkMac"

if [ ! -f "${EXECUTABLE}" ]; then
    echo "❌ Build failed: Executable not found at ${EXECUTABLE}"
    exit 1
fi

# 2. Create .app bundle directory hierarchy
echo "📁 [2/5] Creating .app bundle directory structure..."
rm -rf "${APP_BUNDLE}"
mkdir -p "${MACOS_DIR}"
mkdir -p "${RESOURCES_DIR}"
mkdir -p "${MODELS_DIR}"

# 3. Copy binary and bundled assets
echo "📋 [3/5] Copying binaries and resources..."
cp "${EXECUTABLE}" "${MACOS_DIR}/${APP_NAME}"
chmod +x "${MACOS_DIR}/${APP_NAME}"

# Embed or symlink on-device models into the bundle if available locally
MODEL_CANDIDATES=(
    "${PROJECT_DIR}/models/model.gguf"
    "${PROJECT_DIR}/models/gemma-2b-it.gguf"
    "${HOME}/Library/Application Support/DearTalk/models/model.gguf"
    "${HOME}/.deartalk/models/model.gguf"
)

FOUND_MODEL=""
for candidate in "${MODEL_CANDIDATES[@]}"; do
    if [ -f "${candidate}" ]; then
        echo "🧠 On-device model found: ${candidate} -> Linking to bundle"
        ln -sf "${candidate}" "${MODELS_DIR}/model.gguf" 2>/dev/null || cp -f "${candidate}" "${MODELS_DIR}/model.gguf" 2>/dev/null || true
        FOUND_MODEL="${candidate}"
        break
    fi
done

if [ -z "${FOUND_MODEL}" ]; then
    echo "ℹ️ Note: No pre-baked model found. The app will auto-download on first launch."
fi

# 4. Generate Info.plist metadata
echo "📝 [4/5] Writing Info.plist and metadata..."
cat <<EOF > "${CONTENTS_DIR}/Info.plist"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundleIdentifier</key>
    <string>ai.deartalk.mac</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>DearTalk</string>
    <key>CFBundleDisplayName</key>
    <string>DearTalk AI</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>14.0</string>
    <key>LSUIElement</key>
    <true/>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSHumanReadableCopyright</key>
    <string>Copyright © 2026 DearTalk Authors. All rights reserved.</string>
</dict>
</plist>
EOF

echo "APPL????" > "${CONTENTS_DIR}/PkgInfo"

# 5. Apply macOS Ad-hoc codesigning
echo "🔐 [5/5] Applying macOS Ad-hoc codesigning..."
codesign --force --deep --sign - "${APP_BUNDLE}"

echo "=================================================="
echo "✅ DearTalk.app standalone bundle successfully packaged!"
echo "📍 Location: ${APP_BUNDLE}"
echo "=================================================="
echo "💡 Launch command: open \"${APP_BUNDLE}\" or move to /Applications"
