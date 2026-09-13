#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
📸 DearTalk 3개 국어(KO, EN, ID) 실기기 대표 스크린샷 자동 캡처기
- 대상: Samsung Galaxy S22 Ultra (192.168.1.136:39951)
- 1) DearTalk Live 실시간 2-Way 대화 화면 (KO, EN, ID)
- 2) DearTalk Live AI 팩 & 하드웨어 진단 설정 시트 화면 (KO, EN, ID)
"""

import os
import sys
import time
import subprocess
from datetime import datetime

DEVICE = "192.168.1.136:39951"
PKG = "ai.deartalk.android.debug"
ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
OUT_DIR = os.path.join(ROOT_DIR, "Journal", "2026", "첨부파일")
os.makedirs(OUT_DIR, exist_ok=True)

LANGUAGES = [
    {
        "lang": "ko",
        "title": "한국어 (Korean)",
        "turn1": ("ME", "QUESTION", "안녕하세요, 서울역 가는 길 알려주실 수 있나요?"),
        "turn2": ("PARTNER", "STATEMENT", "Sure, just take subway line 1 and get off at the next stop.")
    },
    {
        "lang": "en",
        "title": "영어 (English)",
        "turn1": ("ME", "QUESTION", "Hello, could you tell me how to get to Seoul Station?"),
        "turn2": ("PARTNER", "STATEMENT", "네, 1호선을 타시고 다음 역에서 내리시면 됩니다.")
    },
    {
        "lang": "id",
        "title": "인도네시아어 (Bahasa Indonesia)",
        "turn1": ("ME", "QUESTION", "Halo, bisakah Anda memberi tahu saya jalan ke Stasiun Seoul?"),
        "turn2": ("PARTNER", "STATEMENT", "Sure, you can take subway line 1 and get off at the next stop.")
    }
]


def run_cmd(args):
    return subprocess.run(args, capture_output=True, text=True)


def adb_cmd(cmd_list):
    full_cmd = ["adb", "-s", DEVICE] + cmd_list
    return run_cmd(full_cmd)


def capture(filename):
    out_path = os.path.join(OUT_DIR, filename)
    adb_cmd(["shell", "screencap", "-p", "/sdcard/screen_temp.png"])
    adb_cmd(["pull", "/sdcard/screen_temp.png", out_path])
    adb_cmd(["shell", "rm", "/sdcard/screen_temp.png"])
    if os.path.exists(out_path):
        size_kb = os.path.getsize(out_path) // 1024
        print(f"📸 캡처 완료: {filename} ({size_kb} KB)")
        return out_path
    else:
        print(f"❌ 캡처 실패: {filename}")
        return None


def main():
    print(f"🚀 [3개 국어 대표 스크린샷 캡처 시작] 디바이스: {DEVICE}")

    captured_files = []

    for item in LANGUAGES:
        lang = item["lang"]
        title = item["title"]
        print(f"\n==================================================")
        print(f"🌐 [{title}] 대표 스크린샷 생성 중...")
        print(f"==================================================")

        # 1. 새 세션으로 DearTalkLiveActivity 기동 (해당 로케일 적용)
        adb_cmd([
            "shell", "am", "start", "-S",
            "-n", f"{PKG}/ai.deartalk.android.live.DearTalkLiveActivity",
            "--es", "test_locale", lang
        ])
        time.sleep(2.5)

        # 2. 2-Turn 실시간 대화 인젝션
        speaker1, intent1, text1 = item["turn1"]
        print(f"💬 Turn 1 주입: [{speaker1}/{intent1}] '{text1}'")
        adb_cmd([
            "shell", "am", "start",
            "-n", f"{PKG}/ai.deartalk.android.live.DearTalkLiveActivity",
            "--es", "test_speaker", speaker1,
            "--es", "test_intent", intent1,
            "--es", "test_text", text1
        ])
        time.sleep(4.5)

        speaker2, intent2, text2 = item["turn2"]
        print(f"💬 Turn 2 주입: [{speaker2}/{intent2}] '{text2}'")
        adb_cmd([
            "shell", "am", "start",
            "-n", f"{PKG}/ai.deartalk.android.live.DearTalkLiveActivity",
            "--es", "test_speaker", speaker2,
            "--es", "test_intent", intent2,
            "--es", "test_text", text2
        ])
        time.sleep(4.5)

        # 3. Live 실시간 대면 통역 메인 화면 캡처
        live_shot_name = f"showcase_live_{lang}.png"
        f1 = capture(live_shot_name)
        if f1:
            captured_files.append((f"Live 대화 ({title})", live_shot_name))

        # 4. 설정 시트(AI 팩 & 하드웨어 진단 카드) 오픈
        print(f"⚙️ 설정 시트(AI 팩 진단) 오픈 중...")
        adb_cmd([
            "shell", "am", "start",
            "-n", f"{PKG}/ai.deartalk.android.live.DearTalkLiveActivity",
            "--es", "test_locale", lang,
            "--ez", "open_settings", "true"
        ])
        time.sleep(2.0)

        settings_shot_name = f"showcase_settings_{lang}.png"
        f2 = capture(settings_shot_name)
        if f2:
            captured_files.append((f"AI 팩 & 진단 설정 ({title})", settings_shot_name))

    print("\n🎉 [3개 국어 스크린샷 캡처 완료]")
    for desc, fname in captured_files:
        print(f"  - {desc}: ![[{fname}]]")


if __name__ == "__main__":
    main()
