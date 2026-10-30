# CorenyaTAB

**Yapımcı:** rushflex
**Sürüm:** 1.0.0
**Platform:** Spigot / Paper / Folia **1.8 – 1.21.11** (tek jar)

Tablist, header/footer, scoreboard ve placeholder yönetimini tek eklentide toplar. 1.8.8 API'sine
karşı derlenir; yalnızca Folia/Paper 1.21.11'de de bulunan metotlar kullanıldığı için aynı jar
1.8'den 1.21.11'e kadar çalışır.

## Özellikler

- Animasyonlu tablist (prefix, oyuncu satırları, ping/sıralama)
- Header/footer: tarih, saat, konum, web sitesi, para / ELO / elmas / oy sayıları
- PlaceholderAPI desteği (boş bırakılırsa yerleşik değerler kullanılır)
- Skor tablosu ve periyodik istatistik kaydı
- `/corenyatab reload` ile yeniden yükleme
- Folia uyumlu zamanlayıcı (`Scheduling`)

## Derleme

```powershell
.\gradlew build
```

Çıktı: `build/libs/CorenyaTab-1.0.0.jar` → sunucunun `plugins/` klasörüne at.

## Yapılandırma

`src/main/resources/config.yml` varsayılan ayarları içerir; sunucuda `plugins/CorenyaTab/config.yml`
olarak oluşur. Öneki, güncelleme sıklığını ve görünen alanları buradan değiştirebilirsin.

## Kaynak yapısı

```
src/main/java/com/corenya/tab/
├── CorenyaTab.java    # plugin entrypoint
├── TabService.java    # tablist/scoreboard servisi
├── TabRenderer.java   # satır ve header/footer çizimi
├── TabSender.java     # paket gönderimi (sürüm uyumlu)
├── TabSettings.java   # config okuma
├── TabCommand.java    # /corenyatab komutu
├── StatsStore.java    # istatistik kalıcılığı
├── Scheduling.java    # Folia/Bukkit zamanlayıcı soyutlaması
└── Hooks.java         # PlaceholderAPI / Vault köprüleri
```

## Lisans

Bu depoda ayrı bir lisans dosyası yoktur; tüm hakları saklıdır.
