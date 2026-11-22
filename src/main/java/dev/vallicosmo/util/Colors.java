package dev.vallicosmo.util;

/** Kleine ARGB-Helfer (0xAARRGGBB). */
public final class Colors {
	private Colors() {
	}

	public static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	/** Setzt den Alpha-Kanal einer RGB-Farbe (Alpha 0..1). */
	public static int alpha(int rgb, float a) {
		return ((int) (clamp01(a) * 255f + 0.5f) << 24) | (rgb & 0xFFFFFF);
	}

	/** Multipliziert den vorhandenen Alpha-Kanal mit f. */
	public static int mulAlpha(int argb, float f) {
		float a = ((argb >>> 24) & 0xFF) / 255f;
		return alpha(argb, a * f);
	}

	public static int lerp(int a, int b, float t) {
		t = clamp01(t);
		int aa = (int) (((a >>> 24) & 0xFF) + (((b >>> 24) & 0xFF) - ((a >>> 24) & 0xFF)) * t);
		int ar = (int) (((a >>> 16) & 0xFF) + (((b >>> 16) & 0xFF) - ((a >>> 16) & 0xFF)) * t);
		int ag = (int) (((a >>> 8) & 0xFF) + (((b >>> 8) & 0xFF) - ((a >>> 8) & 0xFF)) * t);
		int ab = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
		return (aa << 24) | (ar << 16) | (ag << 8) | ab;
	}

	/** HSV -> opake ARGB-Farbe. h, s, v jeweils 0..1. */
	public static int hsv(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		float r;
		float g;
		float b;
		float hh = h * 6f;
		int i = (int) Math.floor(hh);
		float f = hh - i;
		float p = v * (1f - s);
		float q = v * (1f - s * f);
		float t = v * (1f - s * (1f - f));
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return 0xFF000000 | ((int) (r * 255f) << 16) | ((int) (g * 255f) << 8) | (int) (b * 255f);
	}
}
