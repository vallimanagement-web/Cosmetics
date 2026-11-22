package dev.vallicosmo;

public enum Rarity {
	COMMON("Gewöhnlich", 0xFFB0B7C3),
	RARE("Selten", 0xFF4FA3FF),
	EPIC("Episch", 0xFFB45CFF),
	LEGENDARY("Legendär", 0xFFFFB02E);

	public final String label;
	public final int color;

	Rarity(String label, int color) {
		this.label = label;
		this.color = color;
	}
}
