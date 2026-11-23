package dev.vallicosmo.render;

import java.util.Base64;

import dev.vallicosmo.render.WingGeometry.Shape;
import dev.vallicosmo.render.WingGeometry.Style;
import dev.vallicosmo.util.Colors;
import dev.vallicosmo.util.Noise;

/**
 * Sturm-Schwingen nach Vorlage: ein gebogener, ausgefranster Halbmond mit spitzen Federn oben, sichelförmigem Schwanz
 * unten und einem dicken Bogen an der Innenseite (dort sitzt der Drachenkörper der Vorlage - hier stattdessen Blitze).
 *
 * Die Umrissform ist als 66x85-Raster eingebacken (DATA, je Zelle 2 Bildpunkte der Vorlage):
 * 0 = leer, 1..127 = Membran (Wert = Helligkeit der Ader), 128..255 = Bogenplatte (Wert = Helligkeit).
 * Die Fläche wird aus farbigen Vierecken gebaut, darüber liegen echte 3D-Blitz-Röhren.
 *
 * Koordinaten: (u,v) = Flügelebene (u nach außen, v nach oben), danach nach hinten geklappt (wie WingGeometry).
 */
final class GothicWings {
	private static final int GW = 66;
	private static final int GH = 85;
	private static final int X0 = 70;
	private static final int Y0 = 154;
	private static final int CELL = 2;
	/** Innenkante und Höhe des Ansatzes in Bildpunkten der Vorlage. */
	private static final float XIN = 201f;
	private static final float YO = 250f;
	/** Blöcke pro Bildpunkt und Abstand der Innenkante von der Rückenmitte. */
	private static final float K = 0.0070f;
	private static final float U0 = 0.10f;
	private static final float HALF = CELL * K / 2f;

	private static final int RED = 0xFFD6182B;
	private static final int WHITE = 0xFFF7F7F7;

	private static final String DATA =
		"AAAACwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEwAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAASKQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAVHQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAJAwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"AAABBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADHwAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAANGiQAAAAAAAAAAAAAAAAtAAAAAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAkEhMAAABHAAAAAAAAAAAZLwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAaMTo8AAA4IgAAAAAAAAAAHgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"AAAHDCA8JAAADQAAAAAAAAAAC4sAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAANCRg2FwAAEQcAAAAAAA" +
		"AAER2dAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMTw6KEIALAMPAAAAAAAADxeUAAAAAAAAAAAAAAAA" +
		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFidCQkcyMRQRMgAAAAAAAC4zoQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
		"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";

	/** Blitzpfade in Bildpunkten der Vorlage (x, y): Hornklinge, Innenbogen, drei Dornen. */
	private static final float[][][] BOLTS = {
		{{105, 168}, {115, 185}, {126, 198}, {142, 206}, {160, 211}, {175, 219}},
		{{170, 222}, {156, 232}, {152, 244}, {160, 255}, {172, 267}, {182, 281}, {180, 295}, {172, 303}},
		{{160, 253}, {188, 237}},
		{{184, 267}, {198, 246}},
		{{190, 273}, {201, 263}},
	};
	private static final float[] BOLT_R = {0.034f, 0.040f, 0.022f, 0.022f, 0.020f};

	private static final int CELLS;
	private static final float[] CU;
	private static final float[] CV;
	private static final byte[] KIND;
	private static final float[] VAL;
	private static final int[] STORM_COL;
	private static final int[] CANDY_COL;

	static {
		byte[] raw = Base64.getDecoder().decode(DATA);
		int n = 0;
		for (byte b : raw) {
			if (b != 0) {
				n++;
			}
		}
		CELLS = n;
		CU = new float[n];
		CV = new float[n];
		KIND = new byte[n];
		VAL = new float[n];
		STORM_COL = new int[n];
		CANDY_COL = new int[n];
		int i = 0;
		for (int cy = 0; cy < GH; cy++) {
			for (int cx = 0; cx < GW; cx++) {
				int b = raw[cy * GW + cx] & 0xFF;
				if (b == 0) {
					continue;
				}
				float px = X0 + cx * CELL + CELL / 2f;
				float py = Y0 + cy * CELL + CELL / 2f;
				CU[i] = U0 + (XIN - px) * K;
				CV[i] = (YO - py) * K;
				boolean spine = b >= 128;
				KIND[i] = (byte) (spine ? 2 : 1);
				VAL[i] = spine ? (b - 128) / 127f : (b - 1) / 126f;
				STORM_COL[i] = stormColor(spine, VAL[i]);
				CANDY_COL[i] = candyColor(spine, VAL[i], cx, cy);
				i++;
			}
		}
	}

	private GothicWings() {
	}

	private static float vein(float n) {
		float x = Math.max(0f, Math.min(1f, (n - 0.18f) / 0.37f));
		return (float) Math.pow(x, 1.3);
	}

	private static int stormColor(boolean spine, float n) {
		if (spine) {
			return Colors.lerp(0xFF3A3A48, 0xFFFFFFFF, n);
		}
		int base = Colors.lerp(0xFF121218, 0xFF383844, Math.min(1f, n * 3f));
		return Colors.lerp(base, 0xFFF0F2FF, vein(n));
	}

	private static int candyColor(boolean spine, float n, int cx, int cy) {
		if (spine) {
			boolean red = (((cx * 2 + cy * 3) / 7) & 1) == 0;
			return Colors.lerp(0xFF000000, red ? RED : WHITE, 0.55f + 0.45f * n);
		}
		int base = Colors.lerp(0xFF3A0A10, 0xFFB01424, Math.min(1f, n * 3f));
		return Colors.lerp(base, 0xFFFFE6EA, vein(n) * 0.75f);
	}

	private static boolean isCandy(Style st) {
		return st.shape() == Shape.STORM_CANDY;
	}

	private static float[] ref(float side, float flap) {
		return new float[] {-(float) Math.sin(flap), 0f, side * (float) Math.cos(flap)};
	}

	/** (u,v) der Flügelebene -> 3D: nach außen, mit Flügelschlag nach hinten geklappt, Spitzen und Schwanz wehen. */
	private static void put(float[] c, int j, float side, float u, float v, float flap, float t, float lz) {
		float du = u - U0;
		float wave = 0.075f * du * du * (float) Math.sin(t * 0.10f - 2.6f * u + 1.8f * v);
		float phi = flap + 0.20f * u;
		c[j * 3] = side * u * (float) Math.cos(phi);
		c[j * 3 + 1] = v;
		c[j * 3 + 2] = u * (float) Math.sin(phi) + lz + wave;
	}

	private static float[] lift(float[] pl, int n, float side, float flap, float t, float lz) {
		float[] c = new float[n * 3];
		for (int j = 0; j < n; j++) {
			put(c, j, side, pl[j * 2], pl[j * 2 + 1], flap, t, lz);
		}
		return c;
	}

	/** Zickzack-Blitz entlang eines Pfades (Bildpunkte der Vorlage); Rückgabe: u,v-Paare. */
	private static float[] zig(float[][] px, float amp, float spacing, float t, int salt) {
		int m = px.length;
		float[] pu = new float[m];
		float[] pv = new float[m];
		float[] cum = new float[m];
		for (int i = 0; i < m; i++) {
			pu[i] = U0 + (XIN - px[i][0]) * K;
			pv[i] = (YO - px[i][1]) * K;
			if (i > 0) {
				cum[i] = cum[i - 1] + (float) Math.hypot(pu[i] - pu[i - 1], pv[i] - pv[i - 1]);
			}
		}
		float total = cum[m - 1];
		int n = Math.max(3, Math.round(total / spacing) + 1);
		float[] out = new float[n * 2];
		int seg = 0;
		for (int j = 0; j < n; j++) {
			float d = total * j / (n - 1);
			while (seg < m - 2 && cum[seg + 1] < d) {
				seg++;
			}
			float segLen = Math.max(cum[seg + 1] - cum[seg], 1e-5f);
			float f = Math.min(1f, Math.max(0f, (d - cum[seg]) / segLen));
			float x = pu[seg] + (pu[seg + 1] - pu[seg]) * f;
			float y = pv[seg] + (pv[seg + 1] - pv[seg]) * f;
			float dx = (pu[seg + 1] - pu[seg]) / segLen;
			float dy = (pv[seg + 1] - pv[seg]) / segLen;
			float off = 0f;
			if (j != 0 && j != n - 1) {
				off = ((j & 1) == 0 ? -1f : 1f) * amp * (1f + 0.3f * (float) Math.sin(t * 0.4f + j * 1.7f + salt))
					+ Noise.smooth(salt + j * 13, t * 0.5f) * amp * 0.4f;
			}
			out[j * 2] = x - dy * off;
			out[j * 2 + 1] = y + dx * off;
		}
		return out;
	}

	private static float[] radii(int n, float r0) {
		float[] r = new float[n];
		for (int j = 0; j < n; j++) {
			float f = j / (float) (n - 1);
			r[j] = r0 * (1f - 0.75f * f * f);
		}
		return r;
	}

	private static int boltColor(Style st, boolean candy, int seg, int k) {
		if (candy) {
			int c = (seg & 1) == 0 ? st.base() : WHITE;
			return k % 3 == 0 ? Colors.lerp(c, 0xFF000000, 0.18f) : c;
		}
		return k % 3 == 0 ? 0xFFB4B4C6 : 0xFFF4F4FB;
	}

	/** Durchgang 1: Fläche aus Vierecken + solide Blitz-Röhren. */
	static void base(Mesh m, Style st, float t, float flap) {
		boolean candy = isCandy(st);
		int[] col = candy ? CANDY_COL : STORM_COL;
		float[] q = new float[12];
		for (int s = 0; s < 2; s++) {
			float side = s == 0 ? 1f : -1f;
			float[] ref = ref(side, flap);
			for (int i = 0; i < CELLS; i++) {
				float u = CU[i];
				float v = CV[i];
				float lz = KIND[i] == 2 ? 0.012f : 0f;
				put(q, 0, side, u - HALF, v + HALF, flap, t, lz);
				put(q, 1, side, u + HALF, v + HALF, flap, t, lz);
				put(q, 2, side, u + HALF, v - HALF, flap, t, lz);
				put(q, 3, side, u - HALF, v - HALF, flap, t, lz);
				m.quad(q, ref[0], ref[1], ref[2], col[i]);
			}
			for (int b = 0; b < BOLTS.length; b++) {
				float[] pl = zig(BOLTS[b], b < 2 ? 0.035f : 0.028f, 0.085f, t, b * 31 + 5);
				int n = pl.length / 2;
				m.loft(lift(pl, n, side, flap, t, 0.035f), radii(n, BOLT_R[b]), n, 6, ref,
					(seg, k) -> boltColor(st, candy, seg, k), true, true);
			}
		}
	}

	/** Durchgang 2 (additiv): leuchtende Adern, Schein und wandernde Lichtimpulse auf den Blitzen. */
	static void glow(Mesh m, Style st, float t, float flap) {
		boolean candy = isCandy(st);
		float flash = WingGeometry.flash(st, t);
		float haloA = 0.08f + 0.04f * (float) Math.sin(t * 0.3f) + flash * 0.2f;
		int haloColor = Colors.alpha(st.glow(), haloA);
		float[] q = new float[12];
		for (int s = 0; s < 2; s++) {
			float side = s == 0 ? 1f : -1f;
			float[] ref = ref(side, flap);
			for (int i = 0; i < CELLS; i++) {
				float u = CU[i];
				float v = CV[i];
				float a;
				int c;
				if (KIND[i] == 1) {
					float ve = vein(VAL[i]);
					if (ve < 0.3f) {
						continue;
					}
					float w = 0.5f + 0.5f * (float) Math.sin(t * 0.12f - (u * 5f + v * 3f));
					a = ve * (0.10f + 0.55f * w) + flash * 0.5f * ve;
					c = Colors.alpha(st.pulse(), a);
				} else {
					a = 0.10f + 0.10f * (float) Math.sin(t * 0.2f + u * 4f) + flash * 0.25f;
					c = Colors.alpha(st.glow(), a);
				}
				put(q, 0, side, u - HALF, v + HALF, flap, t, 0.008f);
				put(q, 1, side, u + HALF, v + HALF, flap, t, 0.008f);
				put(q, 2, side, u + HALF, v - HALF, flap, t, 0.008f);
				put(q, 3, side, u - HALF, v - HALF, flap, t, 0.008f);
				m.quad(q, ref[0], ref[1], ref[2], c);
			}
			for (int b = 0; b < BOLTS.length; b++) {
				float[] pl = zig(BOLTS[b], b < 2 ? 0.035f : 0.028f, 0.085f, t, b * 31 + 5);
				int n = pl.length / 2;
				float[] c3 = lift(pl, n, side, flap, t, 0.035f);
				float[] r = radii(n, BOLT_R[b]);
				float[] rh = new float[n];
				float[] rp = new float[n];
				for (int j = 0; j < n; j++) {
					rh[j] = r[j] * 2.8f;
					rp[j] = r[j] * 1.35f;
				}
				m.loft(c3, rh, n, 6, ref, (seg, k) -> haloColor, false, false);
				final int phase = b * 2;
				m.loft(c3, rp, n, 6, ref,
					(seg, k) -> Colors.alpha(st.pulse(), WingGeometry.pulseW(st, phase, n, t, seg, flash) * 0.95f), false, false);
			}
		}
	}
}
