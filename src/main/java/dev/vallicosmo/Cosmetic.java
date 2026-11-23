package dev.vallicosmo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Alle Cosmetics. Neues Cosmetic = neue Zeile hier + (je nach Kategorie) ein Eintrag in
 * WingGeometry.styleOf(...) bzw. HeadGeometry / TrailTicker.
 */
public enum Cosmetic {
	// --- Flügel ---
	WINGS_BLITZ(Category.WINGS, "Blitz-Flügel",
		"Schwarz-weiße Blitzschlangen. Sie knistern, pulsieren und leuchten regelmäßig auf.",
		Rarity.LEGENDARY, false, 0xFFFFFFFF, 0xFF15151C),
	WINGS_CANDY(Category.WINGS, "Zuckerstangen-Flügel",
		"Christmas Edition: rot-weiß gestreifte Candy Canes mit Haken statt Blitzen.",
		Rarity.LEGENDARY, true, 0xFFE8283C, 0xFFFFFFFF),
	WINGS_STORM(Category.WINGS, "Sturm-Schwingen",
		"Ausgefranste Schwingen in Schwarz, Grau und Weiß mit leuchtenden Blitz-Adern. Ein Blitzbogen mit Dornen trägt sie.",
		Rarity.LEGENDARY, false, 0xFFFFFFFF, 0xFF15151C),
	WINGS_STORM_CANDY(Category.WINGS, "Zuckerstangen-Schwingen",
		"Christmas Edition: dieselbe Form in Rot mit weißen Adern und einem rot-weißen Zuckerstangen-Blitzbogen.",
		Rarity.LEGENDARY, true, 0xFFE8283C, 0xFFFFFFFF),
	WINGS_INFERNO(Category.WINGS, "Inferno-Flügel",
		"Züngelnde Flammenfedern, die von Dunkelrot bis Gelb flackern.",
		Rarity.EPIC, false, 0xFFFF7A18, 0xFFFFE066),
	WINGS_VOID(Category.WINGS, "Void-Flügel",
		"Violette Blitze mit cyanfarbenen Energieimpulsen aus dem Nichts.",
		Rarity.EPIC, false, 0xFFB45CFF, 0xFF4DF3FF),

	// --- Heiligenscheine ---
	HALO_GOLD(Category.HALOS, "Goldener Heiligenschein",
		"Ein schwebender Goldring mit einem Lichtpunkt, der ihn umkreist.",
		Rarity.RARE, false, 0xFFFFC933, 0xFFFFF2A0),
	HALO_CANDY(Category.HALOS, "Zuckerstangen-Ring",
		"Christmas Edition: ein rot-weiß gestreifter Ring, dessen Streifen wandern.",
		Rarity.RARE, true, 0xFFE8283C, 0xFFFFFFFF),

	// --- Hüte ---
	HAT_SANTA(Category.HATS, "Weihnachtsmütze",
		"Christmas Edition: rote Zipfelmütze mit flauschigem Rand und Bommel.",
		Rarity.EPIC, true, 0xFFD0202E, 0xFFF4F4F4),
	HAT_CROWN(Category.HATS, "Königskrone",
		"Goldene Krone mit acht Zacken und leuchtenden Edelsteinen.",
		Rarity.EPIC, false, 0xFFFFC933, 0xFFFF4D6D),

	// --- Spuren ---
	TRAIL_SNOW(Category.TRAILS, "Schneeflocken-Spur",
		"Christmas Edition: hinter dir rieseln Schneeflocken.",
		Rarity.RARE, true, 0xFFBEE9FF, 0xFFFFFFFF),
	TRAIL_FLAME(Category.TRAILS, "Flammen-Spur",
		"Du hinterlässt eine Spur aus Feuer.",
		Rarity.RARE, false, 0xFFFF7A18, 0xFFFFE066),
	TRAIL_SPARK(Category.TRAILS, "Funken-Spur",
		"Elektrische Funken knistern an deinen Füßen.",
		Rarity.RARE, false, 0xFFFFFFFF, 0xFF9FE8FF);

	public final Category category;
	public final String title;
	public final String description;
	public final Rarity rarity;
	public final boolean christmas;
	public final int primary;
	public final int secondary;

	Cosmetic(Category category, String title, String description, Rarity rarity, boolean christmas, int primary, int secondary) {
		this.category = category;
		this.title = title;
		this.description = description;
		this.rarity = rarity;
		this.christmas = christmas;
		this.primary = primary;
		this.secondary = secondary;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static List<Cosmetic> of(Category category) {
		List<Cosmetic> out = new ArrayList<>();
		for (Cosmetic c : values()) {
			if (c.category == category) {
				out.add(c);
			}
		}
		return out;
	}

	public static Cosmetic byName(String name) {
		if (name == null) {
			return null;
		}
		try {
			return valueOf(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
