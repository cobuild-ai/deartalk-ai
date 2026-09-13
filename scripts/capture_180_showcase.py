#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
🔄 DearTalk 180도 대면 모드(Flip View) 3개 국어(KO, EN, ID) 실기기 캡처기
- 대상: Samsung Galaxy S22 Ultra (192.168.1.136:39951)
- 180도 상하 대칭 회전 대화 화면 (showcase_live_180_ko.png, en.png, id.png)
"""

import os
import sys
import time
import subprocess

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


def adb_cmd(cmd_list):
    full_cmd = ["adb", "-s", DEVICE] + cmd_list
    return subprocess.run(full_cmd, capture_output=True, text=True)


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
    print(f"🚀 [DearTalk Live 180도 대면 모드 3개 국어 캡처 시작] 디바이스: {DEVICE}")

    captured_files = []

    for item in LANGUAGES:
        lang = item["lang"]
        title = item["title"]
        print(f"\n==================================================")
        print(f"🔄 [{title}] 180도 대면 모드 캡처 중...")
        print(f"==================================================")

        # 1. 180도 모드로 DearTalkLiveActivity 기동
        adb_cmd([
            "shell", "am", "start", "-S",
            "-n", f"{PKG}/ai.deartalk.android.live.DearTalkLiveActivity",
            "--es", "test_locale", lang,
            "--ez", "flip_view", "true"
        ])
        time.sleep(2.5)

        # 2. 2-Turn 대화 주입
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

        # 3. 스크린샷 캡처
        shot_name = f"showcase_live_180_{lang}.png"
        f = capture(shot_name)
        if f:
            captured_files.append((f"180° 대면 통역 ({title})", shot_name))

    print("\n🎉 [180도 대면 모드 3개 국어 캡처 완료]")
    for desc, fname in captured_files:
        print(f"  - {desc}: ![[{fname}]]")


if __name__ == "__main__":
    main()
