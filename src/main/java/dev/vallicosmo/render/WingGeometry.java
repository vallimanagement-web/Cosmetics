package dev.vallicosmo.render;

import dev.vallicosmo.Cosmetic;
import dev.vallicosmo.util.Colors;
import dev.vallicosmo.util.Noise;

/**
 * Baut die Flügel. Lokales Koordinatensystem (nach den Transformationen im FeatureRenderer):
 * x = seitlich, y = nach OBEN, z = nach HINTEN (weg vom Rücken). Ursprung = Schulterblatt-Mitte.
 *
 * Jeder Flügel besteht aus 5 "Speichen" (Blitz-Schlangen / Zuckerstangen / Flammenfedern).
 * Zwei Durchgänge: base() = solide Röhren, glow() = additiver Schein + wandernde Lichtimpulse.
 */
public final class WingGeometry {
	public enum Shape { ZIGZAG, CANE, FLAME, STORM, STORM_CANDY }

	public record Style(Shape shape, int base, int rim, int pulse, int glow, float pulseSpeed) {
	}

	public static final int SPOKES = 5;
	private static final float[] ANG = {78f, 55f, 33f, 12f, -12f};
	private static final float[] LEN = {0.85f, 1.15f, 1.28f, 1.10f, 0.80f};
	private static final int CANE_WHITE = 0xFFF7F7F7;

	private WingGeometry() {
	}

	public static Style styleOf(Cosmetic c) {
		return switch (c) {
			case WINGS_BLITZ -> new Style(Shape.ZIGZAG, 0xFF08080C, 0xFF3A3A46, 0xFFFFFFFF, 0xFFFFFFFF, 0.045f);
			case WINGS_VOID -> new Style(Shape.ZIGZAG, 0xFF14082A, 0xFF3B1A66, 0xFF4DF3FF, 0xFFB45CFF, 0.045f);
			case WINGS_CANDY -> new Style(Shape.CANE, 0xFFD6182B, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFC0C8, 0.025f);
			case WINGS_INFERNO -> new Style(Shape.FLAME, 0xFFB01806, 0xFFFF7A18, 0xFFFFE066, 0xFFFF7A18, 0.07f);
			// Sturm-Schwingen: base = dunkelstes Gefieder, rim = hellstes Gefieder, pulse = Blitzlicht, glow = Schein
			case WINGS_STORM -> new Style(Shape.STORM, 0xFF07070B, 0xFFC2C2D0, 0xFFFFFFFF, 0xFFE6ECFF, 0.045f);
			case WINGS_STORM_CANDY -> new Style(Shape.STORM_CANDY, 0xFFD6182B, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFC0C8, 0.03f);
			default -> null;
		};
	}

	/** Eine Speiche: Pfadpunkte (x,y,z), Radien, Bogenlänge und die ebenen (u,v)-Koordinaten. */
	public static final class Spoke {
		public final int n;
		public final float[] c;
		public final float[] r;
		public final float[] arc;
		final float[] pu;
		final float[] pv;
		final float[] along;

		Spoke(int n) {
			this.n = n;
			this.c = new float[n * 3];
			this.r = new float[n];
			this.arc = new float[n];
			this.pu = new float[n];
			this.pv = new float[n];
			this.along = new float[n];
		}
	}

	/** Kurzes Aufleuchten aller Blitze etwa alle 3 Sekunden (t in Ticks). */
	public static float flash(Style st, float t) {
		if (st.shape() != Shape.ZIGZAG && st.shape() != Shape.STORM) {
			return 0f;
		}
		float ph = t % 58f;
		return ph < 6f ? 1f - ph / 6f : 0f;
	}

	/** Helligkeit des wandernden Lichtimpulses an Segment seg von Speiche i. */
	public static float pulseW(Style st, int i, int n, float t, int seg, float flash) {
		float pp = ((t * st.pulseSpeed() + i * 0.37f) % 1f) * (n + 4) - 2f;
		float d = Math.abs(seg + 0.5f - pp);
		float w = Math.max(0f, 1f - d / 2.4f);
		return Math.min(1f, w + flash);
	}

	public static float flapAngle(float t, boolean sneaking, boolean gliding) {
		if (gliding) {
			return 0.95f + 0.04f * (float) Math.sin(t * 0.5f);
		}
		float base = sneaking ? 0.35f : 0.55f;
		return base + 0.16f * (float) Math.sin(t * 0.11f) + 0.05f * (float) Math.sin(t * 0.37f);
	}

	public static Spoke spoke(Style st, int i, float side, float t, float flap) {
		float ang = (float) Math.toRadians(ANG[i]);
		float dx = (float) Math.cos(ang);
		float dy = (float) Math.sin(ang);
		float nx = -dy;
		float ny = dx;
		float len = LEN[i];
		boolean cane = st.shape() == Shape.CANE;
		int segs = cane ? 12 : 11;
		int hookPts = cane ? 9 : 0;
		int n = segs + 1 + hookPts;
		Spoke sp = new Spoke(n);
		float ox = 0.05f;
		float oy = 0f;
		float shaft = cane ? len * 0.78f : len;
		float hookR = 0.11f;
		int seed = i * 131;
		float crackle = t * 0.5f;

		for (int j = 0; j < n; j++) {
			float u;
			float v;
			float al;
			float rad;
			if (j <= segs) {
				float f = j / (float) segs;
				al = shaft * f;
				float off = 0f;
				switch (st.shape()) {
					case ZIGZAG -> {
						off = ((j & 1) == 0 ? -1f : 1f) * 0.075f * (0.2f + 0.8f * f)
							* (1f + 0.3f * (float) Math.sin(t * 0.35f + i * 1.7f + j * 0.9f));
						if (j == 0) {
							off = 0f;
						}
						off += Noise.smooth(seed + j * 17, crackle) * 0.03f * f;
					}
					case FLAME -> off = 0.055f * (float) Math.sin(f * 7f - t * 0.33f + i * 1.3f) * f;
					case CANE -> off = 0f;
				}
				u = ox + dx * al + nx * off;
				v = oy + dy * al + ny * off;
				rad = switch (st.shape()) {
					case ZIGZAG -> 0.032f * (1f - 0.5f * f);
					case CANE -> 0.03f;
					case FLAME -> 0.034f * (1f - f) + 0.004f;
						default -> 0.03f;
				};
			} else {
				int h = j - segs;
				float ph = (float) Math.PI * h / hookPts;
				float ex = ox + dx * shaft;
				float ey = oy + dy * shaft;
				float sn = (float) Math.sin(ph);
				float cs = (float) Math.cos(ph);
				// Haken kippt nach unten/außen (gegen die Normale n)
				u = ex + dx * hookR * sn - nx * hookR * (1f - cs);
				v = ey + dy * hookR * sn - ny * hookR * (1f - cs);
				al = shaft + hookR * ph;
				rad = 0.03f;
			}
			float phi = flap + 0.22f * (al / 1.3f);
			sp.pu[j] = u;
			sp.pv[j] = v;
			sp.along[j] = al;
			sp.c[j * 3] = side * u * (float) Math.cos(phi);
			sp.c[j * 3 + 1] = v;
			sp.c[j * 3 + 2] = u * (float) Math.sin(phi);
			sp.r[j] = rad;
			if (j > 0) {
				float ddx = sp.c[j * 3] - sp.c[j * 3 - 3];
				float ddy = sp.c[j * 3 + 1] - sp.c[j * 3 - 2];
				float ddz = sp.c[j * 3 + 2] - sp.c[j * 3 - 1];
				sp.arc[j] = sp.arc[j - 1] + (float) Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
			}
		}
		return sp;
	}

	/** Kleiner Seitenast (nur bei Blitzen), startet bei Punkt 5 der Hauptspeiche. */
	private static Spoke branch(int i, float side, float t, float flap, Spoke main) {
		int n = 5;
		Spoke sp = new Spoke(n);
		int from = 5;
		float ang = (float) Math.toRadians(ANG[i]) + (i % 2 == 0 ? 0.7f : -0.7f);
		float dx = (float) Math.cos(ang);
		float dy = (float) Math.sin(ang);
		float nx = -dy;
		float ny = dx;
		for (int j = 0; j < n; j++) {
			float f = j / (float) (n - 1);
			float al = 0.38f * f;
			float off = j == 0 ? 0f : ((j & 1) == 0 ? -1f : 1f) * 0.04f * f
				* (1f + 0.3f * (float) Math.sin(t * 0.4f + i * 2.1f + j));
			float u = main.pu[from] + dx * al + nx * off;
			float v = main.pv[from] + dy * al + ny * off;
			float phi = flap + 0.22f * ((main.along[from] + al) / 1.3f);
			sp.c[j * 3] = side * u * (float) Math.cos(phi);
			sp.c[j * 3 + 1] = v;
			sp.c[j * 3 + 2] = u * (float) Math.sin(phi);
			sp.r[j] = 0.019f * (1f - 0.6f * f);
			if (j > 0) {
				float ddx = sp.c[j * 3] - sp.c[j * 3 - 3];
				float ddy = sp.c[j * 3 + 1] - sp.c[j * 3 - 2];
				float ddz = sp.c[j * 3 + 2] - sp.c[j * 3 - 1];
				sp.arc[j] = sp.arc[j - 1] + (float) Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
			}
		}
		return sp;
	}

	private static float[] refFor(float side, float flap) {
		return new float[] {-(float) Math.sin(flap), 0f, side * (float) Math.cos(flap)};
	}

	/** Durchgang 1: solide, schattierte Röhren. */
	public static void base(Mesh m, Style st, float t, float flap) {
		if (st.shape() == Shape.STORM || st.shape() == Shape.STORM_CANDY) {
			GothicWings.base(m, st, t, flap);
			return;
		}
		for (int s = 0; s < 2; s++) {
			float side = s == 0 ? 1f : -1f;
			float[] ref = refFor(side, flap);
			for (int i = 0; i < SPOKES; i++) {
				Spoke sp = spoke(st, i, side, t, flap);
				final int fi = i;
				final int n = sp.n;
				final float[] arc = sp.arc;
				Mesh.ColorFn col = switch (st.shape()) {
					case ZIGZAG -> (seg, k) -> (k % 3 == 0) ? st.rim() : st.base();
					case CANE -> (seg, k) -> {
						float band = ((arc[seg] + arc[seg + 1]) * 0.5f) / 0.07f + k / 3f - t * 0.05f;
						return (((int) Math.floor(band)) & 1) == 0 ? st.base() : CANE_WHITE;
					};
					case FLAME -> (seg, k) -> {
						float f = seg / (float) (n - 2);
						float g = Colors.clamp01(f + 0.18f * (float) Math.sin(t * 0.55f + seg * 0.9f + fi));
						return g < 0.5f
							? Colors.lerp(st.base(), st.rim(), g * 2f)
							: Colors.lerp(st.rim(), st.pulse(), (g - 0.5f) * 2f);
					};
						default -> (seg, k) -> st.base();
				};
				m.loft(sp.c, sp.r, sp.n, 6, ref, col, true, true);

				if (st.shape() == Shape.ZIGZAG) {
					Spoke br = branch(i, side, t, flap, sp);
					m.loft(br.c, br.r, br.n, 6, ref, (seg, k) -> (k % 3 == 0) ? st.rim() : st.base(), true, true);
				}
			}
		}
	}

	/** Durchgang 2 (additiv, fullbright): Schein um die Röhren + wandernde Lichtimpulse. */
	public static void glow(Mesh m, Style st, float t, float flap) {
		if (st.shape() == Shape.STORM || st.shape() == Shape.STORM_CANDY) {
			GothicWings.glow(m, st, t, flap);
			return;
		}
		float flash = flash(st, t);
		for (int s = 0; s < 2; s++) {
			float side = s == 0 ? 1f : -1f;
			float[] ref = refFor(side, flap);
			for (int i = 0; i < SPOKES; i++) {
				Spoke sp = spoke(st, i, side, t, flap);
				final int fi = i;
				final int n = sp.n;

				float haloA = switch (st.shape()) {
					case ZIGZAG -> 0.06f + 0.035f * (float) Math.sin(t * 0.3f + i) + flash * 0.18f;
					case CANE -> 0.05f + 0.025f * (float) Math.sin(t * 0.2f + i);
					case FLAME -> 0.11f + 0.05f * (float) Math.sin(t * 0.6f + i * 1.7f);
						default -> 0.05f;
				};
				float[] rHalo = new float[n];
				float[] rPulse = new float[n];
				for (int j = 0; j < n; j++) {
					rHalo[j] = sp.r[j] * 2.6f;
					rPulse[j] = sp.r[j] * 1.35f;
				}
				final int haloColor = Colors.alpha(st.glow(), haloA);
				m.loft(sp.c, rHalo, n, 6, ref, (seg, k) -> haloColor, false, false);
				m.loft(sp.c, rPulse, n, 6, ref, (seg, k) -> {
					float w = pulseW(st, fi, n, t, seg, flash);
					return Colors.alpha(st.pulse(), w * 0.95f);
				}, false, false);

				if (st.shape() == Shape.ZIGZAG) {
					Spoke br = branch(i, side, t, flap, sp);
					float[] rb = new float[br.n];
					for (int j = 0; j < br.n; j++) {
						rb[j] = br.r[j] * 2.4f;
					}
					m.loft(br.c, rb, br.n, 6, ref, (seg, k) -> haloColor, false, false);
				}
			}
		}
	}
}
