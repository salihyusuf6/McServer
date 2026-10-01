"""Original Java item geometry and original procedural surface textures; no vanilla assets."""
import struct,zlib
PALETTE={'frame':('frame','#20243c'),'metal':('metal','#b9cde2'),'cyan':('cyan','#39ddec'),'light':('light','#e9ffff'),'violet':('violet','#a06afa'),'gold':('gold','#ffad39'),'green':('green','#53e8ab'),'handle':('handle','#38455e')}
PALETTE.update({'blade':('blade','#667a98'),'edge':('edge','#e0ecfa'),'inlay':('inlay','#9563ec'),'leather':('leather','#202638'),'trim':('trim','#c6a56c')})
def texture(path,color):
    rgb=tuple(bytes.fromhex(color[1:])); rows=[]
    for y in range(16):
        row=bytearray([0])
        for x in range(16):
            # Crisp edge bevel, brushed center, narrow etched seam. Own pixels.
            f=.62 if min(x,y,15-x,15-y)==0 else (1.16 if x<3 or y<2 else .88+(x%3)*.035)
            if y in (5,11) and x in range(5,11):f=.72
            if path.stem in ('blade','edge','inlay','trim'):
                f=.76+.30*(1-abs(x-7.5)/7.5)
                if x in (1,2):f=1.12
            row.extend([min(255,round(v*f)) for v in rgb]+[255])
        rows.append(row)
    def chunk(t,d):return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>2I5B',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b''))
def box(x,y,z,w,h,d,c,angle=0):
    e={'name':c,'from':[x,y,z],'to':[x+w,y+h,z+d],'faces':{f:{'uv':[0,0,16,16],'texture':'#'+c} for f in ['north','south','east','west','up','down']}}
    if angle:e['rotation']={'origin':[x+w/2,y+h/2,z+d/2],'axis':'z','angle':angle}
    return e
def shaft(c,height=10):
    e=[box(7.4,.5,7.3,1.2,height,1.4,'handle'),box(7.1,.3,7,1.8,.7,2,c)]
    for y in (2,3.4,4.8):e.append(box(7.2,y,7.1,1.6,.35,1.8,'frame'))
    return e
def pick(c,meteor=False):
    e=shaft(c,10.6)
    if meteor:
        e += [box(4.8,10,6.4,6.5,3,3.2,'frame'),box(5.2,12.4,6.7,5.7,.6,2.6,'gold'),box(3,9.6,6.8,2,3,2.4,'metal',-22.5),box(1.5,7.7,7,1.3,3.5,2,'gold',-22.5),box(11.3,10.5,6.6,2.6,2.3,2.8,'metal'),box(13.5,10.8,7,1.5,1.5,2,'gold',45),box(6.4,10.1,6.1,2.8,2.8,3.8,'gold',45),box(7.2,10.9,5.95,1.2,1.2,4.1,'light',45)]
    else:
        e += [box(3.4,11.9,7.1,9.2,1.3,1.8,'metal'),box(2.2,10.8,7.1,1.6,3,1.8,'cyan',-45),box(1,8.8,7.15,1.1,2.5,1.7,'cyan',-22.5),box(12.2,10.8,7.1,1.6,3,1.8,'cyan',45),box(13.9,8.8,7.15,1.1,2.5,1.7,'cyan',22.5),box(4,13.1,7.3,8,.35,1.4,'light'),box(6.6,10.9,6.6,2.8,2.8,2.8,'frame',45),box(7.1,11.4,6.35,1.8,1.8,3.3,'cyan',45)]
    return e
def sword(c,nova=False):
    # A continuous tapered blade, with a narrow recessed fuller and joined guard.
    # Each short band meets its neighbours: no floating cubes or block ornaments.
    e=[box(7.42,.6,7.45,1.16,3.8,1.1,'leather'),
       box(7.25,.35,7.28,1.5,.65,1.44,'trim' if nova else 'metal'),
       box(7.3,3.65,7.3,1.4,.65,1.4,'trim' if nova else 'metal')]
    for y in (1,1.55,2.1,2.65,3.2):
        e.append(box(7.39,y,7.42,1.22,.12,1.16,'handle'))
    # Guard curves toward the blade, with a compact inset stone at its centre.
    accent='trim' if nova else 'metal'
    e += [box(5.2,4.05,7.35,5.6,.65,1.3,accent),
          box(4.3,4.25,7.4,1.5,.5,1.2,accent,22.5),
          box(10.2,4.25,7.4,1.5,.5,1.2,accent,-22.5),
          box(7.45,4.05,7.18,1.1,.75,1.64,'inlay')]
    # Premium longsword proportions, slender edge strips and a fully joined point.
    bands=40; bottom=4.65; length=11.0
    for i in range(bands):
        t=i/bands; y=bottom+length*t; h=length/bands
        width=(2.95 if nova else 2.1)*(1-.30*t)
        if t>.78:width*=max(.055,(1-t)/.22)
        x=8-width/2; bevel=min(.18,width*.22); depth=.90 if nova else .65
        e.append(box(x+bevel,y,8-depth/2,width-2*bevel,h,depth,'blade'))
        e.append(box(x,y,8-depth*.31,bevel,h,depth*.62,'edge'))
        e.append(box(8+width/2-bevel,y,8-depth*.31,bevel,h,depth*.62,'edge'))
        if .07<t<.82:
            # A fine violet energy line set into the face, not a separate bar.
            channel=min(.23,width*.16)
            e.append(box(8-channel/2,y,8-depth/2-.012,channel,h,depth+.024,'inlay'))
    if nova:
        for y in (5.4,6.3,7.2):
            e += [box(7.2,y,7.535,.48,.08,.93,'trim',-22.5),
                  box(8.32,y,7.535,.48,.08,.93,'trim',22.5)]
    return e
def hoe():
    return shaft('green',12.2)+[box(6,11.5,6.6,4,2,2.8,'frame'),box(3.2,12.5,7,4.7,.9,2,'metal',22.5),box(1.5,10.2,7,.9,3.5,2,'green',-22.5),box(1.1,8,7.3,.65,3,1.4,'light',-22.5),box(10,11.9,7,2.3,1,2,'green',-22.5),box(6.9,11.7,6.25,2,2,3.5,'green',45),box(7.45,12.25,6.1,.9,.9,3.8,'light',45)]
def relic():
    e=[]
    # Octagonal orbital frame surrounding a faceted diamond crystal.
    for x,y,w,h,a in [(4.6,13,6.8,.7,0),(4.6,2.3,6.8,.7,0),(2.3,4.6,.7,6.8,0),(13,4.6,.7,6.8,0),(2.9,11.7,3.2,.7,45),(9.9,11.7,3.2,.7,-45),(2.9,3.6,3.2,.7,-45),(9.9,3.6,3.2,.7,45)]:e.append(box(x,y,7.4,w,h,1.2,'metal',a))
    e += [box(6,6,6,4,4,4,'violet',45),box(6.9,6.9,5.6,2.2,2.2,4.8,'cyan',45),box(7.6,4,7.5,.8,8,1,'light'),box(7,13,7,2,2,2,'gold',45),box(7,1,7,2,2,2,'gold',45)]
    return e
