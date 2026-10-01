const mineflayer=require('../../NovaCosmos/target/bot/node_modules/mineflayer');
const fs=require('node:fs');const {once}=require('node:events');const assert=require('node:assert/strict');
const wait=ms=>new Promise(r=>setTimeout(r,ms));
const bot=mineflayer.createBot({host:'127.0.0.1',port:25585,username:'CosmosProbe',version:'1.20.4',auth:'offline'});
let done=false;bot.on('messagestr',m=>{console.log(m);if(m.includes('PROBE_MAP_OK'))done=true;if(m.includes('PROBE_FAIL')){console.error(m);process.exitCode=1;bot.quit();}});
const timer=setTimeout(()=>{console.error('FAIL map timeout');bot.end();process.exit(1)},60000);
bot.once('spawn',async()=>{try{
 await wait(1200);const wallet=()=>fs.readFileSync(`NovaCosmos/target/test-server/plugins/NovaCosmos/players/${bot.player.uuid}.yml`,'utf8');const before=Number(wallet().match(/dust: (\d+)/)[1]);bot.chat('/novaprobe map');for(let i=0;i<150&&!done;i++)await wait(100);assert(done,'Map checks must complete');assert.equal(Number(wallet().match(/dust: (\d+)/)[1]),before+15,'All five mob species credit the shared dust wallet');
 const opened=once(bot,'windowOpen');bot.chat('/zindan');await opened;assert.equal(bot.currentWindow.slots[10].name,'iron_sword');for(let slot=11;slot<=14;slot++)assert.equal(bot.currentWindow.slots[slot].name,'barrier');bot.closeWindow(bot.currentWindow);
 bot.chat('/zindan gir');await wait(3800);assert(Math.abs(bot.entity.position.z-.5)<2&&bot.entity.position.y===81,'Default entry leads to hub');
 console.log('PASS: five-zone world, all connecting bridges, all species/stats, locked crossings, void rescue and five-slot player menu');bot.quit();clearTimeout(timer);
}catch(e){console.error('FAIL',e);clearTimeout(timer);bot.quit();process.exitCode=1;}});
bot.on('error',e=>{console.error(e);clearTimeout(timer);process.exitCode=1});
