package dev.vallicosmo;

/** Kategorien im Cosmetics-Menü. Pro Kategorie kann genau ein Cosmetic aktiv sein. */
public enum Category {
	WINGS("Flügel"),
	HALOS("Heiligenscheine"),
	HATS("Hüte"),
	TRAILS("Spuren");

	public final String label;

	Category(String label) {
		this.label = label;
	}
}
