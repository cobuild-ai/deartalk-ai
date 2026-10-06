#!/bin/bash
# ==============================================================================
# ✨ DearTalk-AI Pre-PR Automated Verification Suite (verify.sh)
# Open-source automated verification launcher for JVM tests, device stress, and builds
# ==============================================================================

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

CYAN='\033[1;36m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'
MAGENTA='\033[1;35m'
RED='\033[1;31m'
BOLD='\033[1m'
NC='\033[0m'

# Auto-provision Python Virtual Environment (.venv)
VENV_DIR="$PROJECT_ROOT/.venv"
if [ ! -f "$VENV_DIR/bin/python3" ]; then
    echo -e "${CYAN}📦 Provisioning isolated Python virtual environment (.venv)...${NC}"
    python3 -m venv "$VENV_DIR"
    echo -e "${GREEN}✔ Virtual environment ready: $VENV_DIR${NC}"
fi
PYTHON="$VENV_DIR/bin/python3"

# Interactive mode when no arguments provided
if [ -z "$1" ]; then
    echo -e "\n${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}${BOLD} 🧪 DearTalk-AI Automated Testing & Verification Suite ${NC}"
    echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  ${GREEN}1)${NC} 🚀 전체 일괄 검증 (All-in-One: Policy + Unit + Device)"
    echo -e "  ${GREEN}2)${NC} 🚀 Pre-Push 필수 게이트 (make pre-push: Policy + 5-Lens + Unit)"
    echo -e "  ${GREEN}3)${NC} 🧠 SkyBrain 5대 렌즈 종합 평가 (make audit-lenses)"
    echo -e "  ${GREEN}4)${NC} 🔒 구글 플레이 개인정보처리방침/Zero-Data 검사 (make audit-policy)"
    echo -e "  ${GREEN}5)${NC} 🧪 JVM 단위 테스트 (make test)"
    echo -e "  ${GREEN}6)${NC} 📱 실기기 자동화 안정성 검증 (make verify-device)"
    echo -e "  ${GREEN}7)${NC} 🔨 Debug APK 빌드 (make build)"
    echo -e "  ${GREEN}8)${NC} 📦 Release Bundle (AAB) 빌드 (make release)"
    echo -e "  ${GREEN}9)${NC} 🔍 Kotlin 코드 린트 (make lint)"
    echo -e "  ${YELLOW}q)${NC} 종료 (Quit)"
    echo -e "${CYAN}────────────────────────────────────────────────────────────${NC}"
    read -r -p "▶ 실행할 작업 번호를 입력하세요 [1-9, q]: " choice

    case "$choice" in
        1) CMD="all" ;;
        2) CMD="pre-push" ;;
        3) CMD="lenses" ;;
        4) CMD="policy" ;;
        5) CMD="unit" ;;
        6) CMD="device" ;;
        7) CMD="build" ;;
        8) CMD="release" ;;
        9) CMD="lint" ;;
        q|Q) echo -e "${GREEN}👋 종료합니다.${NC}"; exit 0 ;;
        *) echo -e "${YELLOW}잘못된 입력입니다. 종료합니다.${NC}"; exit 1 ;;
    esac
else
    CMD="$1"
fi

case "$CMD" in
    all|--all|1)
        echo -e "\n${CYAN}🔒 [1/3] Running Google Play Privacy Policy & Zero-Data Audit...${NC}"
        "$PYTHON" scripts/verify_privacy_policy.py

        echo -e "\n${CYAN}🧪 [2/3] Running JVM Unit Tests...${NC}"
        ./gradlew :deartalk-android:testDebugUnitTest

        echo -e "\n${CYAN}📱 [3/3] Running Real-Device Stability & Stress Audit...${NC}"
        shift 2>/dev/null || true
        "$PYTHON" scripts/verify_device_stability.py "$@"

        echo -e "\n${GREEN}${BOLD}🎉 [PASSED] All verification stages passed with zero defects!${NC}\n"
        ;;
    pre-push|2)
        make pre-push
        ;;
    lenses|audit-lenses|5-lens|3)
        "$PYTHON" scripts/verify_five_lenses.py
        ;;
    policy|verify-policy|privacy|4)
        "$PYTHON" scripts/verify_privacy_policy.py
        ;;
    unit|test|--test|5)
        shift 2>/dev/null || true
        echo -e "${CYAN}🧪 Running JVM unit tests with args: $@${NC}"
        ./gradlew :deartalk-android:testDebugUnitTest "$@"
        ;;
    device|verify-device|--device|6)
        shift 2>/dev/null || true
        echo -e "${CYAN}📱 Running automated real-device stability & memory leak audit with args: $@${NC}"
        "$PYTHON" scripts/verify_device_stability.py "$@"
        ;;
    build|--build|debug|7)
        shift 2>/dev/null || true
        echo -e "${CYAN}🔨 Building DearTalk Android Debug APK with args: $@${NC}"
        ./gradlew :deartalk-android:assembleDebug "$@"
        ;;
    release|bundle|8)
        shift 2>/dev/null || true
        echo -e "\n${CYAN}🔒 [Pre-flight 1/2] Verifying Google Play Privacy Policy & Zero-Data Invariant...${NC}"
        "$PYTHON" scripts/verify_privacy_policy.py
        echo -e "\n${CYAN}🧪 [Pre-flight 2/2] Running DearTalk Android Unit Tests...${NC}"
        ./gradlew :deartalk-android:testDebugUnitTest
        echo -e "\n${MAGENTA}📦 Building DearTalk Android Release Bundle (AAB) with args: $@${NC}"
        ./gradlew :deartalk-android:bundleRelease "$@"
        ;;
    lint|9)
        shift 2>/dev/null || true
        echo -e "${CYAN}🔍 Checking Kotlin code quality...${NC}"
        ./gradlew :deartalk-android:lintDebug "$@" || true
        ;;
    help|--help|-h)
        echo -e "${CYAN}✨ DearTalk-AI Automated Verification Suite:${NC}"
        echo -e "  ${GREEN}./verify.sh${NC}                 - Interactive menu mode"
        echo -e "  ${GREEN}./verify.sh all${NC}             - Run All-in-One verification (Policy + Unit + Device)"
        echo -e "  ${GREEN}./verify.sh pre-push${NC}        - Run Pre-Push safety gates (make pre-push)"
        echo -e "  ${GREEN}./verify.sh lenses${NC}          - Run SkyBrain 5-Lens Audit"
        echo -e "  ${GREEN}./verify.sh policy${NC}          - Run Google Play Privacy Policy Audit"
        echo -e "  ${GREEN}./verify.sh unit [args...]${NC}  - Run JVM unit tests"
        echo -e "  ${GREEN}./verify.sh device [args...]${NC}- Run automated real-device stability audit"
        echo -e "  ${GREEN}./verify.sh build${NC}           - Build Debug APK"
        echo -e "  ${GREEN}./verify.sh release${NC}         - Build Release Bundle (AAB)"
        echo -e "  ${GREEN}./verify.sh lint${NC}            - Run Kotlin static analysis"
        ;;
    *)
        echo -e "${YELLOW}Unknown command: $CMD. Showing help:${NC}"
        ./verify.sh --help
        ;;
esac
