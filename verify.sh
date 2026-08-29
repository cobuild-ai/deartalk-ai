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
    echo -e "  ${GREEN}1)${NC} 🚀 전체 일괄 검증 (All-in-One: Unit Tests + Real-Device Audit)"
    echo -e "  ${GREEN}2)${NC} 🧪 JVM 단위 테스트 (make test)"
    echo -e "  ${GREEN}3)${NC} 📱 실기기 자동화 안정성 검증 (make verify-device)"
    echo -e "  ${GREEN}4)${NC} 🔨 Debug APK 빌드 (make build)"
    echo -e "  ${GREEN}5)${NC} 📦 Release Bundle (AAB) 빌드 (make release)"
    echo -e "  ${GREEN}6)${NC} 🔍 Kotlin 코드 린트 (make lint)"
    echo -e "  ${YELLOW}q)${NC} 종료 (Quit)"
    echo -e "${CYAN}────────────────────────────────────────────────────────────${NC}"
    read -r -p "▶ 실행할 작업 번호를 입력하세요 [1-6, q]: " choice

    case "$choice" in
        1) CMD="all" ;;
        2) CMD="unit" ;;
        3) CMD="device" ;;
        4) CMD="build" ;;
        5) CMD="release" ;;
        6) CMD="lint" ;;
        q|Q) echo -e "${GREEN}👋 종료합니다.${NC}"; exit 0 ;;
        *) echo -e "${YELLOW}잘못된 입력입니다. 종료합니다.${NC}"; exit 1 ;;
    esac
else
    CMD="$1"
fi

case "$CMD" in
    all|--all|1)
        echo -e "\n${CYAN}🧪 [1/2] Running JVM Unit Tests...${NC}"
        ./gradlew :deartalk-android:testDebugUnitTest

        echo -e "\n${CYAN}📱 [2/2] Running Real-Device Stability & Stress Audit...${NC}"
        shift 2>/dev/null || true
        "$PYTHON" scripts/verify_device_stability.py "$@"

        echo -e "\n${GREEN}${BOLD}🎉 [PASSED] All verification stages passed with zero defects!${NC}\n"
        ;;
    unit|test|--test|2)
        shift 2>/dev/null || true
        echo -e "${CYAN}🧪 Running JVM unit tests with args: $@${NC}"
        ./gradlew :deartalk-android:testDebugUnitTest "$@"
        ;;
    device|verify-device|--device|3)
        shift 2>/dev/null || true
        echo -e "${CYAN}📱 Running automated real-device stability & memory leak audit with args: $@${NC}"
        "$PYTHON" scripts/verify_device_stability.py "$@"
        ;;
    build|--build|debug|4)
        shift 2>/dev/null || true
        echo -e "${CYAN}🔨 Building DearTalk Android Debug APK with args: $@${NC}"
        ./gradlew :deartalk-android:assembleDebug "$@"
        ;;
    release|bundle|5)
        shift 2>/dev/null || true
        echo -e "${MAGENTA}📦 Building DearTalk Android Release Bundle (AAB) with args: $@${NC}"
        ./gradlew :deartalk-android:bundleRelease "$@"
        ;;
    lint|6)
        shift 2>/dev/null || true
        echo -e "${CYAN}🔍 Checking Kotlin code quality...${NC}"
        ./gradlew :deartalk-android:lintDebug "$@" || true
        ;;
    help|--help|-h)
        echo -e "${CYAN}✨ DearTalk-AI Automated Verification Suite:${NC}"
        echo -e "  ${GREEN}./verify.sh${NC}                 - Interactive menu mode"
        echo -e "  ${GREEN}./verify.sh all${NC}             - Run All-in-One verification (Unit + Device)"
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
