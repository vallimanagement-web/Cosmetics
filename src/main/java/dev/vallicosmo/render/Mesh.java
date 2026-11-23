package dev.vallicosmo.render;

/**
 * Reine Geometrie ohne Minecraft-Klassen: baut Röhren/Kegel ("Loft") entlang eines Pfades.
 * Jede Querschnittsscheibe ist ein regelmäßiges n-Eck; Vertex-Normalen zeigen radial nach außen
 * (weiches Shading -> wirkt wie eine runde Schlange statt wie ein Kasten).
 */
public final class Mesh {
	public static final float[] REF_Z = {0f, 0f, 1f};
	public static final float[] REF_Y = {0f, 1f, 0f};

	@FunctionalInterface
	public interface ColorFn {
		/** Farbe (ARGB) für Segment seg (zwischen Ring seg und seg+1) und Seite face. Alpha 0 = überspringen. */
		int color(int seg, int face);
	}

	private final VertexSink sink;

	public Mesh(VertexSink sink) {
		this.sink = sink;
	}

	/**
	 * @param c     Pfadpunkte (x,y,z hintereinander), mindestens 2
	 * @param r     Radius pro Pfadpunkt
	 * @param n     Anzahl Pfadpunkte
	 * @param sides Seiten des Querschnitts (6 = rund genug, gerade Zahl für Endkappen)
	 * @param ref   Referenzvektor für die Orientierung des Querschnitts (z. B. Normale der Ebene, in der der Pfad liegt)
	 */
	public void loft(float[] c, float[] r, int n, int sides, float[] ref, ColorFn col, boolean capStart, boolean capEnd) {
		if (n < 2 || sides < 3) {
			return;
		}
		float[][] pos = new float[n][sides * 3];
		float[][] nor = new float[n][sides * 3];
		float[][] tan = new float[n][3];

		for (int i = 0; i < n; i++) {
			int i0 = Math.max(i - 1, 0);
			int i1 = Math.min(i + 1, n - 1);
			float tx = c[i1 * 3] - c[i0 * 3];
			float ty = c[i1 * 3 + 1] - c[i0 * 3 + 1];
			float tz = c[i1 * 3 + 2] - c[i0 * 3 + 2];
			float tl = len(tx, ty, tz);
			if (tl < 1e-6f) {
				tx = 0f;
				ty = 1f;
				tz = 0f;
				tl = 1f;
			}
			tx /= tl;
			ty /= tl;
			tz /= tl;
			tan[i][0] = tx;
			tan[i][1] = ty;
			tan[i][2] = tz;

			// b1 = t x ref
			float bx = ty * ref[2] - tz * ref[1];
			float by = tz * ref[0] - tx * ref[2];
			float bz = tx * ref[1] - ty * ref[0];
			float bl = len(bx, by, bz);
			if (bl < 1e-3f) {
				float ax = 0f;
				float ay = 1f;
				float az = 0f;
				if (Math.abs(ty) >= 0.9f) {
					ax = 1f;
					ay = 0f;
				}
				bx = ty * az - tz * ay;
				by = tz * ax - tx * az;
				bz = tx * ay - ty * ax;
				bl = len(bx, by, bz);
			}
			bx /= bl;
			by /= bl;
			bz /= bl;
			// b2 = t x b1  ->  (b1, b2, t) ist rechtshändig
			float cx = ty * bz - tz * by;
			float cy = tz * bx - tx * bz;
			float cz = tx * by - ty * bx;

			for (int k = 0; k < sides; k++) {
				double a = 2.0 * Math.PI * k / sides;
				float ca = (float) Math.cos(a);
				float sa = (float) Math.sin(a);
				float rx = ca * bx + sa * cx;
				float ry = ca * by + sa * cy;
				float rz = ca * bz + sa * cz;
				pos[i][k * 3] = c[i * 3] + rx * r[i];
				pos[i][k * 3 + 1] = c[i * 3 + 1] + ry * r[i];
				pos[i][k * 3 + 2] = c[i * 3 + 2] + rz * r[i];
				nor[i][k * 3] = rx;
				nor[i][k * 3 + 1] = ry;
				nor[i][k * 3 + 2] = rz;
			}
		}

		for (int i = 0; i < n - 1; i++) {
			for (int k = 0; k < sides; k++) {
				int k2 = (k + 1) % sides;
				int color = col.color(i, k);
				if ((color >>> 24) == 0) {
					continue;
				}
				v(pos[i], nor[i], k, color);
				v(pos[i], nor[i], k2, color);
				v(pos[i + 1], nor[i + 1], k2, color);
				v(pos[i + 1], nor[i + 1], k, color);
			}
		}

		if (sides % 2 == 0) {
			if (capEnd) {
				int e = n - 1;
				int color = col.color(Math.max(n - 2, 0), 0);
				if ((color >>> 24) != 0) {
					for (int k = 0; k < sides; k += 2) {
						cap(c, e, pos[e], k, sides, color, tan[e][0], tan[e][1], tan[e][2], false);
					}
				}
			}
			if (capStart) {
				int color = col.color(0, 0);
				if ((color >>> 24) != 0) {
					for (int k = 0; k < sides; k += 2) {
						cap(c, 0, pos[0], k, sides, color, -tan[0][0], -tan[0][1], -tan[0][2], true);
					}
				}
			}
		}
	}

	/**
	 * Flache, spitz zulaufende Klinge (Feder): Querschnitt ist eine flache Raute mit Mittelgrat.
	 *
	 * @param c   Pfadpunkte (x,y,z hintereinander)
	 * @param w   halbe Breite pro Pfadpunkt (0 = Spitze)
	 * @param h   halbe Dicke pro Pfadpunkt
	 * @param ref Normale der Flügelebene (bestimmt, wohin "breit" und "dick" zeigen)
	 * @param col Farbe je Segment und Seite (0 = oben/vorn-links, 1 = oben/hinten, 2, 3 = untere Hälfte)
	 */
	public void blade(float[] c, float[] w, float[] h, int n, float[] ref, ColorFn col) {
		if (n < 2) {
			return;
		}
		float[][] b1 = new float[n][3];
		float[][] b2 = new float[n][3];
		for (int i = 0; i < n; i++) {
			int i0 = Math.max(i - 1, 0);
			int i1 = Math.min(i + 1, n - 1);
			float tx = c[i1 * 3] - c[i0 * 3];
			float ty = c[i1 * 3 + 1] - c[i0 * 3 + 1];
			float tz = c[i1 * 3 + 2] - c[i0 * 3 + 2];
			float tl = len(tx, ty, tz);
			if (tl < 1e-6f) {
				tx = 0f;
				ty = 1f;
				tz = 0f;
				tl = 1f;
			}
			tx /= tl;
			ty /= tl;
			tz /= tl;
			float bx = ty * ref[2] - tz * ref[1];
			float by = tz * ref[0] - tx * ref[2];
			float bz = tx * ref[1] - ty * ref[0];
			float bl = len(bx, by, bz);
			if (bl < 1e-4f) {
				bx = 1f;
				by = 0f;
				bz = 0f;
				bl = 1f;
			}
			bx /= bl;
			by /= bl;
			bz /= bl;
			b1[i][0] = bx;
			b1[i][1] = by;
			b1[i][2] = bz;
			b2[i][0] = ty * bz - tz * by;
			b2[i][1] = tz * bx - tx * bz;
			b2[i][2] = tx * by - ty * bx;
		}
		for (int i = 0; i < n - 1; i++) {
			for (int k = 0; k < 4; k++) {
				int color = col.color(i, k);
				if ((color >>> 24) == 0) {
					continue;
				}
				int k2 = (k + 1) & 3;
				bladeVertex(c, w, h, b1, b2, i, k, k, color);
				bladeVertex(c, w, h, b1, b2, i, k2, k, color);
				bladeVertex(c, w, h, b1, b2, i + 1, k2, k, color);
				bladeVertex(c, w, h, b1, b2, i + 1, k, k, color);
			}
		}
	}

	/** Eckpunkt k (0: +breit, 1: +dick, 2: -breit, 3: -dick) der Raute am Pfadpunkt i; Normale der Seitenfläche face. */
	private void bladeVertex(float[] c, float[] w, float[] h, float[][] b1, float[][] b2, int i, int k, int face, int color) {
		float sw = (k == 0 ? 1f : (k == 2 ? -1f : 0f)) * w[i];
		float sh = (k == 1 ? 1f : (k == 3 ? -1f : 0f)) * h[i];
		float x = c[i * 3] + b1[i][0] * sw + b2[i][0] * sh;
		float y = c[i * 3 + 1] + b1[i][1] * sw + b2[i][1] * sh;
		float z = c[i * 3 + 2] + b1[i][2] * sw + b2[i][2] * sh;
		// Seitennormale der Raute: Kante (w,0)->(0,h) hat Normale (h, w), usw.
		float hw = Math.max(h[i], 0.002f);
		float ww = Math.max(w[i], 0.002f);
		float nb1 = (face == 0 || face == 3) ? hw : -hw;
		float nb2 = (face == 0 || face == 1) ? ww : -ww;
		float nx = b1[i][0] * nb1 + b2[i][0] * nb2;
		float ny = b1[i][1] * nb1 + b2[i][1] * nb2;
		float nz = b1[i][2] * nb1 + b2[i][2] * nb2;
		float nl = len(nx, ny, nz);
		if (nl < 1e-6f) {
			nl = 1f;
		}
		sink.vertex(x, y, z, color, nx / nl, ny / nl, nz / nl);
	}

	/** Einzelnes Viereck (12 Werte: 4 Ecken x,y,z) mit einheitlicher Normale und Farbe. */
	public void quad(float[] p, float nx, float ny, float nz, int color) {
		for (int i = 0; i < 4; i++) {
			sink.vertex(p[i * 3], p[i * 3 + 1], p[i * 3 + 2], color, nx, ny, nz);
		}
	}

	/** Kleine Kugel (6-seitige Ringe), z. B. Bommel oder Edelstein. */
	public void blob(float cx, float cy, float cz, float rad, int color) {
		float[] c = {
			cx, cy - rad, cz,
			cx, cy - rad * 0.5f, cz,
			cx, cy, cz,
			cx, cy + rad * 0.5f, cz,
			cx, cy + rad, cz
		};
		float[] r = {0f, rad * 0.87f, rad, rad * 0.87f, 0f};
		loft(c, r, 5, 6, REF_Z, (s, k) -> color, false, false);
	}

	private void cap(float[] c, int i, float[] ring, int k, int sides, int color, float nx, float ny, float nz, boolean reverse) {
		int k1 = (k + 1) % sides;
		int k2 = (k + 2) % sides;
		float cx = c[i * 3];
		float cy = c[i * 3 + 1];
		float cz = c[i * 3 + 2];
		if (!reverse) {
			sink.vertex(cx, cy, cz, color, nx, ny, nz);
			sink.vertex(ring[k * 3], ring[k * 3 + 1], ring[k * 3 + 2], color, nx, ny, nz);
			sink.vertex(ring[k1 * 3], ring[k1 * 3 + 1], ring[k1 * 3 + 2], color, nx, ny, nz);
			sink.vertex(ring[k2 * 3], ring[k2 * 3 + 1], ring[k2 * 3 + 2], color, nx, ny, nz);
		} else {
			sink.vertex(cx, cy, cz, color, nx, ny, nz);
			sink.vertex(ring[k2 * 3], ring[k2 * 3 + 1], ring[k2 * 3 + 2], color, nx, ny, nz);
			sink.vertex(ring[k1 * 3], ring[k1 * 3 + 1], ring[k1 * 3 + 2], color, nx, ny, nz);
			sink.vertex(ring[k * 3], ring[k * 3 + 1], ring[k * 3 + 2], color, nx, ny, nz);
		}
	}

	private void v(float[] p, float[] nrm, int k, int color) {
		sink.vertex(p[k * 3], p[k * 3 + 1], p[k * 3 + 2], color, nrm[k * 3], nrm[k * 3 + 1], nrm[k * 3 + 2]);
	}

	private static float len(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}
}
