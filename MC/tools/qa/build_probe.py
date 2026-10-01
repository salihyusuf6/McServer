from pathlib import Path
import subprocess,os,zipfile,json
ROOT=Path(__file__).resolve().parents[2];test=ROOT/'NovaCosmos/target/test-server';out=ROOT/'tools/qa/classes';out.mkdir(exist_ok=True)
jars=list((ROOT/'skyblockserver/libraries').rglob('*.jar'))+list((test/'plugins').glob('*.jar'))
subprocess.run(['/opt/homebrew/opt/openjdk@17/bin/javac','--release','17','-cp',os.pathsep.join(map(str,jars)),'-d',str(out),str(ROOT/'tools/qa/NovaProbe.java')],check=True)
with zipfile.ZipFile(test/'plugins/NovaProbe.jar','w') as z:
 for f in out.rglob('*.class'):z.write(f,f.relative_to(out))
 z.writestr('plugin.yml','name: NovaProbe\nversion: 1.0\nmain: org.nova.qa.NovaProbe\napi-version: "1.20"\ndepend: [NovaDungeon]\ncommands:\n  novaprobe:\n    permission: novaprobe.admin\npermissions:\n  novaprobe.admin:\n    default: op\n')
(test/'ops.json').write_text(json.dumps([{'uuid':'6dc0bcb8-6ee6-37c9-92c6-23da5da7e03e','name':'CosmosProbe','level':4,'bypassesPlayerLimit':True}]))
p=test/'plugins/NovaCosmos/config.yml';s=p.read_text();s=s.replace('resource-pack:\n  enabled: true','resource-pack:\n  enabled: false');p.write_text(s)
