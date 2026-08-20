#!/usr/bin/env bash
# ==============================================================================
# Ralph Loop: DearTalkAI Autonomous Development & Review Loop
# ==============================================================================
set -euo pipefail

# Ensure environment paths for cron execution
export PATH="/opt/homebrew/bin:/Users/smilelife/.local/bin:/usr/local/bin:$PATH"

PROJECT_DIR="/Users/smilelife/Projects/deartalk-ai"
cd "$PROJECT_DIR"

LOG_DIR="$PROJECT_DIR/logs"
mkdir -p "$LOG_DIR"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$LOG_DIR/ralph_loop_${TIMESTAMP}.log"

exec > >(tee -a "$LOG_FILE") 2>&1

echo "========================================================"
echo "🚀 [Ralph Loop] Starting Autonomous Cycle at $(date)"
echo "========================================================"

# 1. Check Git Status
# Exclude .agents/INBOX.md from dirty check because users write tasks there
DIRTY_FILES=$(git status --porcelain | grep -v "\.agents/INBOX\.md" || true)
if [[ -n "$DIRTY_FILES" ]]; then
  echo "⚠️ [Ralph Loop] Working tree has uncommitted code changes. Skipping loop to protect local edits:"
  echo "$DIRTY_FILES"
  exit 0
fi

# Auto-commit INBOX.md on main if modified so the branch inherits latest tasks
if [[ -n $(git status --porcelain .agents/INBOX.md) ]]; then
  echo "📥 [Ralph Loop] Committing latest INBOX.md tasks to main..."
  git add .agents/INBOX.md
  git commit -m "docs(inbox): update task list" || true
fi

# Ensure we are on main and up to date
git checkout main
git pull --rebase origin main || true

BRANCH_NAME="auto/ralph-loop-${TIMESTAMP}"
echo "🌿 [Ralph Loop] Creating working branch: $BRANCH_NAME"
git checkout -b "$BRANCH_NAME"

# 2. Run agy CLI with Autonomous Prompt
PROMPT_FILE="$PROJECT_DIR/.agents/prompts/ralph_loop_prompt.md"
if [[ ! -f "$PROMPT_FILE" ]]; then
  echo "❌ [Ralph Loop] Prompt file not found: $PROMPT_FILE"
  exit 1
fi

PROMPT_CONTENT=$(cat "$PROMPT_FILE")

echo "🤖 [Ralph Loop] Invoking agy CLI (autonomous mode)..."
agy --mode accept-edits --dangerously-skip-permissions --effort high --prompt "$PROMPT_CONTENT"

# 3. Build & Test Verification (Gatekeeper)
echo "🛡️ [Ralph Loop] Running Multiplatform (macOS, iOS, Android) Gatekeepers..."
BUILD_SUCCESS=true

echo "  📦 [1/3] Swift Build & Run (DearTalk-macOS)..."
if ! swift build --package-path "$PROJECT_DIR/deartalk-apple/DearTalk-macOS" || ! swift run --package-path "$PROJECT_DIR/deartalk-apple/DearTalk-macOS" DearTalkMacRunner; then
  BUILD_SUCCESS=false
  echo "❌ [Ralph Loop] macOS Swift build or runner failed!"
fi

if [[ "$BUILD_SUCCESS" = true ]]; then
  echo "  📱 [2/3] Swift Build & Run (DearTalk-iOS)..."
  if ! swift build --package-path "$PROJECT_DIR/deartalk-apple/DearTalk-iOS" || ! swift run --package-path "$PROJECT_DIR/deartalk-apple/DearTalk-iOS" DearTalkIOSRunner; then
    BUILD_SUCCESS=false
    echo "❌ [Ralph Loop] iOS Swift build or runner failed!"
  fi
fi

if [[ "$BUILD_SUCCESS" = true ]]; then
  echo "  🤖 [3/3] Android Gradle Tests (:deartalk-android)..."
  if ! ./gradlew test; then
    BUILD_SUCCESS=false
    echo "❌ [Ralph Loop] Android tests failed!"
  fi
fi

# 4. Handle Result
if [[ "$BUILD_SUCCESS" = true ]]; then
  # Check if there are any changes made by agy
  if [[ -n $(git status --porcelain) ]]; then
    echo "✅ [Ralph Loop] Changes detected and all gatekeeper tests passed. Committing..."
    git add .
    git commit -m "chore(auto): autonomous ralph loop improvement [${TIMESTAMP}]"
    
    echo "📤 [Ralph Loop] Pushing backup branch: $BRANCH_NAME..."
    git push origin "$BRANCH_NAME"
    
    echo "🔀 [Ralph Loop] Auto-merging verified changes into main..."
    git checkout main
    git merge "$BRANCH_NAME" --no-edit
    
    echo "🚀 [Ralph Loop] Pushing merged main branch to origin/main..."
    git push origin main
    
    # Cleanup local auto branch
    git branch -d "$BRANCH_NAME" || true
    
    echo "🎉 [Ralph Loop] Cycle successfully completed and merged into main!"
  else
    echo "✨ [Ralph Loop] No code changes required in this cycle. Clean exit."
    git checkout main
    git branch -d "$BRANCH_NAME" || true
  fi
else
  echo "🚨 [Ralph Loop] Build/Test verification failed! Rolling back changes..."
  git reset --hard HEAD
  git clean -fd
  git checkout main
  git branch -D "$BRANCH_NAME" || true
  echo "⚠️ [Ralph Loop] Rollback completed. Main branch preserved safely."
  exit 1
fi

echo "========================================================"
echo "🏁 [Ralph Loop] Cycle Finished at $(date)"
echo "========================================================"
