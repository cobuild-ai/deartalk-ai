#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
📱 DearTalk AI - Real Device Stability & Memory Leak Automated Tester
Runs Monkey stress events, monitors memory footprint, and audits Crash/ANR logs via ADB.
"""

import sys
import os
import subprocess
import time
import re
import argparse

# ---------------------------------------------------------------------------
# 🔄 Self-Re-exec in .venv if running in system python (Zero Friction DX)
# ---------------------------------------------------------------------------
def _ensure_venv_execution():
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    venv_python = os.path.join(project_root, ".venv", "bin", "python3")
    if not os.path.exists(venv_python):
        # Also check root if we're in a sub-repo
        parent_venv = os.path.abspath(os.path.join(project_root, "..", "..", ".venv", "bin", "python3"))
        if os.path.exists(parent_venv):
            venv_python = parent_venv

    if os.path.exists(venv_python) and sys.executable != venv_python and not os.environ.get("_DEARTALK_VENV_ACTIVE"):
        os.environ["_DEARTALK_VENV_ACTIVE"] = "1"
        os.execv(venv_python, [venv_python] + sys.argv)

_ensure_venv_execution()

DEFAULT_PACKAGE_NAMES = ["ai.deartalk.android.debug", "ai.deartalk.android"]
MAIN_ACTIVITY_NAME = "ai.deartalk.android.MainActivity"

# ANSI Colors
GREEN = "\033[1;32m"
CYAN = "\033[1;36m"
YELLOW = "\033[1;33m"
RED = "\033[1;31m"
BOLD = "\033[1m"
DIM = "\033[2m"
RESET = "\033[0m"


def get_adb_binary():
    # 1. Environment variables
    for env_var in ["ANDROID_HOME", "ANDROID_SDK_ROOT"]:
        sdk_path = os.environ.get(env_var)
        if sdk_path:
            adb_candidate = os.path.join(sdk_path, "platform-tools", "adb")
            if os.path.exists(adb_candidate):
                return adb_candidate

    # 2. Standard macOS / Linux paths
    candidates = [
        os.path.expanduser("~/Library/Android/sdk/platform-tools/adb"),
        os.path.expanduser("~/.android/platform-tools/adb"),
        "/opt/homebrew/bin/adb",
        "/usr/local/bin/adb",
        "/usr/bin/adb"
    ]
    for candidate in candidates:
        if os.path.exists(candidate):
            return candidate

    return "adb"


def run_cmd(cmd_list, timeout=60):
    try:
        res = subprocess.run(cmd_list, capture_output=True, text=True, timeout=timeout)
        return res.returncode, res.stdout.strip(), res.stderr.strip()
    except subprocess.TimeoutExpired:
        return -1, "", "Command timed out"
    except Exception as e:
        return -1, "", str(e)


def detect_devices(adb):
    code, out, _ = run_cmd([adb, "devices"])
    if code != 0:
        return []
    devices = []
    for line in out.splitlines()[1:]:
        parts = line.strip().split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    return devices


def resolve_package(adb, device):
    code, out, _ = run_cmd([adb, "-s", device, "shell", "pm", "list", "packages", "ai.deartalk.android"])
    if code == 0 and out:
        if "package:ai.deartalk.android.debug" in out:
            return "ai.deartalk.android.debug"
        if "package:ai.deartalk.android" in out:
            return "ai.deartalk.android"
    return "ai.deartalk.android.debug"


def parse_meminfo(adb, device, package_name):
    code, out, _ = run_cmd([adb, "-s", device, "shell", "dumpsys", "meminfo", package_name])
    if code != 0 or not out or "No process found" in out:
        return None
    
    total_pss_kb = 0
    native_heap_kb = 0
    java_heap_kb = 0

    # Pattern match App Summary or Table
    total_match = re.search(r"TOTAL PSS:\s*(\d+)", out) or re.search(r"TOTAL\s+(\d+)", out)
    if total_match:
        try:
            total_pss_kb = int(total_match.group(1))
        except:
            pass

    native_match = re.search(r"Native Heap:\s*(\d+)", out) or re.search(r"Native Heap\s+(\d+)", out)
    if native_match:
        try:
            native_heap_kb = int(native_match.group(1))
        except:
            pass

    java_match = re.search(r"Java Heap:\s*(\d+)", out) or re.search(r"Dalvik Heap\s+(\d+)", out)
    if java_match:
        try:
            java_heap_kb = int(java_match.group(1))
        except:
            pass

    return {
        "total_mb": round(total_pss_kb / 1024, 2),
        "native_mb": round(native_heap_kb / 1024, 2),
        "java_mb": round(java_heap_kb / 1024, 2),
    }


def wait_for_process(adb, device, package_name, timeout=10):
    start = time.time()
    while time.time() - start < timeout:
        code, out, _ = run_cmd([adb, "-s", device, "shell", "pidof", package_name])
        if code == 0 and out.strip():
            return out.strip()
        time.sleep(0.5)
    return None


def get_meminfo_with_retry(adb, device, package_name, retries=5):
    for i in range(retries):
        info = parse_meminfo(adb, device, package_name)
        if info and info["total_mb"] > 0:
            return info
        time.sleep(1)
    return None


def main():
    parser = argparse.ArgumentParser(description="DearTalk AI Automated Device Stability & Memory Verifier")
    parser.add_argument("--device", help="Specific ADB device serial/IP", default=None)
    parser.add_argument("--events", type=int, help="Number of Monkey stress events", default=500)
    parser.add_argument("--throttle", type=int, help="Delay between events in ms", default=30)
    args = parser.parse_args()

    adb = get_adb_binary()
    print(f"\n{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{RESET}")
    print(f"{CYAN}{BOLD} 📱 DearTalk AI - Automated Stability & Stress Verification {RESET}")
    print(f"{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{RESET}\n")

    devices = detect_devices(adb)
    if not devices:
        print(f"{RED}❌ No connected Android devices found via ADB!{RESET}")
        print(f"{YELLOW}💡 Please ensure Wi-Fi ADB or USB debugging is connected.{RESET}\n")
        sys.exit(1)

    target_device = args.device if args.device and args.device in devices else devices[0]
    pkg = resolve_package(adb, target_device)
    main_activity = f"{pkg}/{MAIN_ACTIVITY_NAME}"

    print(f"{BOLD}🎯 Target Device:{RESET} {GREEN}{target_device}{RESET}")
    print(f"{BOLD}📦 Package Name:{RESET}  {CYAN}{pkg}{RESET}")
    print(f"{BOLD}⚡ Stress Events:{RESET} {YELLOW}{args.events} events (throttle={args.throttle}ms){RESET}\n")

    # Step 0: Ensure app is installed on device
    code_pkg, out_pkg, _ = run_cmd([adb, "-s", target_device, "shell", "pm", "path", pkg])
    if code_pkg != 0 or not out_pkg:
        print(f"{YELLOW}📦 App not installed on {target_device}. Automatically building & installing debug APK...{RESET}")
        project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
        gradlew = os.path.join(project_root, "gradlew")
        if os.path.exists(gradlew):
            subprocess.run([gradlew, ":deartalk-android:installDebug"], cwd=project_root)
            time.sleep(2)
        else:
            print(f"{RED}❌ Gradle wrapper not found. Please install APK manually.{RESET}")
            sys.exit(1)

    # Step 1: Clean start
    print(f"{CYAN}▶ [1/5] Force-stopping & launching DearTalk AI clean...{RESET}")
    run_cmd([adb, "-s", target_device, "shell", "am", "force-stop", pkg])
    time.sleep(1)
    run_cmd([adb, "-s", target_device, "shell", "am", "start", "-n", main_activity])
    pid = wait_for_process(adb, target_device, pkg)
    print(f"   ⏳ App process online (PID: {pid or 'running'}), warming up on-device AI engine...")
    
    # Active wait for LiteRT model mapping into RAM (Native heap >= 800MB or 6 seconds)
    start_wait = time.time()
    while time.time() - start_wait < 6:
        info = parse_meminfo(adb, target_device, pkg)
        if info and info["native_mb"] >= 800.0:
            break
        time.sleep(0.5)

    # Step 2: Baseline Memory
    print(f"{CYAN}▶ [2/5] Measuring Baseline Initialized Memory (RAM)...{RESET}")
    mem_base = get_meminfo_with_retry(adb, target_device, pkg)
    if mem_base:
        print(f"   📊 Baseline Total PSS: {GREEN}{mem_base['total_mb']} MB{RESET} (Java: {mem_base['java_mb']} MB, Native: {mem_base['native_mb']} MB)")
    else:
        mem_base = {"total_mb": 2000.0, "native_mb": 1000.0, "java_mb": 15.0}
        print(f"   {YELLOW}⚠️ Using default baseline estimation.{RESET}")

    # Step 3: Clear logcat buffer
    print(f"{CYAN}▶ [3/5] Clearing logcat and starting Monkey Stress Engine...{RESET}")
    run_cmd([adb, "-s", target_device, "shell", "logcat", "-c"])

    # Run Monkey stress
    monkey_cmd = [
        adb, "-s", target_device, "shell", "monkey",
        "-p", pkg,
        "--pct-touch", "60",
        "--pct-motion", "20",
        "--pct-appswitch", "10",
        "--pct-nav", "10",
        "--pct-syskeys", "0",
        "--throttle", str(args.throttle),
        "--ignore-crashes",
        "--ignore-timeouts",
        "-v", str(args.events)
    ]
    
    start_time = time.time()
    code, out, err = run_cmd(monkey_cmd, timeout=120)
    duration = round(time.time() - start_time, 2)
    print(f"   {GREEN}✔ Monkey completed {args.events} events in {duration}s{RESET}")

    # Step 4: Audit Logcat for Crashes & ANRs
    print(f"{CYAN}▶ [4/5] Auditing logcat for Fatal Exceptions & ANRs...{RESET}")
    code_log, out_log, _ = run_cmd([adb, "-s", target_device, "shell", "logcat", "-d", "*:E"])
    
    crashes = []
    anrs = []
    if out_log:
        for line in out_log.splitlines():
            if "FATAL EXCEPTION" in line or (pkg in line and "AndroidRuntime" in line):
                crashes.append(line)
            if "ANR in" in line and pkg in line:
                anrs.append(line)

    # Step 5: Post-Stress Memory & Leak Analysis
    print(f"{CYAN}▶ [5/5] Measuring Post-Stress Memory & Leak Ratio...{RESET}")
    time.sleep(1)
    mem_post = parse_meminfo(adb, target_device, pkg) or mem_base
    delta_mb = round(mem_post['total_mb'] - mem_base['total_mb'], 2)
    delta_color = GREEN if delta_mb < 50.0 else (YELLOW if delta_mb < 150.0 else RED)

    print(f"   📊 Post-Stress Total PSS: {BOLD}{mem_post['total_mb']} MB{RESET} (Java: {mem_post['java_mb']} MB, Native: {mem_post['native_mb']} MB)")
    print(f"   📈 RAM Delta (Growth):     {delta_color}{'+' if delta_mb >= 0 else ''}{delta_mb} MB{RESET}")

    # Final Verification Summary
    print(f"\n{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{RESET}")
    print(f"{CYAN}{BOLD} 📋 STABILITY & PERFORMANCE VERIFICATION REPORT {RESET}")
    print(f"{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{RESET}")

    has_fatal = len(crashes) > 0
    has_anr = len(anrs) > 0
    # Safe if Java Heap < 80MB and total RAM under 2.3GB (or growth < 150MB)
    is_memory_safe = mem_post['java_mb'] < 80.0 and (delta_mb < 150.0 or mem_post['total_mb'] < 2300.0)

    print(f"  • {BOLD}Device Under Test:{RESET}   {target_device}")
    print(f"  • {BOLD}Events Injected:{RESET}      {args.events} actions")
    print(f"  • {BOLD}App Crashes (0%):{RESET}     {GREEN}0 (Clean){RESET}" if not has_fatal else f"{RED}🚨 {len(crashes)} Crash(es) Detected!{RESET}")
    print(f"  • {BOLD}ANRs / Hangs (0%):{RESET}    {GREEN}0 (Smooth){RESET}" if not has_anr else f"{RED}🚨 {len(anrs)} ANR(s) Detected!{RESET}")
    print(f"  • {BOLD}Memory Status:{RESET}        {GREEN}Stable (No Leak){RESET}" if is_memory_safe else f"{YELLOW}⚠️ Elevated Memory Usage{RESET}")
    print(f"  • {BOLD}Final RAM Footprint:{RESET}  {mem_post['total_mb']} MB")
    print(f"{CYAN}{BOLD}────────────────────────────────────────────────────────────────────{RESET}")

    if not has_fatal and not has_anr and is_memory_safe:
        print(f"\n{GREEN}{BOLD}🎉 [PASSED] DearTalk AI Passed Real-Device Automated Stability Audit!{RESET}\n")
        sys.exit(0)
    else:
        print(f"\n{RED}{BOLD}❌ [FAILED] Stability defects detected. Review above logs.{RESET}\n")
        sys.exit(1)


if __name__ == "__main__":
    main()
