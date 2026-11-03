# PaceLab – futópad programozó POC

Egyszerű Android alkalmazás Bluetooth FTMS futópadhoz. A futópadot a konzolon kell elindítani. Az alkalmazás kizárólag a célsebességet állítja a kiválasztott program alapján.

Az alkalmazás fekete-sárga PaceLab felületet és saját futópad ikont használ. [Képernyőkép mintaprogrammal](docs/pacelab-preview.png).

## Használat

1. Telepítsd a [POC APK-t](dist/treadmill-programmer-poc.apk) Android 16 telefonra, vagy nyisd meg a projektet Android Studióban és futtasd a telefonon.
2. Írj UTF-8 CSV fájlt a számítógépen az [examples/intervals.csv](examples/intervals.csv) mintájára. A fejléc kötelező. Az idő percben, a sebesség km/h-ban értendő; tizedeshez pontot használj.
3. Másold a fájlt a telefonra. Az alkalmazásban válaszd a **CSV fájl kiválasztása** gombot, adj neki nevet és mentsd el. A mentett programok között bármikor válthatsz; mindegyiknél látszik a diagram, a hossz és a legnagyobb sebesség. Az importált CSV tartalmát is megőrzi az app.
4. A **Bluetooth eszközök keresése** gombbal válaszd ki a futópadot. Az app a hirdetett nevet, az FTMS-jelzést (ha a hirdetésben szerepel), a Bluetooth-címet és a jelerősséget mutatja. A sikeres csatlakozást megjegyzi, és az app későbbi megnyitásakor automatikusan próbál csatlakozni. Ha a készülék Bluetooth-címe megváltozik, válaszd ki újra kézzel. Más, futópadhoz kapcsolódó alkalmazást zárj be.
5. A futópadot indítsd el a saját konzolján. Ezután nyomd meg a **Program indítása** gombot.

Az alkalmazás a szakasz elején elküldi a célsebességet, és az FTMS válasz után indítja a szakasz időzítőjét. A parancs elutasítása vagy a kapcsolat megszakadása leállítja az automatikus programot. A program vége, illetve az alkalmazás **Leállítás** gombja **nem állítja le a futópad szalagját**: azt a konzolon vagy a biztonsági kulccsal kell megtenni.

## CSV szabályok

- Fejléc: `timeInterval(min),speed(kmph)`
- Egy sor: pozitív idő percben, majd 0-nál nagyobb, legfeljebb 25 km/h sebesség.
- Legfeljebb 8 órás program és 256 KB-os fájl importálható.
- A futópad által jelzett sebességtartományt és lépésközt az alkalmazás indítás előtt ellenőrzi.

## Forrásból építés

Android Studio projekt: Java 17, Android Gradle Plugin 8.13.2, `compileSdk 36`, `targetSdk 36`, `minSdk 36`. Telepítsd az Android 16 (API 36) SDK-platformot, majd futtasd: `./gradlew :app:assembleDebug`.

Telefonra telepítés Android Studióból: nyisd meg a projekt gyökérkönyvtárát, várd meg a Gradle-szinkront, a telefonon engedélyezd a fejlesztői lehetőségeket és az USB-hibakeresést, USB-n csatlakoztasd, fogadd el a telefonon a hibakeresési engedélykérést, válaszd ki az eszközt az Android Studio felső sávjában, majd kattints a **Run app** gombra. A POC APK és az Android Studio debug build ugyanazt a projektben lévő teszt aláírókulcsot használja, így a verzió frissíthető az adatok törlése nélkül.

A mellékelt APK az Android Gradle Plugin debug buildjéből készült, Android 16 (API 36) célverzióval. A `gradle/poc-debug.keystore` kizárólag ehhez a POC-hoz való tesztkulcs, éles kiadáshoz nem használható.

## Korlátok

Nincs beépített programszerkesztés, exportálás vagy edzésnapló. Az FTMS kompatibilitás listázása önmagában nem igazolja, hogy az adott gép elfogadja a sebességállító parancsot. Valódi futópadon történő próbát igényel. A Bluetooth kapcsolat megszakadásakor a telefon már nem tudja biztonsággal leállítani a szalagot.
