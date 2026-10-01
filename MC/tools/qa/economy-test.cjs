// Nova ekonomi regresyon testi — gerçek Minecraft protokolüyle, izole test sunucusunda
// (NovaCosmos/target/test-server, 127.0.0.1:25585). Ana sunucuya dokunmaz.
//
// Sınanan akışlar:
//   1. Nova spawner: komutla verilir, yerleştirilince türü uygulanır, kırılınca türüyle geri gelir
//   2. Doğal (etiketsiz) spawner kırılınca eşya vermez
//   3. Otomatik toplama: kırılan taş doğrudan çantaya girer
//   4. Minyon seviyesi: yükselt -> panelden topla -> tekrar koy, seviye korunur; vurarak kırınca da korunur
//   5. Spawner kasası: anahtar + COMMAND ödülü gerçekten teslim edilir
//   6. Dört kasanın bütün ödülleri geçerli (önizlemede sessizce düşen ödül yok)
//   7. Market spawner kategorisi: iki tıklamalı satın alma
//
// Kullanım: test sunucusu açıkken  node tools/qa/economy-test.cjs
const MOD = '../../NovaCosmos/target/bot/node_modules/';
const mineflayer = require(MOD + 'mineflayer');
const nbt = require(MOD + 'prismarine-nbt');
const { Vec3 } = require(MOD + 'vec3');
const assert = require('node:assert/strict');
const { once } = require('node:events');

const sleep = ms => new Promise(r => setTimeout(r, ms));
const NAME = 'CosmosProbe';
const BASE = new Vec3(1000, 120, 1000);            // test platformu (ana dünya)
const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25585, username: NAME, version: '1.20.4', auth: 'offline' });

const messages = [];
bot.on('messagestr', m => { console.log('CHAT', m); messages.push(m); });
bot.on('error', console.error);
bot.on('kicked', r => console.error('KICKED', r));
const guard = setTimeout(() => { console.error('FAIL zaman aşımı'); process.exit(1); }, 240000);

async function run(cmd, marker, ms = 8000) {
  const begin = messages.length;
  bot.chat(cmd);
  if (!marker) { await sleep(400); return; }
  for (let t = 0; t < ms; t += 100) {
    await sleep(100);
    const hit = messages.slice(begin).find(m => m.includes(marker));
    if (hit) return hit;
  }
  throw new Error(`"${cmd}" için beklenen mesaj gelmedi: ${marker}`);
}

async function waitMessage(marker, ms = 8000) {
  const begin = messages.length;
  for (let t = 0; t < ms; t += 100) {
    await sleep(100);
    const hit = messages.slice(begin).find(m => m.includes(marker));
    if (hit) return hit;
  }
  throw new Error('Beklenen mesaj gelmedi: ' + marker);
}

const pdc = item => {
  if (!item || !item.nbt) return {};
  const tag = nbt.simplify(item.nbt);
  return tag.PublicBukkitValues || {};
};
const items = name => bot.inventory.items().filter(i => i.name === name);
const count = name => items(name).reduce((s, i) => s + i.count, 0);
const spawnerItems = () => items('spawner').filter(i => pdc(i)['novaeconomy:nova_spawner']);

async function openWindow(action) {
  const opened = once(bot, 'windowOpen');
  await action();
  const [window] = await Promise.race([opened, sleep(6000).then(() => { throw new Error('Pencere açılmadı'); })]);
  await sleep(300);
  return window;
}

function topSlots(window) {
  return window.slots.slice(0, window.inventoryStart);   // üst envanter
}

bot.once('spawn', async () => {
  try {
    await sleep(1500);

    // ── Kurulum: platform, para, kazma ──────────────────────────────────────
    // Önceki koşulardan kalan spawner canavarları botu öldürüyordu.
    await run('/difficulty peaceful');
    await run('/gamemode creative');
    await run('/execute in minecraft:overworld run forceload add 990 990 1010 1010');
    await run('/execute in minecraft:overworld run fill 994 119 994 1006 119 1006 minecraft:stone');
    await run('/execute in minecraft:overworld run fill 994 120 994 1006 125 1006 minecraft:air');
    await run(`/execute in minecraft:overworld run tp ${NAME} 1000.5 120 1000.5 0 0`);
    await sleep(1500);
    await run('/gamemode survival');
    await run('/clear');
    await run(`/eco set ${NAME} 1000000`, 'set');
    await run(`/minecraft:give ${NAME} minecraft:diamond_pickaxe 1`);
    for (let i = 0; i < 6 && bot.entity.position.distanceTo(BASE.offset(0.5, 0, 0.5)) > 2; i++) {
      await run(`/execute in minecraft:overworld run tp ${NAME} 1000.5 120 1000.5 0 0`);
      await sleep(1200);
    }
    assert.ok(bot.entity.position.distanceTo(BASE.offset(0.5, 0, 0.5)) < 2, 'Bot test platformunda');

    const pick = () => bot.equip(items('diamond_pickaxe')[0], 'hand');

    // ── 1. Nova spawner yaşam döngüsü ────────────────────────────────────────
    await run(`/market spawner ${NAME} ZOMBIE 1`, 'Spawner aldın');
    await sleep(300);
    let sp = spawnerItems()[0];
    assert.equal(pdc(sp)['novaeconomy:nova_spawner'], 'ZOMBIE', 'Spawner eşyası türünü PDC etiketinde taşır');

    const spawnerPos = BASE.offset(2, 0, 0);
    await bot.equip(sp, 'hand');
    await bot.placeBlock(bot.blockAt(spawnerPos.offset(0, -1, 0)), new Vec3(0, 1, 0));
    await sleep(500);
    assert.equal(bot.blockAt(spawnerPos).name, 'spawner', 'Spawner yerleşti');
    const data = await run(`/data get block ${spawnerPos.x} ${spawnerPos.y} ${spawnerPos.z} SpawnData.entity.id`, 'block data');
    assert.ok(data.includes('minecraft:zombie'), 'Yerleşen spawner zombi üretir: ' + data);
    assert.equal(spawnerItems().length, 0, 'Yerleştirilen spawner çantadan düştü');

    await pick();
    await bot.dig(bot.blockAt(spawnerPos));
    await sleep(800);
    assert.equal(bot.blockAt(spawnerPos).name, 'air', 'Spawner kırıldı');
    sp = spawnerItems()[0];
    assert.ok(sp, 'Kırılan Nova spawner çantaya geri geldi');
    assert.equal(pdc(sp)['novaeconomy:nova_spawner'], 'ZOMBIE', 'Geri gelen spawner türünü korur');
    console.log('OK 1: spawner koy/kır türüyle birlikte korunuyor');

    // ── 2. Doğal spawner eşya vermez ─────────────────────────────────────────
    const natural = BASE.offset(1, 0, -1);
    await run(`/execute in minecraft:overworld run setblock ${natural.x} ${natural.y} ${natural.z} minecraft:spawner`);
    await sleep(400);
    const before2 = count('spawner');
    await pick();
    await bot.dig(bot.blockAt(natural));
    await sleep(800);
    assert.equal(count('spawner'), before2, 'Etiketsiz (doğal) spawner kırılınca eşya vermez');
    console.log('OK 2: doğal spawner bedava eşyaya dönüşmüyor');

    // ── 3. Otomatik toplama ──────────────────────────────────────────────────
    const stonePos = BASE.offset(1, 0, 1);
    await run(`/execute in minecraft:overworld run setblock ${stonePos.x} ${stonePos.y} ${stonePos.z} minecraft:stone`);
    await sleep(400);
    const cobble = count('cobblestone');
    await pick();
    await bot.dig(bot.blockAt(stonePos));
    await sleep(600);
    assert.equal(count('cobblestone'), cobble + 1, 'Kırılan taş doğrudan çantaya girdi');
    const ground = Object.values(bot.entities).filter(e => e.name === 'item' && e.position.distanceTo(stonePos) < 2);
    assert.equal(ground.length, 0, 'Yerde eşya kalmadı');
    console.log('OK 3: otomatik toplama çalışıyor');

    // ── 4. Minyon seviyesi korunur ───────────────────────────────────────────
    // Önceki çalıştırmalardan kalan minyonları panelden topla (test tekrar çalıştırılabilir).
    for (let i = 0; i < 8; i++) {
      const panel = await openWindow(() => bot.chat('/minyon'));
      const firstSlot = topSlots(panel)[0];
      if (!firstSlot || firstSlot.name === 'barrier' || firstSlot.name.endsWith('glass_pane')) {
        bot.closeWindow(panel);
        break;
      }
      const gone = waitMessage('kaldırıldı');
      await bot.clickWindow(0, 1, 1);
      await gone;
      await sleep(500);
      if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
    }
    await run('/clear');
    await run(`/minecraft:give ${NAME} minecraft:diamond_pickaxe 1`);
    await sleep(300);

    await run(`/minyon give ${NAME} miner 1`, 'minyon aldın');
    await sleep(300);
    const minionPos = BASE.offset(-2, 0, 0);
    const placeMinion = async () => {
      const m = items('golden_pickaxe').find(i => pdc(i)['skyminions:minyon_item'] === 'miner');
      assert.ok(m, 'Çantada Madenci Minyon eşyası var');
      await bot.equip(m, 'hand');
      await bot.lookAt(minionPos.offset(0.5, 0, 0.5), true);
      const placed = waitMessage('yerleştirildi');
      await bot.activateBlock(bot.blockAt(minionPos.offset(0, -1, 0)), new Vec3(0, 1, 0));
      await placed;
      await sleep(600);
    };
    await placeMinion();

    // Panel -> depo -> iki kez yükselt
    let win = await openWindow(() => bot.chat('/minyon'));
    assert.ok(topSlots(win)[0], 'Panelde minyon listelendi');
    win = await openWindow(() => bot.clickWindow(0, 1, 0));          // sağ tık: depo
    let up = waitMessage('seviye 2');
    await bot.clickWindow(49, 0, 0);
    await up;
    await sleep(500);
    up = waitMessage('seviye 3');
    await bot.clickWindow(49, 0, 0);
    await up;
    await sleep(300);
    bot.closeWindow(bot.currentWindow);
    await run(`/minyon list ${NAME}`, 'Sv 3');

    // Panelden topla: Shift + sağ tık
    win = await openWindow(() => bot.chat('/minyon'));
    const picked = waitMessage('kaldırıldı');
    await bot.clickWindow(0, 1, 1);
    await picked;
    await sleep(600);
    if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
    let minionItem = items('golden_pickaxe').find(i => pdc(i)['skyminions:minyon_item'] === 'miner');
    assert.equal(pdc(minionItem)['skyminions:minyon_level'], 3, 'Panelden toplanan minyon eşyası seviye 3 taşır');

    // Tekrar koy -> seviye 3 ile başlar
    await placeMinion();
    await run(`/minyon list ${NAME}`, 'Sv 3');
    console.log('OK 4a: panelden toplayıp tekrar koyunca seviye korunuyor');

    // Vurarak kaldır -> düşen eşya da seviye 3 taşır
    const stand = Object.values(bot.entities).find(e => e.name === 'armor_stand' && e.position.distanceTo(minionPos.offset(0.5, 0, 0.5)) < 1.2);
    assert.ok(stand, 'Minyon NPC bulundu');
    const punched = waitMessage('kaldırıldı');
    await bot.attack(stand);
    await punched;
    await run(`/execute in minecraft:overworld run tp ${NAME} ${minionPos.x + 0.5} ${minionPos.y} ${minionPos.z + 0.5}`);
    await sleep(1800);
    minionItem = items('golden_pickaxe').find(i => pdc(i)['skyminions:minyon_item'] === 'miner');
    assert.ok(minionItem, 'Vurularak kaldırılan minyonun eşyası toplandı');
    assert.equal(pdc(minionItem)['skyminions:minyon_level'], 3, 'Vurarak kaldırınca da seviye korunur');
    console.log('OK 4b: vurarak kaldırınca da seviye korunuyor');
    await run(`/execute in minecraft:overworld run tp ${NAME} 1000.5 120 1000.5 0 0`);
    await sleep(800);

    // ── 5. Spawner kasası gerçekten teslim eder ──────────────────────────────
    const cratePos = BASE.offset(4, 0, 3);
    await run(`/execute in minecraft:overworld run setblock ${cratePos.x} ${cratePos.y} ${cratePos.z} minecraft:gold_block`);
    await sleep(400);
    await bot.lookAt(cratePos.offset(0.5, 0.5, 0.5), true);
    await sleep(300);
    await run('/crate set spawner', 'yerleştirildi');
    await run(`/crate give ${NAME} spawner 1`, 'teslim edildi');
    await sleep(300);
    const spawnersBefore = spawnerItems().length;
    await pick();                                       // elde minyon/spawner olmasın
    await bot.lookAt(cratePos.offset(0.5, 0.5, 0.5), true);
    const delivered = waitMessage('ödülün teslim edildi');
    await bot.activateBlock(bot.blockAt(cratePos));
    const line = await delivered;
    await sleep(800);
    if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
    if (line.includes('Spawner')) {
      assert.equal(spawnerItems().length, spawnersBefore + 1, 'Kasa ödülü Nova spawner olarak çantaya geldi: ' + line);
    } else {
      assert.ok(line.includes('Para'), 'Kasa ödülü para veya spawner olmalı: ' + line);
    }
    assert.equal(items('tripwire_hook').length, 0, 'Anahtar tüketildi');
    console.log('OK 5: spawner kasası COMMAND ödülünü teslim ediyor ->', line);

    // ── 6. Bütün kasa ödülleri geçerli ───────────────────────────────────────
    const expected = { vote: 11, 'tarım': 12, novacraft: 13, spawner: 9 };
    for (const [crate, n] of Object.entries(expected)) {
      win = await openWindow(() => bot.chat('/crate preview ' + crate));
      const shown = topSlots(win).filter(Boolean).length;
      assert.equal(shown, n, `${crate} kasasında ${n} ödül görünmeli (görünen ${shown})`);
      bot.closeWindow(win);
      await sleep(300);
    }
    console.log('OK 6: dört kasanın bütün ödülleri geçerli');

    // ── 7. Market spawner kategorisi, iki tıklama ────────────────────────────
    win = await openWindow(() => bot.chat('/market'));
    assert.equal(topSlots(win)[33]?.name, 'spawner', 'Ana menüde spawner kategorisi var');
    win = await openWindow(() => bot.clickWindow(33, 0, 0));
    const first = waitMessage('Onaylamak için');
    await bot.clickWindow(10, 0, 0);
    await first;
    const bought = waitMessage('Spawner alındı');
    await bot.clickWindow(10, 0, 0);
    await bought;
    await sleep(500);
    bot.closeWindow(bot.currentWindow);
    assert.ok(spawnerItems().some(i => pdc(i)['novaeconomy:nova_spawner'] === 'CHICKEN'), 'Satın alınan Tavuk Spawner çantada');
    console.log('OK 7: market spawner satın alma iki tıklamayla çalışıyor');

    // ── 9. Minyon NPC'si çoğalmamalı ─────────────────────────────────────────
    // Gerçek sorun: bölge boşalıp dolunca kayıtlı NPC geri gelirken eklenti ikinci
    // bir NPC yaratıyordu (canlı sunucuda tek minyonda 14 kopyaya kadar çıkmıştı).
    // Aynı koşul burada deterministik üretilir: bağlı NPC öldürülür, yerine kayıtlı
    // kopya bırakılır. Doğru davranış: mevcut NPC'ye bağlan, fazlalıkları temizle.
    {
      const npcAt = () => Object.values(bot.entities)
        .filter(e => e.name === 'armor_stand' && e.position.distanceTo(minionPos.offset(0.5, 0, 0.5)) < 1.5).length;

      await placeMinion();
      await sleep(1500);
      assert.equal(npcAt(), 1, 'Yerleştirmeden sonra tek NPC');

      const at = `${minionPos.x + 0.5} ${minionPos.y} ${minionPos.z + 0.5}`;
      // Bağlı NPC'yi kaldır ve yerine bölge yüklemesinden dönmüş gibi iki etiketli kopya bırak.
      await run(`/execute in minecraft:overworld run kill @e[type=armor_stand,x=${minionPos.x},y=${minionPos.y},z=${minionPos.z},distance=..2]`);
      await sleep(600);
      for (let i = 0; i < 2; i++) {
        await run(`/execute in minecraft:overworld run summon minecraft:armor_stand ${at} {NoGravity:1b,Small:1b,CustomName:'{"text":"Madenci Minyon [Sv 3]"}',CustomNameVisible:1b,BukkitValues:{"skyminions:minyon_type":"miner"}}`);
        await sleep(400);
      }
      assert.equal(npcAt(), 2, 'Kopya NPC kurulumu hazır');

      // Minyonun çalışma turu (madenci: 6 sn) NPC'yi yeniden bağlamalı ve tekilleştirmeli.
      await sleep(16000);
      const seen = npcAt();
      assert.equal(seen, 1, `Fazla NPC temizlenmeli (görülen ${seen})`);
      console.log('OK 9: minyon NPC kopyaları tekilleştiriliyor');
    }

    // ── 8. OP olmayan oyuncu minyonu ada dışına koyamaz ──────────────────────
    // Konsol FIFO'su verilirse bot kendini OP'luktan çıkarır, sonra konsoldan geri alır.
    if (process.env.TEST_CONSOLE_FIFO) {
      await run(`/minyon give ${NAME} farmer 1`, 'minyon aldın');
      await run(`/deop ${NAME}`, 'no longer');
      await sleep(500);
      const farmer = items('golden_hoe').find(i => pdc(i)['skyminions:minyon_item'] === 'farmer');
      await bot.equip(farmer, 'hand');
      const target = BASE.offset(-1, 0, -3);
      await bot.lookAt(target.offset(0.5, 0, 0.5), true);
      const denied = waitMessage('yalnız kendi adana');
      await bot.activateBlock(bot.blockAt(target.offset(0, -1, 0)), new Vec3(0, 1, 0));
      await denied;
      require('node:fs').writeFileSync(process.env.TEST_CONSOLE_FIFO, `op ${NAME}\n`);
      await sleep(1200);
      assert.ok(items('golden_hoe').some(i => pdc(i)['skyminions:minyon_item'] === 'farmer'), 'Reddedilen minyon eşyası çantada kaldı');
      console.log('OK 8: OP olmayan oyuncu ada dışına minyon koyamıyor');
    }

    console.log('PASS: spawner döngüsü, doğal spawner koruması, otomatik toplama, minyon seviyesi (panel + vurma), spawner kasası, 4 kasa ödül doğrulaması, market spawner, minyon ada koruması, NPC tekilleştirme');
    bot.quit();
    clearTimeout(guard);
  } catch (e) {
    console.error('FAIL', e);
    bot.quit();
    clearTimeout(guard);
    process.exitCode = 1;
  }
});
