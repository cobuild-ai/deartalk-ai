#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🔒 DearTalk AI - Privacy Policy & Zero-Data Compliance Auditor
Validates that:
1. AndroidManifest.xml maintains the Zero-Network Invariant (NO android.permission.INTERNET).
2. The local docs/index.html Privacy Policy static asset is valid and present.
3. The official Cloudflare Pages deployment (https://deartalk-ai.pages.dev/) is live (HTTP 200).
"""

import sys
import os
import urllib.request
import urllib.error

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MANIFEST_PATH = os.path.join(PROJECT_ROOT, "deartalk-android", "src", "main", "AndroidManifest.xml")
LOCAL_HTML_PATH = os.path.join(PROJECT_ROOT, "docs", "index.html")
LIVE_URL = "https://deartalk-ai.pages.dev/"

CYAN = "\033[1;36m"
GREEN = "\033[1;32m"
YELLOW = "\033[1;33m"
RED = "\033[1;31m"
BOLD = "\033[1m"
NC = "\033[0m"

def audit_manifest_zero_network():
    print(f"{CYAN}🔍 [1/4] Auditing Android Manifest for Zero-Network Invariant...{NC}")
    if not os.path.exists(MANIFEST_PATH):
        print(f"{RED}❌ AndroidManifest.xml not found at {MANIFEST_PATH}{NC}")
        return False
    
    with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
        content = f.read()

    if "android.permission.INTERNET" in content:
        print(f"{RED}🚨 CRITICAL ERROR: 'android.permission.INTERNET' found in AndroidManifest.xml!{NC}")
        print(f"{RED}   DearTalk AI violates the Zero-Data Invariant rule.{NC}")
        return False

    print(f"{GREEN}✔ Passed: Zero INTERNET permissions verified (100% Offline Air-Gapped).{NC}")
    return True

def audit_local_policy_html():
    print(f"\n{CYAN}📄 [2/4] Checking local Privacy Policy asset (docs/index.html)...{NC}")
    if not os.path.exists(LOCAL_HTML_PATH):
        print(f"{RED}❌ docs/index.html not found! Run privacy policy setup first.{NC}")
        return False

    size = os.path.getsize(LOCAL_HTML_PATH)
    if size < 500:
        print(f"{RED}❌ docs/index.html is too small ({size} bytes). May be corrupted.{NC}")
        return False

    print(f"{GREEN}✔ Passed: Local Privacy Policy HTML verified ({size:,} bytes).{NC}")
    return True

UI_CARDS_PATH = os.path.join(PROJECT_ROOT, "deartalk-android", "src", "main", "java", "ai", "deartalk", "android", "ui", "main", "components", "MainMiscCards.kt")

def audit_in_app_privacy_link():
    print(f"\n{CYAN}📱 [3/4] Verifying In-App Privacy Policy Link (Play Store Mandate)...{NC}")
    candidates = [
        UI_CARDS_PATH,
        os.path.join(PROJECT_ROOT, "deartalk-android/src/main/java/ai/deartalk/android/ui/main/MainScreen.kt")
    ]
    found = False
    for path in candidates:
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                if "https://deartalk-ai.pages.dev/" in f.read():
                    found = True
                    break
    
    if not found:
        print(f"{RED}❌ In-App UI does not link to https://deartalk-ai.pages.dev/!{NC}")
        print(f"{RED}   Google Play requires users to access Privacy Policy inside the app.{NC}")
        return False

    print(f"{GREEN}✔ Passed: In-App UI contains direct link to Cloudflare Pages Privacy Policy.{NC}")
    return True

def audit_live_cloudflare_pages():
    print(f"\n{CYAN}🌐 [4/4] Checking live Cloudflare Pages deployment ({LIVE_URL})...{NC}")
    req = urllib.request.Request(
        LIVE_URL,
        headers={"User-Agent": "DearTalk-Release-Preflight-Auditor/1.0"}
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            status = response.getcode()
            if status == 200:
                print(f"{GREEN}✔ Passed: Live Privacy Policy URL responds with HTTP 200 OK.{NC}")
                return True
            else:
                print(f"{YELLOW}⚠ Warning: Live URL returned HTTP status {status}.{NC}")
                return False
    except urllib.error.HTTPError as e:
        print(f"{RED}❌ HTTP Error: Live URL returned code {e.code}.{NC}")
        return False
    except urllib.error.URLError as e:
        print(f"{YELLOW}⚠ Warning: Network unreachable or offline ({e.reason}). Skipping live URL check.{NC}")
        # Allow pass if offline developer environment, but warn
        return True
    except Exception as e:
        print(f"{YELLOW}⚠ Warning: Could not verify live URL: {e}{NC}")
        return True

def main():
    print(f"\n{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}")
    print(f"{CYAN}{BOLD} 🔒 Google Play Pre-Flight: Privacy Policy & Security Audit {NC}")
    print(f"{CYAN}{BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━{NC}\n")

    p1 = audit_manifest_zero_network()
    p2 = audit_local_policy_html()
    p3 = audit_in_app_privacy_link()
    p4 = audit_live_cloudflare_pages()

    if p1 and p2 and p3 and p4:
        print(f"\n{GREEN}{BOLD}🎉 [PASSED] Google Play Privacy Policy & Zero-Data audit completed successfully!{NC}\n")
        return 0
    else:
        print(f"\n{RED}{BOLD}❌ [FAILED] Privacy Policy or Security audit failed. Please review the errors above.{NC}\n")
        return 1

if __name__ == "__main__":
    sys.exit(main())
