# Nova Skyblock — oynanabilir yerel sunucu

Minecraft Java **1.20.4** ile **localhost:25565** adresine bağlan.
`Nova-Baslat.command` ağı başlatır; `Nova-Durdur.command` dünyaları kaydederek kapatır.
Aynı anda eski `start-cosmos.sh`, başka bir Paper/Velocity veya ikinci sunucu açma.
Sunucuya ayrılan üst bellek: Paper 3 GB, proxy 512 MB.

## İlk oyun

1. AuthMe'nin gösterdiği `/register` veya `/login` komutuyla giriş yap.
2. `/is` ile ada oluştur. Menü gerekirse `/is create AdaAdi normal` kullan.
3. `/uzay baslangic` ile bir kez 25 Yıldız Tozu al; `/uzay` menüsünden Ay Kazması üret.
4. Adanda üretim yap, günlük görevlerle toz kazan, silah ve yiyecek hazırla.
5. `/zindan` menüsünden Ay Harabeleri'ne gir. Komutla: `/zindan gir 1`.
6. Avdan kazandığın tozu `/uzay` menüsünde ekipmana dönüştür; anahtarları spawn kasalarında kullan.
7. `/zindan cik` geldiğin güvenli konuma döndürür. Işınlanma üç saniye sürer; hareket veya hasar iptal eder.

`/uzay` menüsündeki kılıç ve kasa düğmeleri ilgili sistemlere ulaşır.
Zindan için adan ve hayatta kalma modu gerekir. Ölümde zindan envanterin ve seviyen korunur;
normal Skyblock dünyalarının ölüm kuralları değişmez. Zindanda PvP kapalıdır.

## Beş bölgeli zindan dünyası

Yeni dünya **nova_dungeon**; eski `dungeon` haritası korunur. `/zindan gir` güvenli merkeze götürür.
Düz beyaz yolu ve köprüleri takip ederek beş alanda ilerle. `/zindan gir 1` gibi komutlarla
kilidini açtığın bölgeye doğrudan gidebilirsin. `/zindan merkez` merkeze, `/zindan cik` geldiğin yere döndürür.

| Bölge | Açılış | Yaratık | Can | Temel hasar | Toz |
|---|---:|---|---:|---:|---:|
| 1 · Ay Harabeleri | 0 av | Zombi | 24 | 3 | 1 |
| 2 · Meteor Geçidi | 15 av | Çöl zombisi | 42 | 4 | 2 |
| 3 · Boşluk Tapınağı | 40 av | İskelet okçu | 64 | 6 | 3 |
| 4 · Buzul Yörüngesi | 75 av | Buz iskeleti | 96 | 8 | 4 |
| 5 · Yıldız Çekirdeği | 120 av | Wither iskeleti | 140 | 11 | 5 |

Can/hasar değerleri Minecraft puanlarıdır; 2 can puanı bir kalptir. Zırh alınan hasarı azaltır.
Her alan 49×49 bloktur; renkli geçitler, aydınlatma, sütunlar, siperler ve seviye yazıları bulunur.
Merkez ve köprüler canavar doğurmayan geçiş alanlarıdır. Kilitli bölgelere yürüyerek veya
ışınlanarak geçiş engellenir; yönetici geçiş yetkisi bunu aşabilir. Boşluğa düşen oyuncu merkeze alınır.

Her 12 avlık eşiğin ardından müsait alan varsa muhafız doğar: bölge yaratığının üç kat canı,
altı kat toz ödülü ve garantili anahtarı vardır. Normal avlarda anahtar olasılığı %4'tür.
Bölge 1 Yörünge, bölge 2 Yıldız Hasadı, bölge 3–5 Süpernova anahtarı verir.
Dolu çantada anahtar sahibine atanarak ayağının altına düşer; oradan al.
Muhafızın işaretlediği alandan uzaklaş: 1,25 saniye sonra darbe gelir.

Av sayısı korunur; önceki zindandaki ilerleme yeni dünyada da geçerlidir. Toz mevcut NovaCosmos
cüzdanına eklenir. Günlük savaş görevleri ve ekipman yetenekleri yeni dünyada da çalışır.
Yakında 5, toplamda 48 canavar sınırı vardır; üç dakika sonra ya da kendi bölgesinden çıkınca temizlenir.

Harita ilk açılışta parçalara bölünerek oluşturulur. Hazır olana kadar giriş açılmaz.
Sonraki açılışlarda harita yeniden çizilmez; yönetici düzenlemeleri korunur.
`plugins/NovaDungeon/map-v1.ready` haritanın tamamlandığını belirtir. Harita dosyası ile bu
kayıt birlikte yedeklenmelidir. Aynı isimli, eklentiye ait olmayan dünyaya yazılmaz.

## Sohbet matematik yarışması

Sunucu açıkken çevrimiçi oyuncular varsa **5 dakikada bir** toplama, çıkarma, çarpma
veya tam bölme sorusu sorulur. İlk soru açılıştan yaklaşık bir dakika sonra gelir.
Cevap süresi **30 saniyedir**; cevabı normal sohbete yalnız sayı olarak yaz.
İlk doğru cevap **1 vote anahtarı + 5 Yıldız Tozu** kazanır. Yanlış veya sonraki cevaplar ödül vermez.

Çanta doluysa toz cüzdana eklenir, anahtar bekler. Yer açıp `/matematik teslim` yaz;
bekleyen ödül yeniden girişte de denenir. Aynı ödülün tozu tekrar verilmez.
Süre dolduğunda doğru cevap gösterilir; ödül verilmez.
`/matematik` aktif soruyu gösterir. Yetkili: `/matematik baslat`.
Aralık ve süre `plugins/NovaDungeon/config.yml` içindeki `math-quiz` bölümündedir.

Üst bölge doğum kontrolleri: normal oyuncular için av sınırları korunur. `novadungeon.admin`
yetkilileri Survival modunda her bölgeye girip o bölgenin yaratıklarıyla karşılaşabilir.
Creative/Spectator oyuncuları canavar doğurtmaz. `/dungeonadmin durum`, mevcut bölgeyi,
oyun modunu, av sayısını ve otomatik doğuma uygunluğu gösterir.

## Kristal kasalar

Mevcut ana bloklar korunur; üzerinde dönen kristal, yıldız, parçacık halkası ve yazı görünür.
Bu tasarım için ek kaynak paketi gerekmez. Var olan Cosmos silah modelleri kendi paketini kullanır.

| Kimlik | Tasarım | Ana blok — world |
|---|---|---|
| vote | Turkuaz / elmas kristali | 256, 138, 1408 |
| tarım | Yeşil / zümrüt kristali | 256, 138, 1414 |
| novacraft | Mor / ametist kristali | 256, 139, 1411 |
| spawner | Turuncu / spawner kafesi | **henüz konmadı** — bir bloğa bakıp `/crate set spawner` |

**Ana bloğa sağ tık:** anahtar tüketerek aç. **Sol tık:** ödül/olasılık önizlemesi.
`/crate preview vote` gibi komutlarla uzaktan önizlenebilir.

Her kasanın ödül tablosu, anahtarın ne kadar nadir olduğuna göre bir **beklenen değere** (EV)
ayarlandı. Anahtar ne kadar zor çıkıyorsa kasa o kadar değerlidir:

| Kasa | Anahtar nereden | Beklenen değer | Öne çıkan ödüller |
|---|---|---:|---|
| Yörünge | Oy, matematik yarışması, Zindan 1, günlük 3. gün | ~1.600 | para, demir/altın/elmas, 10'luk satış değneği, Çiftçi Minyon, üst kasa anahtarı |
| Yıldız Hasadı | Günlük 5. gün, Zindan 2, çiftçi görevleri | ~3.100 | altın havuç/elma, Çiftçi/Balıkçı Minyon, 25'lik değnek, Seviye 3 Çiftçi Minyon |
| Süpernova | Günlük 7. gün, Zindan 3–5, son görevler | ~12.700 | 10–50 bin para, netherite, Madenci/Savaşçı Minyon, **Spawner anahtarı**, sınırsız değnek, Seviye 5 Madenci |
| Spawner | Her rütbe atlayışı, görev zincirlerinin sonu, Süpernova (%5) | ~53.000 | Zombi/İskelet/Örümcek/Creeper/İnek (%83), Blaze (%7), Enderman (%5), **Demir Golem (%2)** |

Kasalar dört tür ödül verir: eşya, para, Yıldız Tozu ve **komut** ödülü. Komut ödülü minyon,
spawner, satış değneği veya başka kasanın anahtarı gibi özel eşyaları teslim eder
(`COMMAND:<ikon>:<etiket>:<konsol komutu>:<ağırlık>`). Her kasada ağırlık toplamı 100'dür,
yani ağırlık doğrudan yüzde olasılıktır. Tablolar `skyblockserver/plugins/NovaCrates/config.yml`
içindedir; önizleme gerçek olasılıkları gösterir. Kasa başına `renk` ve hologramdaki
`anahtar-kaynagi` metni de oradan değişir.

Ödül açılışta teslim edilir, animasyon yalnız sunumdur. Menüyü kapatmak/bağlantıyı kesmek
ikinci ödül vermez ve animasyonun bitmesini bekleyen bir teslimat bırakmaz.
Dolu çantada anahtar tüketilmez. Para/toz sağlayıcısı hata döndürürse anahtar geri korunur.
Oyuncu başına aynı anda bir açılış vardır; çift el olayı yalnız bir kez işlenir.
Sahte isimli anahtarlar, önizlemeden eşya alma ve yetkisiz anahtar verme engellenir.

## Yönetim

Yalnız OP veya `novacrates.admin` / `novadungeon.admin` yetkilileri kullanabilir:

```text
/crate set vote                    # Baktığın bloğu Yörünge kasası yap
/crate give Oyuncu vote 1          # 1–64 anahtar ver; konsoldan da kullanılabilir
/crate remove vote                # Tasarımı/konumu kaldır; ana bloğa dokunma
/dungeonadmin durum               # Beş bölgenin güvenli girişlerini göster
/dungeonadmin setspawn 1          # İlgili bölgede durarak özel giriş kaydet
```

Kasalar kırılma, patlama ve piston taşınmasına karşı korunur; önce `/crate remove` kullan.
Ayar değişikliklerinden sonra normal kapat/aç yap. `/reload` kullanma.

```sh
python3 tools/serverctl.py start
python3 tools/serverctl.py status
python3 tools/serverctl.py console skyblock dungeonadmin durum
python3 tools/serverctl.py stop
```

Yerel konsol kanalı yalnız bu kullanıcı hesabının erişebildiği bir Unix socket'tir.
Ağ yöneticisi bir servis kapanırsa diğerini de kapatır. Kapanış sırasında tekrar başlatma;
`status` çıktısında süreç ve iki port kapalı olmalıdır. Günlükler `run/skyblock.log`,
`run/proxy.log`, `run/manager.log` ve sunucuların kendi `logs/` dizinlerindedir.

## Kurulum değişiklikleri

- NovaCosmos, NovaCrates ve NovaDungeon yeni JAR'ları ana sunucuya kuruldu.
- Varsayılan Velocity girişi doğrudan Skyblock'a gider; mevcut AuthMe kayıt/giriş koruması devam eder.
- Eski ayrı lobi ve örnek Flask giriş servisi bu oyun yolu için gerekmez; kaynakları silinmedi.
- Paper modern Velocity forwarding açıldı, eski BungeeCord forwarding kapatıldı.
  Mevcut ortak gizli anahtar korundu; backend 127.0.0.1 üzerinde kalır.
- SkBee'nin Skript ile çakışan display-entity modülü kapatıldı; mevcut scriptler bu modülü kullanmıyor.
- Dosyası olmayan TEST dünyasının otomatik yüklemesi kapatıldı.
- Kaynak ve önceki JAR/ayar yedekleri `backups/` altında tutulur.

Forwarding ayarları [PaperMC'nin resmi yapılandırması](https://docs.papermc.io/velocity/player-information-forwarding/)
ile eşleştirildi. Paper Java 17, kurulu Velocity derlemesi Java 25 ile çalıştırılır.

## Kontroller ve sınırlar

`python3 tools/build_playable.py` yereldeki mevcut API JAR'larıyla üç eklentiyi derler,
Cosmos kayıt/ödül testlerini çalıştırır. Harici indirme gerekmez.
`tools/qa/` içindeki probe yalnız `NovaCosmos/target/test-server` içindir;
**NovaProbe.jar ana sunucuya kurulmaz.** Test portu 127.0.0.1:25585'tir.

Gerçek Minecraft protokolüyle kasa menüsü, Shift-tıklama koruması, anahtar tüketimi,
sahte anahtar, dolu çanta, yetki reddi, çift el, koruma iptali, bölge kilitleri,
gerçek canavar ölümü, muhafız anahtarı, kalıcı toz, giriş/çıkış ve tasarım varlıkları sınandı.
Son kontrol çıktısı `tools/qa/latest-test.log` içinde tutulur.
Grafik Minecraft istemcisiyle görsel kabul testi yapılmadı; görüntülerin varlığı ve dönüşümleri sunucu üzerinde doğrulandı.

Bu kurulum **yerelde oynamak** için hazırlanmıştır. Dış oyuncular için kaynak paketinin
`http://127.0.0.1:8088/novacosmos.zip` adresi erişilebilir bir sunucu adresiyle değiştirilmeli;
alan adı/ağ erişimi ayrıca ayarlanmalıdır. Mevcut oyun sürümü 1.20.4'tür.
EssentialsX 2.21.2 bu Paper sürümü için resmi destek uyarısı verir; testler belirli oyun akışlarını kapsar,
sunucudaki bütün eski eklentilerin her özelliğini kapsamaz. AuthMe GeoIP veritabanı eksikliği de uyarı verir;
parolalı kayıt/giriş denetimi çalışır.

Fiziksel anahtar envanteri, Vault ve Cosmos farklı kayıt sistemleridir. Normal kapanma/çıkış akışı
korunur; işletim sisteminin tam işlem ortasında çökmesi için bu sistemler arasında ortak atomik
veritabanı işlemi garantisi yoktur. Düzenli kapatma ve yedekleme kullan.

## Ekonomi ve market

`/market` (`/magaza`, `/shop`) altı kategorili alım-satım menüsünü açar: madencilik, çiftlik,
orman, avcılık, yiyecek, ekipman. Sol tık 1 adet alır, Shift+Sol tık bir yığın alır,
sağ tık 1 adet satar, Shift+Sağ tık çantandaki hepsini satar. Menüdeki eşyalar yalnız
gösterimdir; menüden eşya alınamaz, menüye eşya konamaz.

`/sat [miktar|hepsi]` elindekini, `/satall` çantandaki bütün satılabilir eşyaları satar.
Yalnız **sade** eşyalar satılır: isim, lore, büyü, model verisi veya PDC etiketi taşıyan
eşyalar (kasa anahtarları, Cosmos ekipmanı, müzayede etiketli eşyalar) markete girmez.
Bunlar için `/ah sell <fiyat>` kullanılır.

Fiyatlar `plugins/NovaEconomy/config.yml` içindedir. Eklenti açılışta **satış ≥ alış** olan
her kaydı kapatır ve konsola yazar; sonsuz para döngüsü bu yüzden oluşamaz. Fiyat değişikliğinden
sonra `/market reload` yeterlidir.

`/otosat` baktığın sandığı otomatik satışa alır: 30 saniyede bir içindekiler satılıp paran
hesabına geçer. Oyuncu başına 3 sandık sınırı vardır; sandık kırılınca kayıt silinir.
Yetkili `/market degnek <oyuncu> [kullanım]` ile **Satış Değneği** verir; değnek sandığa
sağ tıklayınca içeriği satar. Satılacak bir şey yoksa kullanım harcanmaz.

Kırdığın blokların ürünü ve tecrübesi **doğrudan çantana** gelir; çanta doluysa yere düşer.
Sandık, fırın gibi içerikli bloklara dokunulmaz. `/toplama` bunu kendin için açıp kapatır.

Yeni oyuncu 1.000 para ve tek seferlik `baslangic` kiti ile başlar; `/kit gunluk` günde bir kez alınır.

## Ada görevleri, yükseltmeler ve liderlik

`/is missions` beş kategoride 19 Türkçe görev sunar: Madenci, Avcı, Çiftçi, Balıkçı, Kaşif.
Görevler zincirlidir — biri bitmeden sonraki açılmaz. Ödüller para, jeneratör yükseltmesi ve
kasa anahtarlarıdır. `/is upgrade` fiyatları yeni ekonomiye göre indirildi: ada sınırı 15.000'den,
jeneratör 20.000'den başlar; huni ve vagon limitleri geç oyun hedefidir. Ada bankası günlük
**%5** faiz verir.

`/lider ayarla <ada|seviye|para>` bulunduğun yere hologram liderlik tablosu kurar
(`/lider sil`, `/lider yenile`). Tablolar 30 saniyede bir yenilenir, dünyaya kaydedilmez,
sunucu açılışında yeniden kurulur. Yetki: `novasidebar.admin`.

Blok yığınlama genişletildi: cevher blokları, saman, kemik, slime ve bal blokları
tek yığında toplanabilir (`/is toggle blocks`). Bu hem ada değeri yoğunluğunu artırır hem de
blok sayısını düşürür. **Spawnerlar yığınlanmaz**: SSB2 yığını spawner türünü taşımadığı için
yığına giren spawner türünü kaybederdi. **Canavar yığınlama yoktur**; onun için ayrı bir eklenti gerekir.

### Ada değeri nereden gelir

Sonsuz üretilebilen blokların ada değeri **sıfırdır**: cobblestone, taş, netherrack, obsidyen,
kaktüs, şeker kamışı, buz ve kar. Jeneratörü kazmak artık ada seviyesini düşürmez ve seviye
eksiye inemez. Değer, **yerleştirdiğin** bloklardan gelir.

Değerli blokların ada değeri, içindeki malzemenin **market satış fiyatına eşittir** — yani
ada değeri, adadaki blokların para karşılığıdır. Hiçbir blok ötekinden "verimli" değildir:

| Blok | Değer | | Blok | Değer |
|---|---:|---|---|---:|
| Demir bloğu | 108 | | Netherite bloğu | 22.500 |
| Altın bloğu | 180 | | Beacon | 5.000 |
| Elmas bloğu | 540 | | Spawner (her tür) | 5.000 |
| Zümrüt bloğu | 450 | | Kömür / Lapis / Kızıltaş bloğu | 36 / 36 / 27 |

Ada seviyesi = değer ÷ 100 (tam sayı). Spawner değeri en ucuz spawnerın (12.000) altında
tutuldu; ucuz spawner alıp ada değeri şişirmek kazançlı değildir.

## Minyonlar

`/minyonmarket` dört minyon satar:

| Minyon | Fiyat | Limit | Ne yapar |
|---|---:|---:|---|
| Çiftçi | 5.000 | 2 | Olgun ekinleri hasat eder |
| Madenci | 7.500 | 2 | Ayardaki taş/cevher bloklarını kazar |
| Balıkçı | 9.000 | 1 | Yanında su varsa balık tutar |
| Savaşçı | 15.000 | 1 | Yakındaki canavarları öldürür, ganimeti depolar |

Minyon eşyası **PDC etiketiyle** tanınır; sahte isimli eşya minyon kurmaz. Bir bloğa sağ
tıklayarak yerleştirilir, üstündeki yer boş olmalıdır. Minyon **yalnız üyesi olduğun adaya**
konur; spawn'a veya başkasının adasına konamaz (madenci minyon orada blok kazabilirdi).

Her minyonun tek bir NPC'si olur: bölge boşalıp yeniden yüklendiğinde eklenti var olan
NPC'ye yeniden bağlanır, fazlalık ve sahipsiz kalan NPC'leri siler (eski sürümde her
giriş-çıkışta bir kopya birikiyordu).

**Seviye eşyada saklanır:** minyonu panelden toplayınca da vurarak kaldırınca da eşya seviyesini
taşır ("Seviye: 5" satırı) ve tekrar koyunca aynı seviyeden devam eder. Seviye 1 eşyalar üst üste
yığınlanır, farklı seviyedekiler ayrı durur. Minyona sağ tıklayınca depo açılır:
sol tık eşyayı çantana alır, sağ tık satar, alt sıradaki şişe minyonu yükseltir.
Depodaki fiyatlar market tablosundan okunur, ayrı bir fiyat listesi yoktur.
Minyonu yalnız sahibi vurarak kaldırır; eşyası yere düşer.

`/minyon` kendi minyon panelini açar: her minyonun türü, seviyesi, deposu ve konumu görünür.
Sol tık minyonun yanına ışınlar, sağ tık deposunu açar, **Shift + sağ tık** minyonu kaldırır —
minyon eşyası ve depodaki her şey çantana geri gelir. Minyonu vurarak kaldırınca da depo
içeriği artık kaybolmaz, minyonla birlikte yere düşer.

Yetkili `/minyon give <oyuncu> <farmer|miner|killer|fisher> [adet] [seviye]` ile minyon verebilir.
Ayarlar: `plugins/SkyMinions/config.yml` (aralık, yarıçap, hasar, yükseltme fiyatı, limitler).

## Spawnerlar

`/market` ana menüsündeki **Spawnerlar** kategorisi on tür satar. Pahalı eşya olduğu için satın
alma iki tıklamalıdır: ilk tık fiyatı gösterir, 5 saniye içinde ikinci tık satın alır.

| Spawner | Fiyat | | Spawner | Fiyat |
|---|---:|---|---|---:|
| Tavuk | 12.000 | | İskelet | 40.000 |
| Domuz | 15.000 | | Creeper | 60.000 |
| İnek | 20.000 | | Enderman | 120.000 |
| Zombi | 25.000 | | Blaze | 150.000 |
| Örümcek | 35.000 | | Demir Golem | 350.000 |

Tek spawner, Savaşçı Minyon ganimeti topladığında yaklaşık 20–40 saatlik oyunla kendini öder;
`/is upgrade` spawner hızı ve canavar ganimeti yükseltmeleri bu süreyi kısaltır.
Savaşçı Minyon yalnız düşman canavarlara vurur: inek, domuz, tavuk ve demir golem elle kesilir.

- Spawner türü eşyada saklanır; koyduğun an o türü üretir.
- **Kırınca türüyle birlikte geri alırsın** (her kazma olur); yerleştir-kır tecrübe vermez.
- Patlamada kaybolmaz; yumurtayla türü değiştirilemez.
- Doğal zindan spawnerları eşya vermez — bedava spawner çıkarılamaz.
- Spawnerlar markete geri satılamaz; oyuncular arası `/ah` ile satılabilir.

Yetkili: `/market spawner <oyuncu> <TÜR> [adet]` (kasalar da bu komutu kullanır).
Fiyatlar `plugins/NovaEconomy/config.yml` → `spawnerlar` bölümündedir.

## Günlük ödül, rütbe ve oynama ödülü

`/gunluk` günlük ödülü verir; seri 7 güne kadar büyür: 1.000 → 2.000 → 3.000 + Yörünge anahtarı →
4.000 → 5.000 + Yıldız Hasadı anahtarı → 7.500 → 12.500 + Süpernova anahtarı (haftada ~35.000).
Bir gün kaçırılırsa seri başa döner. Gün sınırı Europe/Istanbul saatine göredir.
Ödülde anahtar olabileceği için çantada 1 boş yer gerekir; yoksa gün harcanmaz.

`/ranklar` rütbe merdivenini, `/rankup` bir üst rütbeyi satın alır:
Taş 25.000 → Demir 100.000 → Altın 350.000 → Elmas 1.000.000 → Zümrüt 3.000.000 → NOVA 10.000.000.
Her rütbe bir öncekinin ayrıcalıklarını devralır:
Taş `/back` `/workbench` • Demir `/enderchest` `/hat` • Altın `/feed` `/repair` `/near` •
Elmas `/nick` + ada uçuşu • Zümrüt `/heal` `/ptime` • NOVA `/fly` `/speed`.
Ev hakkı sırayla 4, 5, 6, 8, 10, 15 olur. Rütbe atlarken `kurucu`/`vip` gibi diğer grupların silinmez.
Her rütbe atlayışı **1 Spawner anahtarı** verir (NOVA: 2).

Aktif oynanan her 30 dakikada 500 para verilir. **AFK sayılmaz:** son 5 dakikada kamerasını
çevirmeyen, blok kırıp koymayan, sohbet/komut kullanmayan oyuncunun süresi işlemez; su akıntısı
havuzunda beklemek etkinlik sayılmaz. Ayarlar `plugins/NovaRewards/config.yml`,
ilerleme `plugins/NovaRewards/data.yml` içindedir.

## Ekonomi dengesi

Hedef tempo, günde ~2 saat oynayan ortalama bir oyuncu için:

| Dönem | Birikim | Ne alınır |
|---|---:|---|
| İlk gün (2–3 saat) | 15–40 bin | Ada sınırı ve jeneratör yükseltmesi, Çiftçi Minyon, Taş rütbesi |
| 1–2. hafta (20–30 saat) | 250–500 bin | Demir → Altın, jeneratör 3, ekin hızı, ilk spawnerlar |
| 1. ay (~60 saat) | 1,5–3 milyon | Elmas, spawner hızı, Savaşçı Minyon + blaze/creeper spawner |
| 2–3. ay | 10 milyon+ | Zümrüt → NOVA, huni/vagon limitleri |

**Para girişi:** market satışı (ana gelir: erken oyunda saatte ~3–8 bin, jeneratör 4 + minyonlarla
15–20 bin, spawner hattıyla daha fazla), günlük ödül (~35 bin/hafta), oy (~2 bin/oy), görevler
(tek sefer, toplam ~550 bin), aktif oynama (saatte 1.000).

**Para çıkışı:** market alımı (satışın 4 katı), ada yükseltmeleri (15 bin – 2 milyon, toplam ~8 milyon),
rütbeler (toplam ~14,5 milyon), minyonlar ve yükseltmeleri, spawnerlar (12 – 350 bin), müzayede vergisi %5.

Döngü koruması: markette satış hiçbir eşyada alıştan pahalı olamaz (açılışta denetlenir);
kasa/minyon/spawner eşyaları markete satılamaz; spawner kır-koy tecrübe vermez; AFK para kasamaz.

## Oy sistemi

NuVotifier 2.7.3 kuruldu ve zVoteParty ona bağlandı. Oy ödülleri Türkçeleştirildi:
%40 750 para, %30 1.500 para, %20 1 Yörünge anahtarı + 500 para, %10 2 anahtar + 2.500 para.
50 oyda vote party: çevrimiçi herkese 1 anahtar + 2.000 para.

Votifier şu an `127.0.0.1:8192` dinler ve jetonunu ilk açılışta üretti
(`plugins/Votifier/config.yml`). Sunucu dışarı açılırken bu adres dışarıdan erişilebilir
olmalı ve jeton oy sitelerine girilmelidir; aksi halde oy paketi sunucuya ulaşmaz.
