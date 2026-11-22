package dev.vallicosmo.render;

import dev.vallicosmo.Cosmetic;
import dev.vallicosmo.util.Colors;

/**
 * Heiligenscheine, Weihnachtsmütze und Krone.
 * Lokales System: Ursprung = Mitte der Kopfoberseite, y = nach OBEN, z = nach HINTEN (Gesicht zeigt nach -z).
 */
public final class HeadGeometry {
	private static final int RED = 0xFFD0202E;
	private static final int RED_DARK = 0xFFA8121F;
	private static final int FLUFF = 0xFFF4F4F4;
	private static final int FLUFF_SHADE = 0xFFDADADA;
	private static final int CANE_WHITE = 0xFFF7F7F7;
	private static final int HALO_POINTS = 29;

	private HeadGeometry() {
	}

	private static float[] ring(float radius, float y, int points, float bob) {
		float[] c = new float[points * 3];
		for (int i = 0; i < points; i++) {
			double a = 2.0 * Math.PI * i / (points - 1);
			c[i * 3] = (float) Math.cos(a) * radius;
			c[i * 3 + 1] = y + bob;
			c[i * 3 + 2] = (float) Math.sin(a) * radius;
		}
		return c;
	}

	private static float[] constant(int n, float v) {
		float[] r = new float[n];
		java.util.Arrays.fill(r, v);
		return r;
	}

	/** Durchgang 1 des Heiligenscheins (solide Röhre). */
	public static void haloBase(Mesh m, Cosmetic c, float t) {
		float bob = 0.015f * (float) Math.sin(t * 0.1f);
		float[] pts = ring(0.31f, 0f, HALO_POINTS, bob);
		float[] r = constant(HALO_POINTS, c == Cosmetic.HALO_CANDY ? 0.022f : 0.017f);
		if (c == Cosmetic.HALO_CANDY) {
			float[] arc = arcLengths(pts, HALO_POINTS);
			m.loft(pts, r, HALO_POINTS, 6, Mesh.REF_Y, (seg, k) -> {
				float band = ((arc[seg] + arc[seg + 1]) * 0.5f) / 0.07f + k / 3f - t * 0.06f;
				return (((int) Math.floor(band)) & 1) == 0 ? c.primary : CANE_WHITE;
			}, false, false);
		} else {
			m.loft(pts, r, HALO_POINTS, 6, Mesh.REF_Y, (seg, k) -> (k % 3 == 0) ? c.secondary : c.primary, false, false);
		}
	}

	/** Durchgang 2: Schein + umlaufender Lichtpunkt (additiv). */
	public static void haloGlow(Mesh m, Cosmetic c, float t) {
		float bob = 0.015f * (float) Math.sin(t * 0.1f);
		float[] pts = ring(0.31f, 0f, HALO_POINTS, bob);
		int glowRgb = c == Cosmetic.HALO_CANDY ? 0xFFFFC0C8 : c.primary;
		float a = 0.16f + 0.06f * (float) Math.sin(t * 0.2f);
		int haloColor = Colors.alpha(glowRgb, a);
		m.loft(pts, constant(HALO_POINTS, 0.06f), HALO_POINTS, 6, Mesh.REF_Y, (seg, k) -> haloColor, false, false);

		float pp = ((t * 0.02f) % 1f) * (HALO_POINTS + 4) - 2f;
		m.loft(pts, constant(HALO_POINTS, 0.03f), HALO_POINTS, 6, Mesh.REF_Y, (seg, k) -> {
			float w = Math.max(0f, 1f - Math.abs(seg + 0.5f - pp) / 3f);
			return Colors.alpha(0xFFFFFFFF, w * 0.95f);
		}, false, false);
	}

	/** Weihnachtsmütze oder Krone (solide). */
	public static void hat(Mesh m, Cosmetic c, float t) {
		if (c == Cosmetic.HAT_SANTA) {
			santa(m, t);
		} else if (c == Cosmetic.HAT_CROWN) {
			crown(m, t);
		}
	}

	private static void santa(Mesh m, float t) {
		// flauschiger Rand
		int rp = 25;
		float[] brim = ring(0.29f, 0.03f, rp, 0f);
		m.loft(brim, constant(rp, 0.05f), rp, 6, Mesh.REF_Y, (seg, k) -> (k % 2 == 0) ? FLUFF : FLUFF_SHADE, false, false);

		// Zipfel: wird nach oben schmaler und kippt nach hinten
		int n = 10;
		float[] c = new float[n * 3];
		float[] r = new float[n];
		float sway = 0.015f * (float) Math.sin(t * 0.12f);
		for (int i = 0; i < n; i++) {
			float f = i / (float) (n - 1);
			c[i * 3] = sway * f;
			c[i * 3 + 1] = 0.05f + 0.36f * f - 0.10f * f * f * f;
			c[i * 3 + 2] = 0.30f * f * f;
			r[i] = 0.265f * (float) Math.pow(1f - f, 0.85) + 0.014f;
		}
		m.loft(c, r, n, 8, Mesh.REF_Z, (seg, k) -> ((k + seg) % 2 == 0) ? RED : RED_DARK, false, true);

		// Bommel
		m.blob(c[(n - 1) * 3], c[(n - 1) * 3 + 1] - 0.02f, c[(n - 1) * 3 + 2], 0.068f, FLUFF);
	}

	private static void crown(Mesh m, float t) {
		int rp = 25;
		float[] band = ring(0.275f, 0.03f, rp, 0f);
		Cosmetic cr = Cosmetic.HAT_CROWN;
		m.loft(band, constant(rp, 0.034f), rp, 6, Mesh.REF_Y, (seg, k) -> (k % 3 == 0) ? 0xFFFFE27A : cr.primary, false, false);

		int spikes = 8;
		for (int s = 0; s < spikes; s++) {
			double a = 2.0 * Math.PI * s / spikes;
			float px = (float) Math.cos(a) * 0.275f;
			float pz = (float) Math.sin(a) * 0.275f;
			float h = (s % 2 == 0) ? 0.2f : 0.14f;
			float[] c = {px, 0.04f, pz, px, 0.04f + h, pz};
			float[] r = {0.05f, 0.004f};
			m.loft(c, r, 2, 4, Mesh.REF_Z, (seg, k) -> (k % 2 == 0) ? cr.primary : 0xFFE0A81C, false, false);
			float glint = 0.5f + 0.5f * (float) Math.sin(t * 0.15f + s);
			int gem = (s % 2 == 0) ? Colors.lerp(cr.secondary, 0xFFFFFFFF, glint * 0.5f) : Colors.lerp(0xFF4D9BFF, 0xFFFFFFFF, glint * 0.5f);
			m.blob(px, 0.04f + h + 0.015f, pz, 0.03f, gem);
		}
	}

	private static float[] arcLengths(float[] pts, int n) {
		float[] arc = new float[n];
		for (int i = 1; i < n; i++) {
			float dx = pts[i * 3] - pts[i * 3 - 3];
			float dy = pts[i * 3 + 1] - pts[i * 3 - 2];
			float dz = pts[i * 3 + 2] - pts[i * 3 - 1];
			arc[i] = arc[i - 1] + (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
		return arc;
	}
}
