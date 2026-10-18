#!/usr/bin/env python3
"""Generate an illustrative demo, with no phone footage or user data. Requires Pillow and ffmpeg."""
from pathlib import Path
import math, subprocess
from PIL import Image, ImageDraw, ImageFont
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/demo"
OUT.mkdir(parents=True, exist_ok=True)
W, H, FPS, SECONDS = 1280, 720, 12, 36
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
def font(size): return ImageFont.truetype(FONT, size)
scenes = [
 ("Camera Wake", "No ADB activation", ["TELESIN P4-WTP-06 + Xiaomi 17 Ultra", "Android 17 / HyperOS", "Illustration of user-tested behavior"], "off"),
 ("1. Update the remote", "V5.7  →  V5.9", ["Use the official ReefColor app", "Select Diving Phone Case", "Verify V5.9 after the update"], "firmware"),
 ("2. Pair again", "Forget → power cycle → pair", ["Android Bluetooth settings", "Remove only TELESIN_SDC", "Fresh pairing loads the new controls"], "pair"),
 ("3. Enable Camera Wake", "One-time setup", ["Install Beta 5; enable Accessibility", "Allow unrestricted battery / autostart", "Enable camera on screen wake"], "setup"),
 ("4. Use the controls", "Hold M1 to wake Camera", ["Short M1 / M2: switch modes", "W / T: zoom out / in", "Capture: photo / video control"], "camera"),
 ("5. Hold M1 to sleep", "Clean black transition", ["Temporary guard absorbs remote gestures", "No PC or startup script during use", "Turn wake mode off after case use"], "sleep"),
]
proc = subprocess.Popen(["ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-", "-an", "-c:v", "libx264", "-crf", "23", "-pix_fmt", "yuv420p", "-map_metadata", "-1", "-movflags", "+faststart", str(OUT / "camera-wake-beta5-demo.mp4")], stdin=subprocess.PIPE)
for frame in range(FPS * SECONDS):
 t = frame / FPS; index = min(int(t // 6), len(scenes)-1); local = t % 6
 title, sub, lines, state = scenes[index]
 im = Image.new("RGB", (W,H), "#0b1220"); d = ImageDraw.Draw(im)
 d.text((64,40), "CAMERA WAKE  /  BETA 5", fill="#6de0cb", font=font(22))
 d.text((64,130), title, fill="white", font=font(42))
 d.text((64,210), sub, fill="#6de0cb", font=font(29))
 for n,line in enumerate(lines): d.text((64,315+n*57), line, fill="#d1d9e6", font=font(23))
 d.rounded_rectangle((900,80,1190,610), radius=40, fill="#26354b", outline="#63748c", width=3)
 d.rounded_rectangle((913,94,1177,594), radius=30, fill="black")
 if state in ("firmware","pair","setup"):
  d.text((938,230), {"firmware":"ReefColor", "pair":"Bluetooth", "setup":"Wake enabled"}[state], fill="white", font=font(23))
  if state=="firmware":
   d.text((955,300), "V5.7 → V5.9", fill="#6de0cb", font=font(20)); d.rectangle((940,365,1150,380),fill="#23374b"); d.rectangle((940,365,940+int(210*min(local/4,1)),380),fill="#6de0cb")
  elif state=="pair": d.text((948,310), "TELESIN_SDC", fill="#6de0cb", font=font(20))
  else: d.rounded_rectangle((1000,315,1100,355),radius=20,fill="#6de0cb"); d.ellipse((1065,320,1095,350),fill="white")
 elif state=="camera" or (state=="sleep" and local<1.3):
  if state=="camera" and local<1.6: pass
  else:
   d.rectangle((925,170,1165,440),fill="#152c36")
   d.text((950,270), "SIMULATED",fill="#7897a6",font=font(20))
   d.text((980,310), f"{1+int(max(0,local-2)//1)%3}x",fill="white",font=font(32))
   d.text((946,465), "PHOTO   VIDEO",fill="#d1d9e6",font=font(18)); d.ellipse((1015,510,1075,570),outline="white",width=5)
 if state in ("camera","sleep"):
  d.text((940,625), "M1: HOLD",fill="#6de0cb",font=font(22))
 d.text((64,603), "ILLUSTRATIVE WALKTHROUGH • NO PHONE FOOTAGE",fill="#93a4bd",font=font(19))
 d.text((64,638), "Test scope and limitations: github.com/shon3l/camera-wake",fill="#93a4bd",font=font(18))
 d.rectangle((0,708,int(W*(frame+1)/(FPS*SECONDS)),720),fill="#6de0cb")
 if frame==FPS*27: im.save(OUT/"poster.png")
 proc.stdin.write(im.tobytes())
proc.stdin.close()
if proc.wait(): raise SystemExit("ffmpeg failed")
print(OUT / "camera-wake-beta5-demo.mp4")
