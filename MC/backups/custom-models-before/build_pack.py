#!/usr/bin/env python3
"""Minecraft-native voxel equipment; only built-in textures, no mods or external artwork."""
from pathlib import Path
import json, zipfile, hashlib

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'resource-pack/source'
PALETTE = {
    'frame': ('obsidian', '#25213f'), 'metal': ('iron_block', '#c8d5e1'),
    'cyan': ('diamond_block', '#60ede7'), 'light': ('sea_lantern', '#e2fffb'),
    'violet': ('amethyst_block', '#a47bf3'), 'gold': ('gold_block', '#ffcb5e'),
    'green': ('emerald_block', '#63e89b'), 'handle': ('netherite_block', '#454251'),
}

def box(x, y, z, w, h, d, color):
    return {'from': [x, y, z], 'to': [x+w, y+h, z+d], 'shade': True,
            'faces': {f: {'uv': [0, 0, 16, 16], 'texture': '#' + color} for f in ['north','south','east','west','up','down']}}

def sword(color, nova=False):
    e = [box(7, 0, 7, 2, 5, 2, 'handle'), box(6.5, 0, 6.5, 3, 1, 3, color),
         box(4, 4, 6.5, 8, 1.5, 3, 'frame'), box(3, 4.5, 7, 2, 2, 2, color),
         box(11, 4.5, 7, 2, 2, 2, color), box(6, 5.5, 7, 4, 7.5, 2, 'metal'),
         box(6.5, 13, 7, 3, 1, 2, color), box(7, 14, 7, 2, 2, 2, 'light'),
         box(7.5, 5, 6.8, 1, 9, 2.4, color), box(7, 3.5, 6, 2, 2, 4, 'light')]
    for y in [1.5, 3]: e.append(box(6.8, y, 6.8, 2.4, .35, 2.4, color))
    if nova:
        for x in [4.5, 10.5]:
            e += [box(x, 7, 7, 1, 5, 2, 'frame'), box(x, 11, 6.8, 1, 2, 2.4, color)]
    return e

def pick(color, meteor=False):
    e = [box(7, 0, 7, 2, 12, 2, 'handle'), box(6.5, 0, 6.5, 3, 1, 3, color),
         box(2, 11, 6.5, 12, 2, 3, 'frame'), box(3, 13, 7, 10, 1, 2, color),
         box(1, 9, 7, 2, 3, 2, 'metal'), box(13, 9, 7, 2, 3, 2, 'metal'),
         box(0, 7, 7, 1.5, 3, 2, color), box(14.5, 7, 7, 1.5, 3, 2, color),
         box(6, 10, 6, 4, 4, 4, color), box(7, 11, 5.8, 2, 2, 4.4, 'light')]
    for y in [2, 5, 8]: e.append(box(6.8, y, 6.8, 2.4, .6, 2.4, color))
    if meteor: e += [box(5, 14, 7, 2, 2, 2, 'gold'), box(9, 14, 7, 2, 2, 2, 'gold')]
    return e

def hoe():
    return [box(7, 0, 7, 2, 14, 2, 'handle'), box(6.5, 0, 6.5, 3, 1, 3, 'green'),
            box(2, 12, 6.5, 10, 2, 3, 'frame'), box(1, 9, 7, 2, 4, 2, 'green'),
            box(3, 14, 7, 8, 1, 2, 'green'), box(6, 11, 6, 4, 4, 4, 'metal'),
            box(7, 12, 5.8, 2, 2, 4.4, 'light'), box(6.8, 4, 6.8, 2.4, 1, 2.4, 'green'),
            box(6.8, 8, 6.8, 2.4, 1, 2.4, 'green')]

def relic():
    e = [box(6, 5, 6, 4, 6, 4, 'violet'), box(7, 3, 7, 2, 10, 2, 'light'),
         box(2, 6, 7, 1, 4, 2, 'frame'), box(13, 6, 7, 1, 4, 2, 'frame'),
         box(6, 2, 7, 4, 1, 2, 'frame'), box(6, 13, 7, 4, 1, 2, 'frame')]
    for x,y in [(3,4),(3,10),(11,4),(11,10)]: e.append(box(x,y,7,2,2,2,'cyan'))
    return e

ITEMS = [
    ('ay_kazmasi','iron_pickaxe',9101,'Ay Kazması','SELENE / 01','cyan',25,'8 saniye Acele II',pick('cyan')),
    ('meteor_kazmasi','diamond_pickaxe',9102,'Meteor Kazması','IMPACT / 02','gold',160,'Eğilerek 3 × 3 kazı',pick('gold',True)),
    ('pulsar_kilici','iron_sword',9103,'Pulsar Kılıcı','PULSAR / 03','violet',65,'Tam vuruşta +2 canavar hasarı',sword('violet')),
    ('nova_kilici','diamond_sword',9104,'Süpernova Kılıcı','SUPERNOVA / 04','violet',240,'+4 canavar hasarı · Güç I',sword('violet',True)),
    ('yorunge_capasi','diamond_hoe',9105,'Yörünge Çapası','ORBIT / 05','green',100,'Hasat sonrası tohumla yeniden ekim',hoe()),
    ('bosluk_tilsimi','amethyst_shard',9106,'Boşluk Tılsımı','VOID / 06','cyan',120,'12 saniye yavaş düşüş',relic()),
]

def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2)+'\n')

write(PACK/'pack.mcmeta', {'pack': {'pack_format':22,'description':'§bNova Cosmos §8| §dUzay Ekipmanları §7• 1.20.4'}})
cards = []
for id, base, model_id, title, code, color, cost, ability, elements in ITEMS:
    model = {'credit':'Original voxel design • Nova Cosmos',
             'textures': {k: 'minecraft:block/'+v[0] for k,v in PALETTE.items()},
             'elements':elements, 'gui_light':'front',
             'display':{'gui':{'rotation':[0,0,-35],'translation':[0,0,0],'scale':[.85,.85,.85]},
                        'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.4,.4,.4]},
                        'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[.8,.8,.8]},
                        'thirdperson_righthand':{'rotation':[0,-90,55],'translation':[0,4,.5],'scale':[.85,.85,.85]},
                        'thirdperson_lefthand':{'rotation':[0,90,-55],'translation':[0,4,.5],'scale':[.85,.85,.85]},
                        'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1.13,3.2,1.13],'scale':[.68,.68,.68]},
                        'firstperson_lefthand':{'rotation':[0,90,-25],'translation':[1.13,3.2,1.13],'scale':[.68,.68,.68]}}}
    model['textures']['particle'] = 'minecraft:block/'+PALETTE[color][0]
    write(PACK/f'assets/novacosmos/models/item/{id}.json', model)
    write(PACK/f'assets/minecraft/models/item/{base}.json',
          {'parent':'minecraft:item/handheld' if base!='amethyst_shard' else 'minecraft:item/generated',
           'textures':{'layer0':'minecraft:item/'+base},
           'overrides':[{'predicate':{'custom_model_data':model_id},'model':f'novacosmos:item/{id}'},
                        {'predicate':{'custom_model_data':model_id+1},'model':'minecraft:item/'+base+'_cosmos_fallback'}]})
    write(PACK/f'assets/minecraft/models/item/{base}_cosmos_fallback.json',
          {'parent':'minecraft:item/handheld' if base!='amethyst_shard' else 'minecraft:item/generated',
           'textures':{'layer0':'minecraft:item/'+base}})
    # Front projection of the actual exported voxel geometry, no separate concept art.
    rects=[]
    for e in elements:
        x,y,z=e['from']; xx,yy,zz=e['to']; c=e['faces']['north']['texture'][1:]
        rects.append(f'<rect x="{x}" y="{16-yy}" width="{xx-x}" height="{yy-y}" fill="{PALETTE[c][1]}" stroke="#0005" stroke-width=".1"/>')
    svg='<svg viewBox="-4 -4 24 24" role="img" aria-label="'+title+' modelinin önden görünümü">'+''.join(rects)+'</svg>'
    cards.append(f'<article style="--accent:{PALETTE[color][1]}"><small>{code}</small><div class="visual">{svg}</div><h2>{title}</h2><p>{ability}</p><footer><span>✦ {cost} yıldız tozu</span><span>MK {model_id-9100:02}</span></footer></article>')

html='''<!doctype html><html lang="tr"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Nova Cosmos — Uzay Cephaneliği</title>
<style>*{box-sizing:border-box}body{margin:0;background:#090c14;color:#ebedf7;font:15px system-ui,sans-serif}main{max-width:1200px;margin:auto;padding:60px 28px}nav{display:flex;justify-content:space-between;color:#84d6d6;letter-spacing:3px;font-size:12px}header{padding:65px 0 45px;max-width:760px}h1{font-size:clamp(40px,7vw,76px);line-height:1.05;letter-spacing:-3px;margin:20px 0}header p{color:#99a6bb;line-height:1.8;max-width:540px}em{font-style:normal;color:#8af2e1}.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}article{border:1px solid #263044;background:linear-gradient(150deg,#161d2d,#10141e);padding:25px;border-radius:12px;transition:transform .2s,border-color .2s}article:hover{transform:translateY(-5px);border-color:var(--accent)}small{color:var(--accent);font-size:10px;letter-spacing:3px}.visual{height:230px;display:grid;place-items:center;background:radial-gradient(ellipse,#ffffff08,transparent 70%)}svg{height:220px;filter:drop-shadow(0 10px 10px #0007);transform:rotate(-30deg)}h2{font-size:22px;letter-spacing:-.5px;margin:10px 0}article p{color:#9aa5b9;font-size:13px;min-height:38px}footer{display:flex;justify-content:space-between;border-top:1px solid #ffffff12;padding-top:18px;color:var(--accent);font-size:12px}.guide{margin:36px 0;padding:26px;border:1px solid #283346;border-radius:12px;color:#a7b5c8;line-height:1.9}.guide strong{color:#e2f6f4}code{color:#8af2e1}.note{font-size:12px;color:#77849a}@media(max-width:800px){.grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:520px){.grid{grid-template-columns:1fr}main{padding:30px 18px}}</style>
<main><nav><b>✦ NOVA COSMOS</b><span>SKYBLOCK / 1.20.4</span></nav><header><small>YÖRÜNGE İSTASYONU · EKİPMAN ARŞİVİ</small><h1>Adandan<br><em>yıldızlara.</em></h1><p>Ay madenlerinden süpernova enerjisine. Kendi adanı geliştir, kozmik görevleri tamamla ve uzay teknolojilerini üret.</p></header><section class="grid">'''+''.join(cards)+'''</section><div class="guide"><strong>İlk uçuş</strong><br><code>/is</code> ile ada oluştur → <code>/uzay baslangic</code> ile 25 toz al → Ay Kazması üret.<br>Jeneratör madenciliği, doğal canavarlar ve olgun hasatlar günlük görevleri ilerletir.<br><strong>Kozmik olaylar</strong> · Meteor Madenciliği / Kozmik İstila / Yıldız Hasadı<br>90 dakika bekleme · 15 dakika etkinlik · Kişisel hedefi tamamlayana 45 yıldız tozu.</div><p class="note">Görseller, paketteki gerçek 3B model geometrisinin önden izdüşümüdür. Oyun içi blok dokusu ve ışıklandırma farklı görünür. Minecraft Java 1.20.4 kaynak paketi; mod gerektirmez.</p></main></html>'''
(ROOT/'TASARIMLAR.html').write_text(html)
archive=ROOT/'resource-pack/NovaCosmos-1.20.4.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for path in sorted(PACK.rglob('*')):
        if path.is_file(): z.write(path,path.relative_to(PACK))
sha=hashlib.sha1(archive.read_bytes()).hexdigest()
(archive.parent/'SHA1.txt').write_text(sha+'\n')
for path in PACK.rglob('*.json'):
    d=json.loads(path.read_text())
    for e in d.get('elements',[]):
        assert all(-16<=v<=32 for v in e['from']+e['to'])
        assert all(a<b for a,b in zip(e['from'],e['to']))
        assert all(f['texture'][1:] in d['textures'] for f in e['faces'].values())
    for override in d.get('overrides',[]):
        ns, model=override['model'].split(':')
        assert (PACK/f'assets/{ns}/models/{model}.json').exists()
print(f'PASS: 6 native voxel models, overrides, bounds, textures; SHA1 {sha}')
print(archive)
