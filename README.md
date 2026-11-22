# Vallicosmo (Fabric, Minecraft 1.21.11)

Cosmetics-Mod mit eigenem Menü. Öffnen mit **`/vallicosmo`**. Alles ablegen: `/vallicosmo clear`.

## Cosmetics

| Kategorie | Cosmetic | Beschreibung |
|---|---|---|
| Flügel | **Blitz-Flügel** | Schwarz-weiße, 3D-Röhren-Blitze (je 5 Zackenarme pro Seite mit Seitenästen). Sie knistern, ein Lichtimpuls wandert durch jeden Arm, etwa alle 3 s blitzen alle auf. |
| Flügel | **Zuckerstangen-Flügel** (Christmas) | Rot-weiß gestreifte Candy Canes mit Haken, die Streifen wandern langsam. |
| Flügel | Inferno-Flügel | Züngelnde Flammenfedern, Dunkelrot bis Gelb. |
| Flügel | Void-Flügel | Violette Blitze mit cyanfarbenen Impulsen. |
| Heiligenschein | Gold / Zuckerstangen-Ring (Christmas) | Schwebender Ring mit umlaufendem Lichtpunkt. |
| Hüte | Weihnachtsmütze (Christmas), Königskrone | Mit Bommel bzw. leuchtenden Edelsteinen. |
| Spuren | Schnee (Christmas), Flammen, Funken | Partikel hinter dir. |

Die Flügel sind **prozedurale 3D-Geometrie** (keine Modelldateien, keine Texturen außer einem weißen Pixel).
Sie folgen dem Körper (Schleichen) und flattern leicht.

## Menü

- Links Kategorien, in der Mitte Live-Vorschau deines Spielers, rechts die Karten.
- **Überfahren einer Karte** zeigt das Cosmetic sofort am Spieler. **Klick** rüstet aus, nochmal Klick legt ab.
- In der Vorschau dreht sich der Spieler mit der Maus (Vanilla-Verhalten, nur ca. ±30°).
  Die Flügel siehst du am besten im Spiel mit **F5**.
- Die Auswahl wird in `config/vallicosmo.properties` gespeichert.

## Bauen

Voraussetzung: JDK 21.

1. Am einfachsten: Ordner in **IntelliJ IDEA** öffnen (als Gradle-Projekt), warten bis Gradle/Loom fertig sind,
   dann Gradle-Task `build` ausführen. Alternativ mit installiertem Gradle 9.x: `gradle build`.
   (Den Gradle-Wrapper `gradlew` + `gradle/wrapper/` kannst du aus der offiziellen Fabric-Example-Mod kopieren.)
2. Die fertige Mod liegt in `build/libs/vallicosmo-1.0.0.jar`.
3. In den `mods`-Ordner legen, zusammen mit **Fabric API 0.141.3+1.21.11**.

Versionen (siehe `gradle.properties`): Minecraft 1.21.11, Yarn `1.21.11+build.4`, Loader 0.18.1, Loom 1.14, Fabric API 0.141.3+1.21.11.

## Wichtig / Grenzen

- **Nur clientseitig.** Du siehst deine Cosmetics, andere Spieler sehen sie nicht (und du ihre nicht).
  Damit alle sie sehen, bräuchte es eine Server-Komponente mit Netzwerk-Sync - das ist nicht eingebaut.
- Wer eine **Elytra** trägt, sieht beide Flügel übereinander.
- **Nicht gegen echtes Minecraft gebaut.** Die Geometrie ist getestet (kompiliert, und als Bild gerendert zur Formkontrolle), der Teil, der Minecraft anspricht,
  wurde nur gegen die offizielle 1.21.11-Yarn-Dokumentation geschrieben. Falls der erste Build Fehler meldet, sind das
  am ehesten diese Stellen:
  - `VallicosmoClient`: Parameterliste des `LivingEntityFeatureRendererRegistrationCallback`
  - `CosmeticFeatureRenderer`: `ModelPart.applyTransform`, die Felder `isInSneakingPose` / `isGliding` im Render-State
  - `CosmeticScreen`: `mouseClicked(Click, boolean)`, `InventoryScreen.drawEntity(...)`, `PositionedSoundInstance.master(...)`
- Aussehen: Die Wings sind eine eigene Interpretation deiner Beschreibung (Zickzack-Schlangen als Blitze bzw. Zuckerstangen),
  kein Nachbau eines bestimmten Packs.

## Eigene Cosmetics hinzufügen

1. Zeile in `Cosmetic.java` ergänzen.
2. Flügel: Style in `WingGeometry.styleOf(...)`. Kopf: Form in `HeadGeometry`. Spur: Fall in `TrailTicker`.
