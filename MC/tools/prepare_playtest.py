from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[1];test=ROOT/'NovaCosmos/target/test-server';live=ROOT/'skyblockserver'
for name in ['Vault(2).jar','EssentialsX-2.21.2(1).jar','worldedit-bukkit-7.3.0.jar','worldguard-bukkit-7.0.9-dist.jar','multiverse-core-5.5.3.jar']:
 shutil.copy2(live/'plugins'/name,test/'plugins'/name)
for module,file in [('NovaCosmos','NovaCosmos-1.0.0.jar'),('NovaCrates','NovaCrates-1.0-SNAPSHOT.jar'),('NovaDungeon','NovaDungeon-1.0-SNAPSHOT.jar')]:
 shutil.copy2(ROOT/module/'target'/file,test/'plugins'/file)
if not (test/'dungeon').exists():shutil.copytree(live/'dungeon',test/'dungeon')
(test/'plugins/WorldGuard/worlds/dungeon').mkdir(parents=True,exist_ok=True)
shutil.copy2(live/'plugins/WorldGuard/worlds/dungeon/regions.yml',test/'plugins/WorldGuard/worlds/dungeon/regions.yml')
(test/'plugins/Multiverse-Core').mkdir(exist_ok=True)
s=(live/'plugins/Multiverse-Core/worlds.yml').read_text();start=s.index('dungeon:');end=s.index('\nworld:',start)
(test/'plugins/Multiverse-Core/worlds.yml').write_text(s[start:end]+'\n')
print(test)
