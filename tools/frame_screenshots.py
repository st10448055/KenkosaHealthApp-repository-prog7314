#!/usr/bin/env python3
"""Turns REAL emulator screenshots into Play Store promo images (1080x1920, captioned).
Usage: put PNGs in store/screenshots/raw/ named  01-home.png, 02-diary.png ...  then run this script.
Captions come from CAPTIONS below (keyed by the part of the filename after the number)."""
from PIL import Image, ImageDraw, ImageFont
import glob, os
CAPTIONS = {
 "login": "Secure sign-in with Google or email",
 "home": "Your whole health day at a glance",
 "diary": "Log meals in seconds",
 "barcode": "Scan barcodes to track food",
 "water": "Stay on top of your hydration",
 "fasting": "Track fasts with a clear ring view",
 "sleep": "Understand your sleep week",
 "medication": "Never miss a dose",
 "wellness": "Log your mood and unwind",
 "goals": "Set goals and watch progress",
 "settings": "Biometric lock and languages",
}
B='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
W,H=1080,1920
os.makedirs('store/screenshots/framed',exist_ok=True)
files=sorted(glob.glob('store/screenshots/raw/*.png'))
if not files: raise SystemExit("No screenshots in store/screenshots/raw/ - capture them from the running app first.")
for f in files:
    key=os.path.splitext(os.path.basename(f))[0].split('-',1)[-1].lower()
    cap=CAPTIONS.get(key,key.replace('_',' ').title())
    bg=Image.new('RGB',(W,H)); d=ImageDraw.Draw(bg)
    c1,c2=(0x0F,0x28,0x12),(0x6E,0x8C,0x68)
    for y in range(H):
        t=y/H; d.line([(0,y),(W,y)],fill=tuple(int(c1[i]+(c2[i]-c1[i])*t) for i in range(3)))
    font=ImageFont.truetype(B,64); words=cap.split(); lines=[]; cur=''
    for w in words:
        if d.textlength((cur+' '+w).strip(),font=font)<W-160: cur=(cur+' '+w).strip()
        else: lines.append(cur); cur=w
    lines.append(cur)
    y=110
    for l in lines:
        d.text(((W-d.textlength(l,font=font))/2,y),l,font=font,fill='white'); y+=84
    shot=Image.open(f).convert('RGB'); tw=800; th=int(shot.height*tw/shot.width)
    maxh=H-y-90
    if th>maxh: th=maxh; tw=int(shot.width*th/shot.height)
    shot=shot.resize((tw,th),Image.LANCZOS)
    mask=Image.new('L',shot.size,0); ImageDraw.Draw(mask).rounded_rectangle([0,0,*shot.size],radius=48,fill=255)
    x=(W-tw)//2
    d.rounded_rectangle([x-10,y+30,x+tw+10,y+40+th],radius=56,fill=(20,20,20))
    bg.paste(shot,(x,y+35),mask)
    out=f'store/screenshots/framed/{os.path.basename(f)}'; bg.save(out); print('wrote',out)
