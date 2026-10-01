#!/usr/bin/env python3
"""Local Nova network supervisor. Graceful Paper/Velocity shutdown, no shell eval."""
from pathlib import Path
import fcntl,os,signal,socket,subprocess,sys,time,json
ROOT=Path(__file__).resolve().parents[1];RUN=ROOT/'run';RUN.mkdir(exist_ok=True)
PID=RUN/'manager.pid';LOG=RUN/'manager.log';SCRIPT=Path(__file__).resolve();CONTROL=RUN/'console.sock'
def alive():
 try:
  pid=int(PID.read_text());args=subprocess.check_output(['/bin/ps','-p',str(pid),'-o','args='],text=True).strip()
  return pid if str(SCRIPT) in args and args.endswith(' run') else None
 except (OSError,ValueError,subprocess.CalledProcessError):return None
def port(number):
 with socket.socket() as s:s.settimeout(.3);return s.connect_ex(('127.0.0.1',number))==0
def run():
 lock=(RUN/'manager.lock').open('w')
 try:fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
 except BlockingIOError:return
 PID.write_text(str(os.getpid()));children=[];stopping=False
 CONTROL.unlink(missing_ok=True);control=socket.socket(socket.AF_UNIX,socket.SOCK_STREAM);control.bind(str(CONTROL));os.chmod(CONTROL,0o600);control.listen(4);control.settimeout(1)
 def request_stop(*args):
  nonlocal stopping
  stopping=True
 signal.signal(signal.SIGTERM,request_stop);signal.signal(signal.SIGINT,request_stop)
 try:
  specs=[('skyblock',ROOT/'skyblockserver',Path(os.environ.get('COSMOS_JAVA_HOME','/opt/homebrew/opt/openjdk@17'))/'bin/java','server.jar','-Xmx3G','stop'),('proxy',ROOT/'a',Path(os.environ.get('NOVA_PROXY_JAVA_HOME','/opt/homebrew/opt/openjdk@25'))/'bin/java','proxy.jar','-Xmx512M','shutdown')]
  for name,cwd,java,jar,heap,stop in specs:
   stream=(RUN/f'{name}.log').open('a');process=subprocess.Popen([str(java),'-Xms256M',heap,'-jar',jar,*(['--nogui'] if name=='skyblock' else [])],cwd=cwd,stdin=subprocess.PIPE,stdout=stream,stderr=subprocess.STDOUT,text=True)
   children.append((name,process,stop,stream));print(name,'started',process.pid,flush=True)
  while not stopping:
   if any(p.poll() is not None for _,p,_,_ in children):print('A service stopped; shutting down network.',flush=True);break
   try:
    conn,_=control.accept()
    with conn:
     conn.settimeout(2);data=json.loads(conn.recv(8192));target=data['target'];command=data['command']
     if '\n' in command or '\r' in command:raise ValueError('One console command per request')
     selected=next((p for name,p,_,_ in children if name==target),None)
     if selected is None:raise ValueError('Unknown server')
     selected.stdin.write(command+'\n');selected.stdin.flush();conn.sendall(b'OK')
   except socket.timeout:pass
   except (OSError,ValueError,KeyError) as e:print('Console request failed:',type(e).__name__,flush=True)
 finally:
  # Stop the proxy first to prevent new logins, then save the backend normally.
  for name,p,command,stream in reversed(children):
   if p.poll() is None:
    try:p.stdin.write(command+'\n');p.stdin.flush()
    except (BrokenPipeError,OSError):pass
  for name,p,command,stream in reversed(children):
   try:p.wait(timeout=60)
   except subprocess.TimeoutExpired:
    print(name,'did not stop in 60 seconds; sending TERM.',flush=True);p.terminate()
    try:p.wait(timeout=30)
    except subprocess.TimeoutExpired:print(name,'still running; inspect manually.',flush=True)
   stream.close()
  control.close();CONTROL.unlink(missing_ok=True);PID.unlink(missing_ok=True)
if __name__=='__main__':
 command=sys.argv[1] if len(sys.argv)>1 else 'status'
 if command=='run':run()
 elif command=='start':
  if alive():print('Nova zaten çalışıyor.');sys.exit(0)
  if port(25565) or port(25566):raise SystemExit('25565/25566 kullanımda; mevcut süreci önce kontrol et.')
  with LOG.open('a') as log:subprocess.Popen([sys.executable,str(SCRIPT),'run'],stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
  print('Nova başlatılıyor. Durum: python3 tools/serverctl.py status')
 elif command=='stop':
  pid=alive()
  if pid:os.kill(pid,signal.SIGTERM);print('Normal kapanış istendi; dünyalar kaydediliyor.')
  else:print('Nova yöneticisi çalışmıyor.')
 elif command=='console':
  if len(sys.argv)<4:raise SystemExit('console skyblock|proxy <komut>')
  with socket.socket(socket.AF_UNIX,socket.SOCK_STREAM) as sock:
   sock.settimeout(3);sock.connect(str(CONTROL));sock.sendall(json.dumps({'target':sys.argv[2],'command':' '.join(sys.argv[3:])}).encode());print(sock.recv(512).decode())
 elif command=='status':print(json.dumps({'manager':bool(alive()),'proxy_25565':port(25565),'skyblock_25566':port(25566)},ensure_ascii=False))
 else:raise SystemExit('Kullanım: serverctl.py start|stop|status')
