#!/usr/bin/env bash
# ==============================================================================
# Ralph Loop: DearTalkAI Autonomous Development & Review Loop
# ==============================================================================
set -euo pipefail

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
if [[ -n $(git status --porcelain) ]]; then
  echo "⚠️ [Ralph Loop] Working tree is dirty. Skipping loop to protect local edits."
  exit 0
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
echo "🛡️ [Ralph Loop] Running Swift Build Gatekeeper..."
BUILD_SUCCESS=true

if ! swift build --package-path "$PROJECT_DIR/deartalk-apple/DearTalk-macOS"; then
  BUILD_SUCCESS=false
  echo "❌ [Ralph Loop] Swift build failed!"
fi

# 4. Handle Result
if [[ "$BUILD_SUCCESS" = true ]]; then
  # Check if there are any changes made by agy
  if [[ -n $(git status --porcelain) ]]; then
    echo "✅ [Ralph Loop] Changes detected and build passed. Committing..."
    git add .
    git commit -m "chore(auto): autonomous ralph loop improvement [${TIMESTAMP}]"
    
    echo "📤 [Ralph Loop] Pushing to remote branch: $BRANCH_NAME..."
    git push origin "$BRANCH_NAME"
    
    # Try creating PR if gh cli is available
    if command -v gh &> /dev/null; then
      echo "📝 [Ralph Loop] Creating GitHub PR via gh CLI..."
      gh pr create \
        --title "🤖 [Auto Loop] Autonomous Improvement (${TIMESTAMP})" \
        --body "Automated improvement cycle completed by \`agy cli\`.\n\n- Build & Verification: Passed\n- Branch: \`$BRANCH_NAME\`" \
        --base main \
        --head "$BRANCH_NAME" || true
    else
      echo "ℹ️ [Ralph Loop] 'gh' CLI not installed. Branch pushed without PR creation."
    fi
    
    echo "🎉 [Ralph Loop] Cycle successfully completed!"
  else
    echo "✨ [Ralph Loop] No code changes required in this cycle. Clean exit."
  fi
  
  # Return to main branch
  git checkout main
else
  echo "🚨 [Ralph Loop] Build verification failed! Rolling back changes..."
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
