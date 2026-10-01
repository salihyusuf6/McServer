const mineflayer=require('../../NovaCosmos/target/bot/node_modules/mineflayer');
const assert=require('node:assert/strict');const fs=require('node:fs');const {once}=require('node:events');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const bot=mineflayer.createBot({host:'127.0.0.1',port:25585,username:'CosmosProbe',version:'1.20.4',auth:'offline'});
let messages=[];bot.on('messagestr',m=>{console.log('CHAT',m);messages.push(m)});bot.on('error',console.error);
const timeout=setTimeout(()=>{console.error('FAIL timeout');bot.end();process.exit(1)},90000);
async function command(cmd,marker){let begin=messages.length;bot.chat(cmd);for(let i=0;i<150;i++){await sleep(100);let lines=messages.slice(begin);if(lines.some(m=>m.includes('PROBE_FAIL')))throw new Error(lines.find(m=>m.includes('PROBE_FAIL')));if(lines.some(m=>m.includes(marker)))return;}throw new Error('Missing '+marker)}
function wallet(){return fs.readFileSync(`NovaCosmos/target/test-server/plugins/NovaCosmos/players/${bot.player.uuid}.yml`,'utf8')}
function dust(){return Number(wallet().match(/dust: (\d+)/)[1])}
bot.once('spawn',async()=>{try{
 await sleep(1200);const before=dust();await command('/novaprobe prepare','PROBE_PREPARED');assert.equal(dust(),before+12,'Crate dust persists exactly once');
 let opened=once(bot,'windowOpen');bot.chat('/crate preview vote');await opened;let diamonds=bot.inventory.items().filter(i=>i.name==='diamond').reduce((s,i)=>s+i.count,0);await bot.clickWindow(1,0,1);await sleep(300);assert.equal(bot.inventory.items().filter(i=>i.name==='diamond').reduce((s,i)=>s+i.count,0),diamonds,'Preview shift click cannot steal');bot.closeWindow(bot.currentWindow);
 opened=once(bot,'windowOpen');bot.chat('/zindan');await opened;assert.equal(bot.currentWindow.slots[15].name,'barrier','Tier 3 locked in real menu');bot.closeWindow(bot.currentWindow);
 bot.chat('/zindan gir 1');await sleep(4200);const preCombat=dust();await command('/novaprobe combat','PROBE_COMBAT_OK');assert.equal(dust(),preCombat+6,'Boss dust credits existing Skyblock wallet');
 bot.chat('/zindan cik');await sleep(4200);await command('/novaprobe visual','PROBE_VISUAL_OK');
 console.log('PASS: real protocol menus, shift-click protection, persistent crate dust, dungeon entry/exit, boss reward, anchored crystal visuals');
 bot.quit();clearTimeout(timeout);
}catch(e){console.error('FAIL',e);bot.quit();clearTimeout(timeout);process.exitCode=1;}});
