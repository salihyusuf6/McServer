"""Original Java item geometry and original procedural surface textures; no vanilla assets."""
import struct,zlib
PALETTE={'frame':('frame','#20243c'),'metal':('metal','#b9cde2'),'cyan':('cyan','#39ddec'),'light':('light','#e9ffff'),'violet':('violet','#a06afa'),'gold':('gold','#ffad39'),'green':('green','#53e8ab'),'handle':('handle','#38455e')}
def texture(path,color):
    rgb=tuple(bytes.fromhex(color[1:])); rows=[]
    for y in range(16):
        row=bytearray([0])
        for x in range(16):
            # Crisp edge bevel, brushed center, narrow etched seam. Own pixels.
            f=.62 if min(x,y,15-x,15-y)==0 else (1.16 if x<3 or y<2 else .88+(x%3)*.035)
            if y in (5,11) and x in range(5,11):f=.72
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
    e=shaft(c,4.2)
    if not nova:
        e += [box(6.95,5.3,7.4,2.1,8.1,1.2,'metal'),box(7.65,5.6,7.2,.7,8.5,1.6,'violet'),box(7.25,13.1,7.35,1.5,1.5,1.3,'light',45),box(4.7,4.5,7,3.2,.8,2,'frame',22.5),box(8.1,4.5,7,3.2,.8,2,'frame',-22.5),box(7.1,4.25,6.7,1.8,1.8,2.6,'violet',45),box(4.4,4.8,7.1,.7,1.2,1.8,'cyan'),box(10.9,4.8,7.1,.7,1.2,1.8,'cyan')]
    else:
        e += [box(6,6.1,7,4,6.8,2,'frame'),box(5.65,7,7.2,.65,5.4,1.6,'metal'),box(9.7,7,7.2,.65,5.4,1.6,'metal'),box(6.65,12,7,2.7,2.7,2,'violet',45),box(7.5,6,6.7,1,7.5,2.6,'light'),box(4.8,5.2,6.8,2.8,1.2,2.4,'gold',-22.5),box(8.4,5.2,6.8,2.8,1.2,2.4,'gold',22.5),box(3.8,5.7,7,1,2.5,2,'violet',-22.5),box(11.2,5.7,7,1,2.5,2,'violet',22.5),box(7,4.8,6.2,2,2,3.6,'violet',45)]
    return e
def hoe():
    return shaft('green',12.2)+[box(6,11.5,6.6,4,2,2.8,'frame'),box(3.2,12.5,7,4.7,.9,2,'metal',22.5),box(1.5,10.2,7,.9,3.5,2,'green',-22.5),box(1.1,8,7.3,.65,3,1.4,'light',-22.5),box(10,11.9,7,2.3,1,2,'green',-22.5),box(6.9,11.7,6.25,2,2,3.5,'green',45),box(7.45,12.25,6.1,.9,.9,3.8,'light',45)]
def relic():
    e=[]
    # Octagonal orbital frame surrounding a faceted diamond crystal.
    for x,y,w,h,a in [(4.6,13,6.8,.7,0),(4.6,2.3,6.8,.7,0),(2.3,4.6,.7,6.8,0),(13,4.6,.7,6.8,0),(2.9,11.7,3.2,.7,45),(9.9,11.7,3.2,.7,-45),(2.9,3.6,3.2,.7,-45),(9.9,3.6,3.2,.7,45)]:e.append(box(x,y,7.4,w,h,1.2,'metal',a))
    e += [box(6,6,6,4,4,4,'violet',45),box(6.9,6.9,5.6,2.2,2.2,4.8,'cyan',45),box(7.6,4,7.5,.8,8,1,'light'),box(7,13,7,2,2,2,'gold',45),box(7,1,7,2,2,2,'gold',45)]
    return e
