// Connection/auth gating only. Does not register an account or change privileges.
const mineflayer=require('../../NovaCosmos/target/bot/node_modules/mineflayer');
const bot=mineflayer.createBot({host:'127.0.0.1',port:25565,username:'NovaNetProbe',version:'1.20.4',auth:'offline'});
let messages=[];bot.on('messagestr',m=>{messages.push(m);console.log(m)});
const timer=setTimeout(()=>{console.error('FAIL timeout');bot.end();process.exitCode=1},15000);
bot.once('spawn',()=>setTimeout(()=>{bot.chat('/zindan');setTimeout(()=>{
 const auth=messages.some(m=>/register|kayıt|kayit|login|giriş/i.test(m));
 if(auth&&!bot.currentWindow)console.log('PASS: proxy routes to Skyblock; AuthMe blocks dungeon until login');
 else {console.error('FAIL: authentication prompt/menu gating not verified');process.exitCode=1;}
 clearTimeout(timer);bot.quit();
},1500)},1000));
bot.once('kicked',r=>{console.error('KICK',JSON.stringify(r));clearTimeout(timer);process.exitCode=1});bot.once('error',e=>{console.error(e.message);clearTimeout(timer);process.exitCode=1});
