# PaceLab – futópad programozó POC

Egyszerű Android alkalmazás Bluetooth FTMS futópadhoz. A futópadot a konzolon kell elindítani. Az alkalmazás kizárólag a célsebességet állítja a kiválasztott program alapján.

Az alkalmazás fekete-sárga PaceLab felületet és saját futópad ikont használ. [Főképernyő kedvencekkel](docs/pacelab-preview.png), [programkönyvtár](docs/program-library-preview.png), [Bluetooth-beállítások](docs/bluetooth-settings-preview.png).

## Használat

1. Telepítsd a [POC APK-t](dist/treadmill-programmer-poc.apk) Android 16 telefonra, vagy nyisd meg a projektet Android Studióban és futtasd a telefonon.
2. Írj UTF-8 CSV fájlt a számítógépen az [examples/intervals.csv](examples/intervals.csv) mintájára. A fejléc elhagyható. Az idő percben, a sebesség km/h-ban értendő; tizedeshez pontot használj.
3. Másold a fájlt a telefonra. Nyisd meg a **Programkönyvtárat**, válaszd a **CSV program importálása** gombot, adj neki nevet és mentsd el. A választó a `Download/treadmill` mappát próbálja először megnyitni. Ha a fájlválasztó nem mutatja a CSV-t, a telefon Fájlkezelőjében válaszd ki a fájlt, majd **Megosztás → PaceLab**. A mentett programok és a kilenc beépített preset itt választhatók ki; mindegyiknél látszik a diagram, a hossz, a legnagyobb sebesség és a becsült távolság. A ☆ gombbal jelölt kedvencek a főképernyőn is megjelennek. A választást és a kedvenceket az app megjegyzi. Az importált CSV tartalmát is megőrzi.
4. A jobb felső **fogaskerékkel** vagy a kapcsolatkártya **Bluetooth beállítások** gombjával nyisd meg a külön eszközmenüt. Ott a **Bluetooth eszközök keresése** gombbal válaszd ki a futópadot. Az app a hirdetett nevet, az FTMS-jelzést (ha a hirdetésben szerepel), a Bluetooth-címet és a jelerősséget mutatja. A sikeres csatlakozást megjegyzi, és az app későbbi megnyitásakor automatikusan próbál csatlakozni. Ha a készülék Bluetooth-címe megváltozik, válaszd ki újra kézzel. Más, futópadhoz kapcsolódó alkalmazást zárj be.
5. A futópadot indítsd el a saját konzolján. Ezután nyomd meg az **Edzés indítása** gombot.

### Élő sebességvezérlés

A főképernyő **Élő vezérlés** gombjával program nélkül is állíthatod a tempót. Három sebességkedvenc van; hosszan nyomva módosíthatók, röviden nyomva az aktív edzésben állítják be az adott tempót. Az első kedvenc sebességgel indul a mérés. A testsúly megadható a kalóriabecsléshez. Az **Edzés befejezése és statisztika** gomb lezárja a mérést, majd mutatja az időt, a becsült távot és kalóriát, az átlag- és csúcssebességet, az átlagtempót és a sebességváltások számát. A futópadot előbb indítsd el a konzolon; az app befejezés gombja a szalagot nem állítja le.

## Beépített edzések

| Típus | Könnyű | Közepes | Nehéz |
| --- | --- | --- | --- |
| Kalória | 30 perc, max. 5 km/h | 35 perc, max. 7 km/h | 40 perc, max. 9 km/h |
| Intervallum | 22 perc, max. 6,5 km/h | 26 perc, max. 8 km/h | 30 perc, max. 10,5 km/h |
| Állóképesség | 25 perc, max. 5 km/h | 40 perc, max. 7 km/h | 50 perc, max. 8 km/h |

Mind a kilenc program 5 perc könnyű bemelegítéssel és 5 perc levezetéssel kezdődik, illetve végződik. Az intervallumprogramok gyorsabb és lassabb szakaszokat váltogatnak; az állóképességi programok tartósabb, egyenletes tempót használnak. Ez a felépítés az [NHS Couch to 5K](https://www.nhs.uk/better-health/get-active/get-running-with-couch-to-5k/couch-to-5k-running-plan/) kezdő futóprogram mintáját követi. A „kalória” kategória hosszabb, változó tempójú mozgást jelent, nem meghatározott kalóriaégetési ígéretet. A nehézségi szint egyéni: a [CDC beszédtesztje](https://www.cdc.gov/physical-activity-basics/measuring/index.html) segíthet megítélni a terhelést. Az app indításkor ellenőrzi, hogy a futópad által jelzett sebességtartomány és lépésköz minden szakaszt támogat-e.

Az alkalmazás a szakasz elején elküldi a célsebességet, és az FTMS válasz után indítja a szakasz időzítőjét. A parancs elutasítása vagy a kapcsolat megszakadása leállítja az automatikus programot. A program vége, illetve az alkalmazás **Leállítás** gombja **nem állítja le a futópad szalagját**: azt a konzolon vagy a biztonsági kulccsal kell megtenni.

A becsült távolságot az egyes szakaszok célsebessége és időtartama alapján számolja az app. A futópad ténylegesen megtett távolsága a sebességváltások és a kézi beavatkozás miatt eltérhet.

Az élő edzés kalóriája becslés, a megadott testsúly és a sebességhez rendelt MET aktivitásérték alapján. A sebességsávok a [2024 Adult Compendium of Physical Activities](https://pacompendium.com/wp-content/uploads/2024/02/1_2024-adult-compendium_1_2024.pdf?force_download=true) sík futópadon végzett gyaloglási adatait követik; futótempónál a Compendium futásértékeihez közelítő sávokat használ. Az FTMS adatfolyam itt nem ad tényleges távolságot vagy pulzust, ezért az app nem ezeket méri.

## CSV szabályok

- Opcionális fejléc: `timeInterval(min),speed(kmph)`
- Egy sor: pozitív idő percben, majd 0-nál nagyobb, legfeljebb 25 km/h sebesség.
- Legfeljebb 8 órás program és 256 KB-os fájl importálható.
- A futópad által jelzett sebességtartományt és lépésközt az alkalmazás indítás előtt ellenőrzi.

## Forrásból építés

Android Studio projekt: Java 17, Android Gradle Plugin 8.13.2, `compileSdk 36`, `targetSdk 36`, `minSdk 36`. Telepítsd az Android 16 (API 36) SDK-platformot, majd futtasd: `./gradlew :app:assembleDebug`.

Telefonra telepítés Android Studióból: nyisd meg a projekt gyökérkönyvtárát, várd meg a Gradle-szinkront, a telefonon engedélyezd a fejlesztői lehetőségeket és az USB-hibakeresést, USB-n csatlakoztasd, fogadd el a telefonon a hibakeresési engedélykérést, válaszd ki az eszközt az Android Studio felső sávjában, majd kattints a **Run app** gombra. A POC APK és az Android Studio debug build ugyanazt a projektben lévő teszt aláírókulcsot használja, így a verzió frissíthető az adatok törlése nélkül.

A mellékelt APK az Android Gradle Plugin debug buildjéből készült, Android 16 (API 36) célverzióval. A `gradle/poc-debug.keystore` kizárólag ehhez a POC-hoz való tesztkulcs, éles kiadáshoz nem használható.

## Korlátok

Nincs beépített programszerkesztés, exportálás vagy edzésnapló. Az FTMS kompatibilitás listázása önmagában nem igazolja, hogy az adott gép elfogadja a sebességállító parancsot. Valódi futópadon történő próbát igényel. A Bluetooth kapcsolat megszakadásakor a telefon már nem tudja biztonsággal leállítani a szalagot.
