from PIL import Image, ImageDraw
import math
S=1024; img=Image.new("RGB",(S,S))
d=ImageDraw.Draw(img)
# 背景对角渐变
for y in range(S):
    for x in range(0,S,4):
        t=((x*0.9+y*0.9)/ (2*S*0.9))
        t=max(0,min(1,t))
        if t<0.55:
            u=t/0.55; c=(int(0x11+(0x0D-0x11)*u),int(0x18+(0x14-0x18)*u),int(0x27+(0x20-0x27)*u))
        else:
            u=(t-0.55)/0.45; c=(int(0x0D+(0x08-0x0D)*u),int(0x14+(0x0D-0x14)*u),int(0x20+(0x16-0x20)*u))
        d.rectangle((x,y,x+3,y),fill=c)
# 圆环弧（带缺口）
cx=cy=512; r=240; w=56
# 用大量小段模拟渐变
a=-28; end=288
while a<end:
    a2=min(a+1.2,end)
    t=((a+28)/316)
    if t<0.5: u=t/0.5; col=(int(0x7C+(0x4E-0x7C)*u),int(0x5C+(0x8D-0x5C)*u),int(0xFF+(0xFF-0xFF)*u))
    else: u=(t-0.5)/0.5; col=(int(0x4E+(0x23-0x4E)*u),int(0x8D+(0xE6-0x8D)*u),int(0xFF+(0xBE-0xFF)*u))
    for k in range(0,7):
        aa=math.radians(a+(a2-a)*k/6)
        x=cx+math.cos(aa)*r; y=cy+math.sin(aa)*r
        d.ellipse((x-w/2,y-w/2,x+w/2,y+w/2),fill=col)
    a=a2
# 遮住圆心多余部分（画回背景色不可行，直接再画内圈背景渐变较重）—— 简化为再画环内背景
inner=Image.new("RGB",(S,S),(0x0D,0x14,0x20))
mask=Image.new("L",(S,S),0)
ImageDraw.Draw(mask).ellipse((cx-r+w/2-1,cy-r+w/2-1,cx+r-w/2+1,cy+r-w/2+1),fill=255)
img.paste(inner,(0,0),mask)
d=ImageDraw.Draw(img)
# 刻度线
for (x1,y1,x2,y2) in [(512,202,512,234),(790,512,822,512),(512,790,512,822)]:
    d.line((x1,y1,x2,y2),fill=(0xE7,0xFF,0xF9),width=16)
# 准星
for (x1,y1,x2,y2) in [(512,372,512,442),(512,582,512,652),(372,512,442,512),(582,512,652,512)]:
    d.line((x1,y1,x2,y2),fill=(0xE7,0xFF,0xF9),width=18)
# 中心方块
d.rounded_rectangle((457,457,567,567),radius=15,fill=(0x20,0xE4,0xB9))
d.rounded_rectangle((483,491,541,541),radius=9,fill=(0xB9,0xFF,0xEE))
# 圆形蒙版预览
mask=Image.new("L",(S,S),0); ImageDraw.Draw(mask).ellipse((0,0,S,S),fill=255)
out=Image.new("RGB",(S,S),(20,20,24)); out.paste(img,(0,0),mask)
out.resize((432,432),Image.LANCZOS).save(r"E:\三角洲行动\apk\out\icon-preview-v17.png")
print("ok")
