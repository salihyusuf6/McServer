#!/usr/bin/env python3
"""Install built plugins/config changes; run only with the main Skyblock server stopped."""
from pathlib import Path
import shutil,datetime,socket,re
ROOT=Path(__file__).resolve().parents[1];live=ROOT/'skyblockserver'
with socket.socket() as sock:
 if sock.connect_ex(('127.0.0.1',25566))==0:raise SystemExit('Önce ana sunucuyu normal şekilde durdur.')
backup=ROOT/'backups'/datetime.datetime.now().strftime('deploy-%Y%m%d-%H%M%S');backup.mkdir(parents=True)
def preserve(path):
 target=backup/path.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True)
 if path.exists():shutil.copy2(path,target)
for module,file in [('NovaCosmos','NovaCosmos-1.0.0.jar'),('NovaCrates','NovaCrates-1.0-SNAPSHOT.jar'),('NovaDungeon','NovaDungeon-1.0-SNAPSHOT.jar')]:
 dest=live/'plugins'/file;preserve(dest);shutil.copy2(ROOT/module/'target'/file,dest)
p=live/'plugins/NovaCrates/config.yml';preserve(p);old=p.read_text()
match=re.search(r'^crate-locations:.*?(?=^\S|\Z)',old,flags=re.M|re.S)
locations=match.group(0).strip() if match else 'crate-locations: {}'
p.write_text((ROOT/'NovaCrates/src/main/resources/config.yml').read_text()+'\n'+locations+'\n')
p=live/'plugins/NovaDungeon/config.yml';preserve(p);p.parent.mkdir(exist_ok=True)
if not p.exists():shutil.copy2(ROOT/'NovaDungeon/src/main/resources/config.yml',p)
p=live/'plugins/SkBee/config.yml';preserve(p);p.write_text(p.read_text().replace('display-entity: true','display-entity: false'))
p=live/'plugins/Multiverse-Core/worlds.yml';preserve(p);s=p.read_text();s=re.sub(r'(TEST:\n(?:(?!\n\S).)*?auto-load:) true',r'\1 false',s,flags=re.S);p.write_text(s)
p=ROOT/'a/velocity.toml';preserve(p);s=p.read_text();s=re.sub(r'try = \[\s*"lobby"\s*\]','try = [\n    "skyblock"\n]',s);s=s.replace('motd = "<#09add3>BannanCraft network"','motd = "<aqua>✦ NOVA SKYBLOCK</aqua> <dark_gray>•</dark_gray> <light_purple>Yıldız Zindanı & Kozmik Kasalar</light_purple>"');p.write_text(s)
p=live/'server.properties';preserve(p);s=p.read_text();s=re.sub(r'^motd=.*$',r'motd=Nova Skyblock | Yildiz Zindani ve Kozmik Kasalar',s,flags=re.M);p.write_text(s)
p=live/'spigot.yml';preserve(p);p.write_text(p.read_text().replace('bungeecord: true','bungeecord: false'))
p=live/'config/paper-global.yml';preserve(p);p.write_text(p.read_text().replace('  velocity:\n    enabled: false','  velocity:\n    enabled: true'))
report=ROOT/'tools/qa/latest-test.log'
lines=(ROOT/'NovaCosmos/target/test-server/logs/latest.log').read_text().splitlines()
report.write_text('\n'.join(line for line in lines if '[NovaProbe] PASS:' in line)+'\n')
print('Kurulum tamamlandı. Yedek:',backup)
