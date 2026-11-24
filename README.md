# Kesem

[![Android CI](https://github.com/dogukanTufan00/kesem/actions/workflows/android-ci.yml/badge.svg)](https://github.com/dogukanTufan00/kesem/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Kesem**, Android için Kotlin, Jetpack Compose (Material 3) ve Room ile geliştirilmiş **çevrimdışı öncelikli (offline-first)** bir kişisel finans, bütçe, borç ve yatırım portföyü takip uygulamasıdır. Tüm finansal veriler cihazda yerel olarak saklanır; yalnızca yatırım portföyündeki varlıkların güncel fiyatlarını yenilemek için kullanıcı isteğiyle halka açık piyasa kaynaklarına bağlanır.

---

## Öne Çıkan Özellikler

### Hesaplar ve İşlemler
- **Çoklu Hesap Yönetimi:** Banka, Nakit, Kredi Kartı ve Yatırım hesaplarını ayrı ayrı veya toplam varlığa dahil ederek takip etme, arşivleme ve bakiye düzeltme.
- **Kredi Kartı & Taksit Desteği:** Kart limiti, hesap kesim ve son ödeme günü takibi; taksitli harcama oluşturma, taksit grubunu tek seferde yönetme ve tek tıkla kredi kartı borcu ödeme transferi.
- **Kapsamlı İşlem Kaydı:** Gelir, gider, hesaplar arası transfer ve bakiye düzeltme işlemleri; etiketleme ve not ekleme.
- **Fiş Fotoğrafı Saklama:** Kameradan veya galeriden seçilen fiş görsellerini sıkıştırarak uygulama içi depolamada saklama (`FileProvider` ile güvenli paylaşım ve tam ekran önizleme).

### Bütçe, Düzenli İşlemler ve Borç Takibi
- **Aylık Bütçeler:** Genel aylık harcama limiti veya kategori bazlı bütçe tanımlama, özelleştirilebilir uyarı eşikleri ile durum takibi (`OK`, `WARNING`, `EXCEEDED`).
- **Düzenli İşlemler:** Haftalık, aylık ve yıllık periyotlarda tekrarlayan gelir/gider kuralları oluşturma, özet ekranından tek dokunuşla onaylama veya atlama.
- **Borç & Alacak Defteri:** Verilen ve alınan borçları vade tarihiyle takip etme, kısmi ödeme kaydetme ve ödemeleri isteğe bağlı olarak ilgili hesap bakiyesine yansıtma.

### Yatırım Portföyü
- **7 Varlık Türü Desteği:** Döviz, Altın/Gümüş, BIST Hisse, ABD Hisse, TEFAS Fon, Kripto ve stopaj oranlı Vadeli Mevduat.
- **Canlı Fiyat Güncelleme & Lot Yönetimi:** İsteğe bağlı yenilemede halka açık kaynaklardan (Truncgil, TCMB, Yahoo Finance, Binance, TEFAS) güncel fiyat çekme; alış/satış lotu ekleme ve manuel fiyat girişi.

### Analiz, Güvenlik ve Yedekleme
- **Kişisel Dönem Başlangıcı:** Maaş gününe göre ayın 1–28 arası herhangi bir gününü dönem başlangıcı olarak belirleyebilme.
- **Analiz Ekranı:** Dönemsel gelir/gider karşılaştırması, kategori dağılım grafikleri ve tasarruf oranı hesaplaması.
- **Güvenlik ve Gizlilik:** `PBKDF2WithHmacSHA256` (rastgele salt ve sabit zamanlı doğrulama) ile 4 haneli PIN kilidi, destekleyen cihazlarda biyometrik kilit açma, arka planda 30 saniye sonra otomatik yeniden kilitleme ve tek dokunuşla bakiye gizleme.
- **Veri Taşınabilirliği:** Tüm veritabanı ve uygulama ayarlarını JSON formatında yedekleme, önizlemeli geri yükleme ve işlem geçmişini CSV olarak dışa aktarma.
- **Tema Desteği:** Açık, Koyu ve Sistem teması.

---

## Teknoloji Yığını ve Mimari

| Katman / Alan | Kullanılan Teknoloji |
|---|---|
| Dil | Kotlin 2.3 |
| Arayüz | Jetpack Compose + Material 3, Navigation Compose |
| Yerel Veritabanı | Room 2.8 (4 şema sürümü, `MIGRATION_1_2`..`3_4`), KSP |
| Tercihler | Jetpack DataStore Preferences |
| Eşzamanlılık | Kotlin Coroutines & `StateFlow` |
| Mimari | MVVM + Manuel Dependency Injection (`AppContainer`) |
| Test | JUnit 4 (11 birim test sınıfı) |

### Proje Yapısı

```text
app/src/main/java/io/github/dogukantufan00/kesem/
├── data/
│   ├── backup/        # JSON yedekleme codec'i, BackupManager ve CSV dışa aktarıcı
│   ├── local/         # Room veritabanı, DAO'lar, Entity'ler ve migration'lar
│   ├── preferences/   # DataStore ayar ve güvenlik tercihleri
│   ├── remote/        # Piyasa verisi servisi ve yanıt ayrıştırıcıları
│   ├── repository/    # WalletRepository (tek doğruluk kaynağı)
│   └── storage/       # Fiş görselleri için sıkıştırma ve FileProvider yönetimi
├── model/             # Enum'lar ve UI modelleri
├── ui/                # Ekranlar, ViewModel'lar, bileşenler, tema ve navigasyon
├── util/              # Bütçe, taksit, yatırım, dönem, analiz hesaplayıcıları ve PinHasher
├── AppContainer.kt
├── KesemApp.kt
└── MainActivity.kt
```

---

## Ekran Görüntüleri

> Ekran görüntüleri yakında eklenecektir.

---

## Kurulum ve Test

### Gereksinimler
- Android Studio (güncel sürüm)
- JDK 17 veya JDK 21
- Android SDK (`compileSdk = 37`, `minSdk = 26`, `targetSdk = 36`)

### Projeyi Çalıştırma

```bash
git clone https://github.com/dogukanTufan00/kesem.git
cd kesem
```

Android Studio ile projeyi açıp doğrudan cihaz veya emülatörde çalıştırabilir, ya da komut satırından birim testlerini ve debug APK derlemesini çalıştırabilirsiniz:

```bash
# Birim testlerini çalıştır
./gradlew testDebugUnitTest

# Debug APK oluştur
./gradlew assembleDebug
```

Windows PowerShell üzerinde `./gradlew` yerine `.\gradlew.bat` kullanabilirsiniz.

---

## Geliştirme Notu

Mimari tasarım, özellik kapsamı, test ve doğrulama süreçleri tarafımdan yürütülmüş; geliştirme aşamasında yapay zekâ destekli kodlama araçlarından faydalanılmıştır.

## Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.
