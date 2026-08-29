# 🏛️ DearTalk AI - Open Source Makefile
# Zero-Friction Developer Experience (DX) for Android & Cross-Platform

.PHONY: help setup-venv verify verify-all test verify-device build release clean lint

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
	@echo "  \033[1;32mmake verify\033[0m             - Run full pre-PR verification (Unit Tests + Real-Device Audit)"
	@echo "  \033[1;32mmake test\033[0m               - Run JVM unit tests"
	@echo "  \033[1;32mmake verify-device\033[0m      - Run automated real-device Monkey stress & memory audit"
	@echo "  \033[1;35mmake build\033[0m              - Assemble debug APK & install on device"
	@echo "  \033[1;35mmake release\033[0m            - Build release App Bundle (AAB)"
	@echo "  \033[1;36mmake setup-venv\033[0m         - Provision isolated Python .venv environment"
	@echo "  \033[1;34mmake lint\033[0m               - Run Kotlin static analysis and linting"
	@echo "  \033[1;31mmake clean\033[0m              - Clean Gradle build outputs"

verify: $(PYTHON)
	@./verify.sh all

verify-all: verify

test:
	@echo "\n\033[1;34m🧪 Running DearTalk Android Unit Tests...\033[0m"
	@./gradlew :deartalk-android:testDebugUnitTest

verify-device: $(PYTHON)
	@$(PYTHON) scripts/verify_device_stability.py

build:
	@echo "\n\033[1;34m🔨 Building DearTalk Android Debug APK...\033[0m"
	@./gradlew :deartalk-android:assembleDebug

release:
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
