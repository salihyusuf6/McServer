#!/usr/bin/env python3
"""Compile against the exact installed Paper/plugin APIs, without downloading new versions."""
from pathlib import Path
import os, subprocess, shutil, zipfile
ROOT=Path(__file__).resolve().parents[1]
JAVA=Path(os.environ.get('COSMOS_JAVA_HOME','/opt/homebrew/opt/openjdk@17'))/'bin'
subprocess.run(['python3',str(ROOT/'NovaCosmos/tools/build.py')],check=True)
jars=sorted((ROOT/'skyblockserver/libraries').rglob('*.jar'))+list((ROOT/'skyblockserver/plugins').glob('*.jar'))
jars=[p for p in jars if not p.name.startswith(('NovaCrates-','NovaDungeon-','NovaCosmos-'))]
jars.append(ROOT/'NovaCosmos/target/NovaCosmos-1.0.0.jar')
for name in ['NovaCrates','NovaDungeon']:
 module=ROOT/name;classes=module/'target/classes'
 if classes.exists():shutil.rmtree(classes)
 classes.mkdir(parents=True)
 subprocess.run([str(JAVA/'javac'),'--release','17','-encoding','UTF-8','-cp',os.pathsep.join(map(str,jars)),'-d',str(classes),*map(str,sorted((module/'src/main/java').rglob('*.java')))],check=True)
 output=module/'target'/f'{name}-1.0-SNAPSHOT.jar'
 with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as z:
  for directory in [classes,module/'src/main/resources']:
   for file in sorted(directory.rglob('*')):
    if file.is_file():z.write(file,file.relative_to(directory))
 jars.append(output);print(output)
# Pure concurrency rules for the first-correct-answer competition.
quiz_tests=ROOT/'NovaDungeon/target/test-classes';quiz_tests.mkdir(exist_ok=True)
subprocess.run([str(JAVA/'javac'),'--release','17','-d',str(quiz_tests),str(ROOT/'NovaDungeon/src/main/java/org/nova/novaDungeon/quiz/MathRound.java'),str(ROOT/'NovaDungeon/src/test/java/org/nova/novaDungeon/quiz/MathRoundTest.java')],check=True)
subprocess.run([str(JAVA/'java'),'-ea','-cp',str(quiz_tests),'org.nova.novaDungeon.quiz.MathRoundTest'],check=True)
