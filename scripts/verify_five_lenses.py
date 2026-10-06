#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🧠 DearTalk AI - SkyBrain 5-Lens Explicit Quality & Security Audit Runner
Enforces 100% Pure Neural On-Device Inference via SkyBrain (Gemma 4 E4B Metal):
1. Clean Code Lens (🧹): Readability, dead code, code smells, naming standards.
2. Clean Architecture Lens (🏛️): Separation of concerns, module boundaries, SRP.
3. Security Lens (🛡️): Zero-Network invariant, credential leakage, permission creep.
4. Performance Lens (⚡): Composable recomposition waste, memory leaks, unneeded allocations.
5. AI Conduct Lens (🤖): Zero fake rules, anti-hallucination, Apache 2.0 license compliance.

🚨 STRICT COMPLIANCE: NO mock heuristics, NO hardcoded fake bypasses.
Evaluated purely by local SkyBrain Gemma 4 E4B SLM via ReviewEngine & Daemon API.
"""

import sys
import os
import time
import json
import subprocess
import urllib.request
import urllib.error
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
SKYBRAIN_DIR = Path(os.getenv("SKYBRAIN_HOME", str(PROJECT_ROOT.parent / "skybrain")))
SKYBRAIN_PY = SKYBRAIN_DIR / ".venv" / "bin" / "python3"
SKYBRAIN_BIN = SKYBRAIN_DIR / ".venv" / "bin" / "skybrain"
DAEMON_URL = "http://127.0.0.1:8000"

CYAN = "\033[1;36m"
GREEN = "\033[1;32m"
YELLOW = "\033[1;33m"
RED = "\033[1;31m"
MAGENTA = "\033[1;35m"
BOLD = "\033[1m"
NC = "\033[0m"


def check_daemon_alive() -> bool:
    """Verifies that the SkyBrain on-device daemon is active."""
    print(f"{CYAN}📡 [1/3] Verifying SkyBrain On-Device Daemon Status...{NC}")
    try:
        req = urllib.request.Request(f"{DAEMON_URL}/healthz", headers={"User-Agent": "SkyBrain-PrePush/1.0"})
        with urllib.request.urlopen(req, timeout=3.0) as resp:
            if resp.status == 200:
                try:
                    q_req = urllib.request.Request(f"{DAEMON_URL}/v1/queue/status")
                    with urllib.request.urlopen(q_req, timeout=2.0) as q_resp:
                        data = json.loads(q_resp.read().decode())
                        model = data.get("active_model", "Gemma 4 E4B")
                        print(f"{GREEN}✔ SkyBrain daemon is active & healthy on port 8000.{NC}")
                        print(f"{GREEN}  Model: {model} (Running on Apple Silicon Metal GPU){NC}")
                        return True
                except Exception:
                    print(f"{GREEN}✔ SkyBrain daemon is active (port 8000).{NC}")
                    return True
    except Exception:
        pass

    print(f"{YELLOW}⚠️ SkyBrain daemon is not running on {DAEMON_URL}.{NC}")
    if SKYBRAIN_BIN.exists():
        print(f"{CYAN}🚀 Attempting to auto-start SkyBrain daemon...{NC}")
        subprocess.run([str(SKYBRAIN_BIN), "start"], check=False)
        for _ in range(15):
            time.sleep(1.0)
            try:
                with urllib.request.urlopen(f"{DAEMON_URL}/healthz", timeout=2.0) as resp:
                    if resp.status == 200:
                        print(f"{GREEN}✔ SkyBrain daemon started successfully!{NC}")
                        return True
            except Exception:
                continue

    print(f"{RED}🚨 CRITICAL: SkyBrain daemon could not be reached.{NC}")
    print(f"{RED}   The 5-Lens Audit must be evaluated by SkyBrain per project governance.{NC}")
    print(f"{RED}   Run 'skybrain start' in a separate terminal and re-run.{NC}")
    return False


def run_git_pre_audit() -> bool:
    """Executes SkyBrain Git Pre-Audit (Secrets + Apache 2.0 + Line Limits)."""
    print(f"\n{CYAN}🔒 [2/3] Invoking SkyBrain Git Pre-Audit ($0 Token Static Audit)...{NC}")
    if not SKYBRAIN_BIN.exists():
        print(f"{RED}❌ SkyBrain CLI binary missing at {SKYBRAIN_BIN}{NC}")
        return False

    cmd = [str(SKYBRAIN_BIN), "git", "audit"]
    try:
        proc = subprocess.run(cmd, cwd=str(PROJECT_ROOT), capture_output=True, text=True, timeout=30)
        if proc.stdout:
            print(proc.stdout.strip())
        if proc.returncode != 0:
            if proc.stderr:
                print(f"{RED}{proc.stderr.strip()}{NC}")
            print(f"{RED}❌ SkyBrain Git Audit reported policy violations!{NC}")
            return False
        print(f"{GREEN}✔ Passed: SkyBrain Git Pre-Audit passed cleanly.{NC}")
        return True
    except Exception as exc:
        print(f"{RED}❌ Error running skybrain git audit: {exc}{NC}")
        return False


def get_target_files() -> list[str]:
    """Finds staged Kotlin files, or defaults to the core key component."""
    # 1. Staged Kotlin files
    try:
        res = subprocess.run(
            ["git", "diff", "--cached", "--name-only", "*.kt"],
            cwd=str(PROJECT_ROOT),
            capture_output=True,
            text=True,
        )
        staged = [line.strip() for line in res.stdout.strip().split("\n") if line.strip()]
        if staged:
            valid = [str(PROJECT_ROOT / f) for f in staged if (PROJECT_ROOT / f).exists()]
            if valid:
                return valid
    except Exception:
        pass

    # 2. Key representative component
    key_comp = PROJECT_ROOT / "deartalk-android/src/main/java/ai/deartalk/android/ui/main/components/MainMiscCards.kt"
    if key_comp.exists():
        return [str(key_comp)]
    return []


def run_five_lenses_evaluation() -> bool:
    """Runs the 5-Lens Evaluation directly via SkyBrain's ReviewEngine."""
    print(f"\n{CYAN}🧠 [3/3] Executing SkyBrain 5-Lens Neural Review (Gemma 4 E4B Metal)...{NC}")

    if not SKYBRAIN_PY.exists():
        print(f"{RED}❌ SkyBrain Python environment missing at {SKYBRAIN_PY}{NC}")
        return False

    targets = get_target_files()
    if not targets:
        print(f"{YELLOW}ℹ No reviewable Kotlin files found. Skipping file-level review.{NC}")
        return True

    print(f"{MAGENTA}📄 Targets to audit: {[os.path.basename(t) for t in targets]}{NC}")

    runner_script = """
import sys
import json
import logging
from pathlib import Path

# Silence noisy network logging
logging.getLogger("urllib3").setLevel(logging.WARNING)
logging.getLogger("skybrain.review.client").setLevel(logging.INFO)

from skybrain.review.client import SkyBrainClient
from skybrain.review.engine import ReviewEngine
from skybrain.review.lenses.clean_code import CleanCodeLens
from skybrain.review.lenses.clean_architecture import CleanArchitectureLens
from skybrain.review.lenses.security import SecurityLens
from skybrain.review.lenses.performance import PerformanceLens
from skybrain.review.lenses.ai_conduct import AIConductLens

lenses = [
    CleanCodeLens,
    CleanArchitectureLens,
    SecurityLens,
    PerformanceLens,
    AIConductLens,
]

# Set 600s timeout to give on-device Gemma 4 E4B Metal GPU sufficient time for complex files
client = SkyBrainClient(timeout=600.0)
engine = ReviewEngine(lens_classes=lenses, client=client)
targets = [Path(p) for p in sys.argv[1:] if Path(p).exists()]

def progress_cb(desc, amt):
    sys.stderr.write(f"  ↳ {desc}\\n")
    sys.stderr.flush()

report = engine.review(
    file_paths=targets,
    verify=False,
    voting_rounds=1,
    use_cache=True,
    progress_callback=progress_cb,
)

data = {
    "total_findings": len(report.findings),
    "critical_count": len([f for f in report.findings if f.severity.name == "CRITICAL"]),
    "high_count": len([f for f in report.findings if f.severity.name == "HIGH"]),
    "findings": [
        {
            "category": f.category.value if hasattr(f.category, "value") else str(f.category),
            "severity": f.severity.name if hasattr(f.severity, "name") else str(f.severity),
            "file": Path(f.file).name,
            "line": f.line,
            "principle_violated": f.principle_violated,
            "description": f.description,
            "suggestion": f.suggestion,
        }
        for f in report.findings
    ]
}

print("###SKYBRAIN_JSON_START###")
print(json.dumps(data, ensure_ascii=False, indent=2))
print("###SKYBRAIN_JSON_END###")
"""

    cmd = [str(SKYBRAIN_PY), "-u", "-c", runner_script] + targets
    try:
        # Use Popen to stream stderr lines in real-time
        proc = subprocess.Popen(
            cmd,
            cwd=str(PROJECT_ROOT),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

        # Stream stderr in real time
        import select
        stdout_chunks = []
        while True:
            reads = [proc.stdout.fileno(), proc.stderr.fileno()]
            ret = select.select(reads, [], [], 1.0)
            if proc.stderr.fileno() in ret[0]:
                err_line = proc.stderr.readline()
                if err_line:
                    print(f"{CYAN}{err_line.rstrip()}{NC}", flush=True)
            if proc.stdout.fileno() in ret[0]:
                out_chunk = proc.stdout.read(4096)
                if out_chunk:
                    stdout_chunks.append(out_chunk)
            if proc.poll() is not None:
                # Read remainder
                rest_out = proc.stdout.read()
                if rest_out:
                    stdout_chunks.append(rest_out)
                rest_err = proc.stderr.read()
                if rest_err:
                    for line in rest_err.splitlines():
                        print(f"{CYAN}{line}{NC}", flush=True)
                break

        stdout_full = "".join(stdout_chunks)
        returncode = proc.returncode

    except Exception as exc:
        print(f"{RED}❌ SkyBrain 5-Lens Review execution error: {exc}{NC}")
        return False

    if returncode != 0:
        print(f"{RED}❌ ReviewEngine returned non-zero code {returncode}{NC}")
        if stdout_full:
            print(stdout_full)
        return False

    # Extract JSON report from stdout
    output = stdout_full
    if "###SKYBRAIN_JSON_START###" not in output or "###SKYBRAIN_JSON_END###" not in output:
        print(f"{RED}❌ Could not parse JSON report from ReviewEngine output.{NC}")
        print(output)
        return False

    json_str = output.split("###SKYBRAIN_JSON_START###")[1].split("###SKYBRAIN_JSON_END###")[0].strip()
    try:
        report_data = json.loads(json_str)
    except Exception as exc:
        print(f"{RED}❌ Error decoding report JSON: {exc}{NC}")
        return False

    total_findings = report_data.get("total_findings", 0)
    critical_count = report_data.get("critical_count", 0)
    high_count = report_data.get("high_count", 0)
    findings = report_data.get("findings", [])

    print(f"\n{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}")
    print(f"{CYAN}{BOLD} 📋 SkyBrain 5-Lens Audit Summary Report                     {NC}")
    print(f"{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}")

    if critical_count > 0:
        print(f"\n{RED}{BOLD}🚨 [CRITICAL BLOCKED] SkyBrain 5-Lens Audit found {critical_count} critical issue(s):{NC}")
        for f in findings:
            if f.get("severity") == "CRITICAL":
                cat = f.get("category", "5-lens")
                principle = f.get("principle_violated", "")
                f_file = f.get("file", "")
                line = f.get("line") or "?"
                desc = f.get("description", "")
                sugg = f.get("suggestion", "")
                print(f"  • {RED}[CRITICAL]{NC} {CYAN}[{cat}] {principle}{NC} ({f_file}:{line}): {desc}")
                if sugg:
                    print(f"    👉 {YELLOW}Fix: {sugg}{NC}")
        print(f"\n{RED}{BOLD}❌ [PUSH BLOCKED] Resolve CRITICAL violations before pushing!{NC}\n")
        return False
    else:
        score = max(70, 100 - (high_count * 5 + (total_findings - high_count) * 2))
        print(f"\n{GREEN}{BOLD}🎉 [PASSED] SkyBrain 5-Lens Neural Audit completed! Score: {score}/100{NC}")
        print(f"{GREEN}   Evaluated 100% on-device by SkyBrain Gemma 4 E4B Metal (Zero-Cloud).{NC}")
        if total_findings > 0:
            print(f"{YELLOW}   (ℹ {total_findings} advisory finding(s) noted - {high_count} HIGH):{NC}")
            for f in findings:
                sev = f.get("severity", "INFO")
                cat = f.get("category", "")
                principle = f.get("principle_violated", "")
                f_file = f.get("file", "")
                line = f.get("line") or "?"
                desc = f.get("description", "")
                sugg = f.get("suggestion", "")
                color = YELLOW if sev == "HIGH" else CYAN
                print(f"     - {color}[{sev}]{NC} [{cat}] {principle} ({f_file}:{line}): {desc}")
                if sugg and sev == "HIGH":
                    print(f"       👉 {sugg}")
        print("")
        return True


def main():
    print(f"\n{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}")
    print(f"{CYAN}{BOLD} 🧠 SkyBrain 5-Lens Quality & Security Neural Audit         {NC}")
    print(f"{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}\n")

    if not check_daemon_alive():
        return 1

    p1 = run_git_pre_audit()
    if not p1:
        return 1

    p2 = run_five_lenses_evaluation()
    if not p2:
        return 1

    print(f"{GREEN}{BOLD}✨ All SkyBrain 5-Lens Quality & Security Gates Succeeded! Safe to Push.{NC}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
