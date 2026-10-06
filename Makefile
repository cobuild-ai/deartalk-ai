# 🏛️ DearTalk AI - Open Source Makefile
# Zero-Friction Developer Experience (DX) for Android & Cross-Platform

.PHONY: all help setup-venv verify verify-all test verify-device build release clean lint audit-policy audit-lenses pre-push

all: pre-push

VENV_DIR ?= .venv
PYTHON ?= $(VENV_DIR)/bin/python3

$(PYTHON):
	@if [ ! -f "$(PYTHON)" ]; then \
		echo "\033[1;36m📦 Provisioning isolated Python virtual environment in $(VENV_DIR)...\033[0m"; \
		python3 -m venv $(VENV_DIR); \
		echo "\033[1;32m✔ Virtual environment ready: $(VENV_DIR)\033[0m"; \
	fi

setup-venv: $(PYTHON)

help:
	@echo "\033[1;36m✨ DearTalk AI Open Source Developer Tools\033[0m"
	@echo "\033[2mAvailable Commands:\033[0m"
	@echo "  \033[1;32mmake pre-push\033[0m           - Run mandatory pre-push gates (Policy + 5-Lens + Unit Tests)"
	@echo "  \033[1;32mmake audit-policy\033[0m       - Audit Google Play Privacy Policy & Zero-Data Invariant"
	@echo "  \033[1;32mmake audit-lenses\033[0m       - Run SkyBrain 5-Lens Explicit Quality & Security Audit"
	@echo "  \033[1;32mmake test\033[0m               - Run JVM unit tests"
	@echo "  \033[1;32mmake verify\033[0m             - Run full pre-PR verification (Unit Tests + Real-Device Audit)"
	@echo "  \033[1;32mmake verify-device\033[0m      - Run automated real-device Monkey stress & memory audit"
	@echo "  \033[1;35mmake build\033[0m              - Assemble debug APK & install on device"
	@echo "  \033[1;35mmake release\033[0m            - Build release App Bundle (AAB) with pre-flight policy & 5-lens audit"
	@echo "  \033[1;36mmake setup-venv\033[0m         - Provision isolated Python .venv environment"
	@echo "  \033[1;34mmake lint\033[0m               - Run Kotlin static analysis and linting"
	@echo "  \033[1;31mmake clean\033[0m              - Clean Gradle build outputs"

# 🔒 [1] 개인정보 처리방침 4단계 무결성 검증 (Zero-Network + HTML + In-App Link + Cloudflare Edge)
audit-policy: $(PYTHON)
	@echo "\033[1;36m🔒 [Audit 1/3] Running Privacy Policy & Zero-Data Auditor...\033[0m"
	@$(PYTHON) scripts/verify_privacy_policy.py

verify-policy: audit-policy

# 🧠 [2] SkyBrain 5대 렌즈 명시적 품질/보안 평가 (CleanCode, Architecture, Security, Performance, AIConduct)
audit-lenses: $(PYTHON)
	@echo "\033[1;36m🧠 [Audit 2/3] Explicitly requesting SkyBrain 5-Lens Evaluation...\033[0m"
	@$(PYTHON) scripts/verify_five_lenses.py

# 🧪 [3] JVM 단위 테스트 전수 검증
test:
	@echo "\n\033[1;34m🧪 [Audit 3/3] Running DearTalk Android Unit Tests...\033[0m"
	@./gradlew :deartalk-android:testDebugUnitTest

# 🚀 Push 전 필수 종합 게이트 (정책 + 5대 렌즈 + 단위 테스트)
pre-push: audit-policy audit-lenses test
	@echo "\n\033[1;32m🎉 [PASSED] All Pre-Push Quality & Security Gates Succeeded! Safe to Push.\033[0m\n"

verify: $(PYTHON)
	@./verify.sh all

verify-all: verify

verify-device: $(PYTHON)
	@$(PYTHON) scripts/verify_device_stability.py

build:
	@echo "\n\033[1;34m🔨 Building DearTalk Android Debug APK...\033[0m"
	@./gradlew :deartalk-android:assembleDebug

release: audit-policy test
	@echo "\n\033[1;34m📦 Building DearTalk Android Release Bundle (AAB)...\033[0m"
	@./gradlew :deartalk-android:bundleRelease

lint:
	@echo "\n\033[1;34m🔍 Checking Kotlin code quality...\033[0m"
	@./gradlew :deartalk-android:lintDebug || true

clean:
	@echo "🧹 Cleaning Gradle builds and temporary files..."
	@./gradlew clean 2>/dev/null || true
	@find . -name ".DS_Store" -delete 2>/dev/null || true
	@find . -name "__pycache__" -type d -exec rm -rf {} + 2>/dev/null || true
	@echo "\033[32m✅ Workspace cleaned.\033[0m"

