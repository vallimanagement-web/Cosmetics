package dev.vallicosmo.util;

/** Deterministisches 1D-Rauschen (-1..1), z. B. für das Knistern der Blitze. */
public final class Noise {
	private Noise() {
	}

	public static float hash(int n) {
		n = (n << 13) ^ n;
		return 1.0f - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f;
	}

	/** Weich interpoliertes Rauschen an Position x. */
	public static float smooth(int seed, float x) {
		int xi = (int) Math.floor(x);
		float f = x - xi;
		f = f * f * (3f - 2f * f);
		float a = hash(seed + xi * 7919);
		float b = hash(seed + (xi + 1) * 7919);
		return a + (b - a) * f;
	}
}
