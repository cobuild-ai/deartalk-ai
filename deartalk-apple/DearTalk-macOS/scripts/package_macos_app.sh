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

# 1. 릴리스 바이너리 빌드
echo "📦 [1/5] Swift Package 릴리스 최적화 빌드 중..."
cd "${PROJECT_DIR}"
swift build -c release

BIN_PATH="$(swift build -c release --show-bin-path)"
EXECUTABLE="${BIN_PATH}/DearTalkMac"

if [ ! -f "${EXECUTABLE}" ]; then
    echo "❌ 빌드 실패: ${EXECUTABLE} 바이너리를 찾을 수 없습니다."
    exit 1
fi

# 2. .app 번들 디렉토리 트리 생성
echo "📁 [2/5] .app 번들 구조 생성 중..."
rm -rf "${APP_BUNDLE}"
mkdir -p "${MACOS_DIR}"
mkdir -p "${RESOURCES_DIR}"
mkdir -p "${MODELS_DIR}"

# 3. 바이너리 및 리소스 복사
echo "📋 [3/5] 실행 파일 및 리소스 복사 중..."
cp "${EXECUTABLE}" "${MACOS_DIR}/${APP_NAME}"
chmod +x "${MACOS_DIR}/${APP_NAME}"

# 모델 파일이 프로젝트나 사용자 홈에 존재할 경우 번들 내부에 자동 복사
MODEL_CANDIDATES=(
    "${PROJECT_DIR}/models/model.gguf"
    "${PROJECT_DIR}/models/gemma-2b-it.gguf"
    "${HOME}/.deartalk/models/model.gguf"
    "${HOME}/.deartalk/models/gemma-2b-it.gguf"
    "${HOME}/Library/Application Support/DearTalk/models/model.gguf"
)

FOUND_MODEL=""
for candidate in "${MODEL_CANDIDATES[@]}"; do
    if [ -f "${candidate}" ]; then
        echo "🧠 온디바이스 모델 발견: ${candidate} -> 번들 내부 복사"
        cp "${candidate}" "${MODELS_DIR}/model.gguf"
        FOUND_MODEL="${candidate}"
        break
    fi
done

if [ -z "${FOUND_MODEL}" ]; then
    echo "ℹ️ 번들에 포함할 로컬 .gguf 모델이 감지되지 않았습니다. (${MODELS_DIR}/ 에 모델 배치 시 자동 로드)"
fi

# 4. Info.plist 및 PkgInfo 생성
echo "📝 [4/5] Info.plist 및 메타데이터 작성 중..."
cat <<EOF > "${CONTENTS_DIR}/Info.plist"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>ko</string>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIdentifier</key>
    <string>ai.deartalk.mac</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>${APP_NAME}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>13.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSPrincipalClass</key>
    <string>NSApplication</string>
    <key>NSAccessibilityUsageDescription</key>
    <string>실시간 타이핑 문맥 분석 및 DIFF 제안을 위해 손쉬운 사용 권한이 필요합니다.</string>
</dict>
</plist>
EOF

echo "APPL????" > "${CONTENTS_DIR}/PkgInfo"

# 5. Ad-hoc 코드사이닝 적용 (고정된 번들 ID 지정하여 TCC 손쉬운 사용 권한 영구 유지)
echo "🔐 [5/5] macOS Ad-hoc 코드사이닝 적용 중..."
codesign --force --deep --sign - --identifier "ai.deartalk.mac" "${APP_BUNDLE}"

echo "=================================================="
echo "✅ DearTalk.app 단일 번들 패키징 완료!"
echo "📍 위치: ${APP_BUNDLE}"
echo "=================================================="
echo "💡 실행 방법: open \"${APP_BUNDLE}\" 또는 /Applications 로 복사"
