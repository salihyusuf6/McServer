const mineflayer=require('../../NovaCosmos/target/bot/node_modules/mineflayer');
const fs=require('node:fs'),assert=require('node:assert/strict');
const wait=ms=>new Promise(r=>setTimeout(r,ms));
const bot=mineflayer.createBot({host:'127.0.0.1',port:25585,username:'CosmosProbe',version:'1.20.4',auth:'offline'});
let messages=[];bot.on('messagestr',m=>{console.log(m);messages.push(m)});
const timer=setTimeout(()=>{console.error('FAIL timeout');bot.end();process.exit(1)},95000);
async function cmd(text,marker,ms=10000){let from=messages.length;bot.chat(text);for(let i=0;i<ms/100;i++){await wait(100);let recent=messages.slice(from);assert(!recent.some(m=>m.includes('PROBE_FAIL')),recent.join('\n'));const line=recent.find(m=>m.includes(marker));if(line)return line;}throw new Error('Missing '+marker)}
function dust(){return Number(fs.readFileSync(`NovaCosmos/target/test-server/plugins/NovaCosmos/players/${bot.player.uuid}.yml`,'utf8').match(/dust: (\d+)/)[1])}
function keys(){return bot.inventory.items().filter(i=>i.name==='tripwire_hook').reduce((n,i)=>n+i.count,0)}
bot.once('spawn',async()=>{try{
 await wait(1200);await cmd('/novaprobe autospawn','PROBE_AUTO_OK',40000);
 let before=dust(),answer=(await cmd('/novaprobe quiz','PROBE_ANSWER')).split(' ').at(-1);
 bot.chat(String(Number(answer)+1));await wait(700);assert.equal(dust(),before,'Wrong answer gets no dust');
 bot.chat(answer);await wait(1200);assert.equal(dust(),before+5);assert.equal(keys(),1,'Winner gets one vote key');
 bot.chat(answer);await wait(700);assert.equal(dust(),before+5);assert.equal(keys(),1,'Duplicate answer cannot pay again');
 before=dust();answer=(await cmd('/novaprobe quizfull','PROBE_ANSWER')).split(' ').at(-1);bot.chat(answer);await wait(1200);assert.equal(dust(),before+5);assert.equal(keys(),0);
 bot.chat('/matematik teslim');await wait(700);assert.equal(dust(),before+5,'Full inventory retry does not repeat dust');
 await cmd('/novaprobe quizroom','PROBE_ROOM');bot.chat('/matematik teslim');await wait(800);assert.equal(keys(),1);assert.equal(dust(),before+5);bot.chat('/matematik teslim');await wait(500);assert.equal(keys(),1);
 console.log('PASS: automatic spawning tiers 1–5, normal-player tier 3, correct/wrong/duplicate chat answers, one vote key and 5 dust, full inventory delivery and idempotent retry');
 bot.quit();clearTimeout(timer);
}catch(e){console.error('FAIL',e);bot.quit();clearTimeout(timer);process.exitCode=1;}});
bot.on('error',e=>{console.error(e);clearTimeout(timer);process.exitCode=1});
