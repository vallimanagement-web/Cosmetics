package dev.vallicosmo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Was ist ausgerüstet? Wird in config/vallicosmo.properties gespeichert. */
public final class CosmeticState {
	private static final Map<Category, Cosmetic> EQUIPPED = new EnumMap<>(Category.class);
	private static Cosmetic preview;

	/** Vom Command gesetzt, im nächsten Client-Tick öffnet sich das Menü (sonst schließt der Chat es sofort wieder). */
	public static boolean openRequested;

	private CosmeticState() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("vallicosmo.properties");
	}

	public static void load() {
		Path f = file();
		if (!Files.exists(f)) {
			// Beim ersten Start gleich etwas Schönes ausrüsten, damit man sofort etwas sieht.
			EQUIPPED.put(Category.WINGS, Cosmetic.WINGS_BLITZ);
			return;
		}
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(f)) {
			p.load(in);
		} catch (IOException e) {
			return;
		}
		for (Category cat : Category.values()) {
			Cosmetic c = Cosmetic.byName(p.getProperty(cat.name()));
			if (c != null && c.category == cat) {
				EQUIPPED.put(cat, c);
			}
		}
	}

	private static void save() {
		Properties p = new Properties();
		for (Map.Entry<Category, Cosmetic> e : EQUIPPED.entrySet()) {
			p.setProperty(e.getKey().name(), e.getValue().name());
		}
		try (OutputStream out = Files.newOutputStream(file())) {
			p.store(out, "Vallicosmo");
		} catch (IOException ignored) {
			// Speichern ist nur Komfort.
		}
	}

	public static Cosmetic equipped(Category cat) {
		return EQUIPPED.get(cat);
	}

	/** Was gerendert wird: die Menü-Vorschau (wenn gerade eines überfahren wird), sonst das Ausgerüstete. */
	public static Cosmetic shown(Category cat) {
		if (preview != null && preview.category == cat) {
			return preview;
		}
		return EQUIPPED.get(cat);
	}

	public static boolean isEquipped(Cosmetic c) {
		return EQUIPPED.get(c.category) == c;
	}

	/** Ausrüsten; nochmal klicken legt es ab. */
	public static void toggle(Cosmetic c) {
		if (isEquipped(c)) {
			EQUIPPED.remove(c.category);
		} else {
			EQUIPPED.put(c.category, c);
		}
		save();
	}

	public static void clearAll() {
		EQUIPPED.clear();
		save();
	}

	public static int equippedCount() {
		return EQUIPPED.size();
	}

	public static void setPreview(Cosmetic c) {
		preview = c;
	}
}
