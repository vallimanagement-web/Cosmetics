package dev.vallicosmo.gui;

import java.util.List;

import dev.vallicosmo.Category;
import dev.vallicosmo.Cosmetic;
import dev.vallicosmo.CosmeticState;
import dev.vallicosmo.util.Colors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/**
 * Das Cosmetics-Menü. Komplett selbst gezeichnet (kein Chest/Handler):
 * animierter Hintergrund, Kategorie-Tabs links, Live-Vorschau in der Mitte, Karten rechts.
 * Überfahren einer Karte zeigt das Cosmetic sofort am Spieler, Klick rüstet aus/ab.
 */
public class CosmeticScreen extends Screen {
	private static final int BG_TOP = 0xF005060F;
	private static final int BG_BOTTOM = 0xF0220B38;
	private static final int PANEL = 0xE60C0F1E;
	private static final int PANEL_LIGHT = 0xE6151A30;
	private static final int ACCENT = 0xFF7C5CFF;
	private static final int TEXT = 0xFFEAF0FF;
	private static final int TEXT_DIM = 0xFF8B94B5;
	private static final int GREEN = 0xFF3DDC84;

	private static final int CARD_H = 42;
	private static final int GAP = 6;
	/** Platz unter der Kartenliste für den Farbwähler (nur im Tab Heiligenscheine). */
	private static final int PICKER_H = 50;
	private static final int[] HALO_PRESETS = {
		0xFFFFFF, 0xFFC83C, 0xFF4B4B, 0xFF8A1F, 0x3DDC84, 0x33D6FF, 0x4D6BFF, 0x9B5CFF, 0xFF5CC8,
	};
	private static float lastHue = -1f;

	private Category tab = Category.WINGS;
	private Cosmetic hovered;
	/** Index der ersten sichtbaren Karte. */
	private int scroll;

	// Layout (wird in init() berechnet)
	private int px;
	private int py;
	private int pw;
	private int ph;
	private int tabX;
	private int tabW;
	private int prevX;
	private int prevW;
	private int listX;
	private int listW;
	private int bodyY;
	private int bodyH;

	public CosmeticScreen() {
		super(Text.literal("Vallicosmo"));
	}

	@Override
	protected void init() {
		pw = Math.min(width - 24, 600);
		ph = Math.min(height - 24, 320);
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		bodyY = py + 46;
		bodyH = ph - 46 - 58;
		int pad = 12;
		tabX = px + pad;
		tabW = Math.max(80, (int) (pw * 0.2f));
		prevX = tabX + tabW + pad;
		prevW = Math.max(110, (int) (pw * 0.3f));
		listX = prevX + prevW + pad;
		listW = px + pw - pad - listX;
	}

	@Override
	public boolean shouldPause() {
		return false; // sonst friert in Singleplayer die Flügel-Animation ein
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		// Eigener Hintergrund in render(), kein Vanilla-Blur.
	}

	@Override
	public void removed() {
		CosmeticState.setPreview(null);
	}

	private static float time() {
		return (System.nanoTime() / 1_000_000L) / 1000f;
	}

	// ---- Scrollen: es werden immer nur so viele Karten gezeigt, wie ganz hineinpassen

	private int viewH() {
		return bodyH - 22 - (tab == Category.HALOS ? PICKER_H : 0);
	}

	private int rows() {
		return Math.max(1, (viewH() + GAP) / (CARD_H + GAP));
	}

	private int maxScroll() {
		return Math.max(0, Cosmetic.of(tab).size() - rows());
	}

	private void clampScroll() {
		scroll = Math.max(0, Math.min(scroll, maxScroll()));
	}

	private boolean scrollbarVisible() {
		return maxScroll() > 0;
	}

	private boolean visible(int i) {
		return i >= scroll && i < scroll + rows();
	}

	private int[] cardRect(int i) {
		int w = listW - (scrollbarVisible() ? 10 : 0);
		return new int[] {listX, bodyY + 22 + (i - scroll) * (CARD_H + GAP), w, CARD_H};
	}

	private int[] trackRect() {
		return new int[] {listX + listW - 6, bodyY + 22, 6, rows() * (CARD_H + GAP) - GAP};
	}

	private int[] upRect() {
		return new int[] {listX + listW - 30, bodyY + 3, 13, 12};
	}

	private int[] downRect() {
		return new int[] {listX + listW - 15, bodyY + 3, 13, 12};
	}

	// ---- Farbwähler (Tab Heiligenscheine)

	private int pickerY() {
		return bodyY + bodyH - PICKER_H + 2;
	}

	private int[] swatchRect(int i) {
		int n = HALO_PRESETS.length + 1;
		int gap = 3;
		int w = (listW - gap * (n - 1)) / n;
		return new int[] {listX + i * (w + gap), pickerY() + 11, w, 14};
	}

	private int[] hueRect() {
		return new int[] {listX, pickerY() + 30, listW, 10};
	}

	private int[] tabRect(int i) {
		int h = 30;
		return new int[] {tabX, bodyY + 22 + i * (h + 6), tabW, h};
	}

	private static boolean in(double mx, double my, int[] r) {
		return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		float t = time();
		drawBackdrop(ctx, t);
		drawPanel(ctx, t);

		// Hover bestimmen -> Live-Vorschau am Spieler
		hovered = null;
		List<Cosmetic> items = Cosmetic.of(tab);
		clampScroll();
		for (int i = 0; i < items.size(); i++) {
			if (visible(i) && in(mouseX, mouseY, cardRect(i))) {
				hovered = items.get(i);
			}
		}
		CosmeticState.setPreview(hovered);

		drawHeader(ctx, t);
		drawTabs(ctx, mouseX, mouseY, t);
		drawPreview(ctx, mouseX, mouseY, t);
		drawCards(ctx, items, mouseX, mouseY, t);
		if (tab == Category.HALOS) {
			drawPicker(ctx, mouseX, mouseY, t);
		}
		drawFooter(ctx, mouseX, mouseY);
		super.render(ctx, mouseX, mouseY, delta);
	}

	// ---------------------------------------------------------------- Zeichnen

	private void drawBackdrop(DrawContext ctx, float t) {
		ctx.fillGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);
		// Fallender Schnee / Sternenstaub
		for (int i = 0; i < 70; i++) {
			float seed = i * 12.9898f;
			float sx = frac((float) Math.sin(seed) * 43758.545f);
			float sy = frac((float) Math.sin(seed * 1.7f) * 24634.634f);
			float speed = 0.02f + 0.05f * frac(seed * 0.37f);
			float x = frac(sx + (float) Math.sin(t * 0.3f + i) * 0.01f) * width;
			float y = frac(sy + t * speed) * height;
			int size = (i % 5 == 0) ? 2 : 1;
			int a = 60 + (int) (80 * frac(seed * 0.91f));
			ctx.fill((int) x, (int) y, (int) x + size, (int) y + size, Colors.alpha(0xFFFFFF, a / 255f));
		}
	}

	private void drawPanel(DrawContext ctx, float t) {
		// Weicher Rand-Schein
		int pulse = 40 + (int) (20 * Math.sin(t * 1.5f));
		for (int i = 4; i >= 1; i--) {
			ctx.fill(px - i, py - i, px + pw + i, py + ph + i, Colors.alpha(ACCENT, (pulse / 255f) / (i + 1)));
		}
		ctx.fill(px, py, px + pw, py + ph, PANEL);
		ctx.fillGradient(px, py, px + pw, py + 40, 0x30FFFFFF, 0x00FFFFFF);
		ctx.drawStrokedRectangle(px, py, pw, ph, Colors.alpha(ACCENT, 0.9f));
	}

	private void drawHeader(DrawContext ctx, float t) {
		// Regenbogen-Titel mit Wellen-Animation
		String title = "VALLICOSMO";
		int x = px + 14;
		for (int i = 0; i < title.length(); i++) {
			String ch = String.valueOf(title.charAt(i));
			int yOff = (int) Math.round(Math.sin(t * 3f + i * 0.6f) * 1.5);
			int col = Colors.hsv(t * 0.1f + i * 0.06f, 0.55f, 1f);
			ctx.drawText(textRenderer, ch, x, py + 10 + yOff, col, true);
			x += textRenderer.getWidth(ch) + 1;
		}
		ctx.drawText(textRenderer, "Cosmetics  |  " + CosmeticState.equippedCount() + " ausgerüstet", px + 14, py + 24, TEXT_DIM, false);
		ctx.drawText(textRenderer, "[ESC] schließen", px + pw - 14 - textRenderer.getWidth("[ESC] schließen"), py + 14, TEXT_DIM, false);
		ctx.fill(px + 12, py + 40, px + pw - 12, py + 41, Colors.alpha(ACCENT, 0.5f));
	}

	private void drawTabs(DrawContext ctx, int mouseX, int mouseY, float t) {
		ctx.drawText(textRenderer, "KATEGORIE", tabX, bodyY + 6, TEXT_DIM, false);
		Category[] cats = Category.values();
		for (int i = 0; i < cats.length; i++) {
			int[] r = tabRect(i);
			boolean sel = cats[i] == tab;
			boolean hov = in(mouseX, mouseY, r);
			int bg = sel ? Colors.alpha(ACCENT, 0.35f) : (hov ? 0x40FFFFFF : PANEL_LIGHT);
			ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
			if (sel) {
				ctx.fill(r[0], r[1], r[0] + 3, r[1] + r[3], ACCENT);
			}
			Cosmetic eq = CosmeticState.equipped(cats[i]);
			ctx.drawText(textRenderer, cats[i].label, r[0] + 10, r[1] + 6, sel ? TEXT : 0xFFC4CBE6, false);
			String sub = eq == null ? "leer" : eq.title;
			if (textRenderer.getWidth(sub) > r[2] - 14) {
				sub = textRenderer.trimToWidth(sub, r[2] - 20) + "..";
			}
			ctx.drawText(textRenderer, sub, r[0] + 10, r[1] + 17, eq == null ? 0xFF5E668A : GREEN, false);
		}
	}

	private void drawPreview(DrawContext ctx, int mouseX, int mouseY, float t) {
		int y2 = bodyY + bodyH;
		ctx.fill(prevX, bodyY, prevX + prevW, y2, PANEL_LIGHT);
		ctx.drawStrokedRectangle(prevX, bodyY, prevW, bodyH, Colors.alpha(ACCENT, 0.35f));
		ctx.drawText(textRenderer, "VORSCHAU", prevX + 6, bodyY + 6, TEXT_DIM, false);

		if (client != null && client.player != null) {
			int boxX1 = prevX + 4;
			int boxY1 = bodyY + 18;
			int boxX2 = prevX + prevW - 4;
			int boxY2 = y2 - 4;
			int size = (int) Math.min((boxY2 - boxY1) / 2.7f, (boxX2 - boxX1) / 3.0f);
			InventoryScreen.drawEntity(ctx, boxX1, boxY1, boxX2, boxY2, size, 0.0625f, mouseX, mouseY, client.player);
		}
		String hint = "Maus bewegen = Drehen";
		ctx.drawText(textRenderer, hint, prevX + (prevW - textRenderer.getWidth(hint)) / 2, y2 - 12, 0xFF5E668A, false);
	}

	private void drawCards(DrawContext ctx, List<Cosmetic> items, int mouseX, int mouseY, float t) {
		ctx.drawText(textRenderer, tab.label.toUpperCase(java.util.Locale.ROOT), listX, bodyY + 6, TEXT_DIM, false);
		if (scrollbarVisible()) {
			int last = Math.min(items.size(), scroll + rows());
			String count = (scroll + 1) + "-" + last + " / " + items.size();
			ctx.drawText(textRenderer, count, upRect()[0] - 6 - textRenderer.getWidth(count), bodyY + 6, TEXT_DIM, false);
			button(ctx, upRect(), "^", mouseX, mouseY, scroll > 0 ? ACCENT : 0xFF444A66);
			button(ctx, downRect(), "v", mouseX, mouseY, scroll < maxScroll() ? ACCENT : 0xFF444A66);
			int[] tr = trackRect();
			ctx.fill(tr[0], tr[1], tr[0] + tr[2], tr[1] + tr[3], 0x40FFFFFF);
			int thumbH = Math.max(16, tr[3] * rows() / items.size());
			int thumbY = tr[1] + (tr[3] - thumbH) * scroll / Math.max(1, maxScroll());
			ctx.fill(tr[0], thumbY, tr[0] + tr[2], thumbY + thumbH, ACCENT);
		}
		for (int i = 0; i < items.size(); i++) {
			if (!visible(i)) {
				continue;
			}
			Cosmetic c = items.get(i);
			int[] r = cardRect(i);
			boolean hov = c == hovered;
			boolean eq = CosmeticState.isEquipped(c);

			int bg = hov ? 0xF0242B4A : PANEL_LIGHT;
			ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
			// Seltenheits-Streifen
			ctx.fill(r[0], r[1], r[0] + 4, r[1] + r[3], c.rarity.color);
			// Legendäre Karten bekommen einen wandernden Glanz
			if (c.rarity == dev.vallicosmo.Rarity.LEGENDARY) {
				int sx = r[0] + (int) (frac(t * 0.25f + i * 0.3f) * (r[2] + 30)) - 30;
				int x1 = Math.max(sx, r[0] + 4);
				int x2 = Math.min(sx + 16, r[0] + r[2]);
				if (x2 > x1) {
					ctx.fill(x1, r[1], x2, r[1] + r[3], 0x18FFFFFF);
				}
			}
			ctx.drawStrokedRectangle(r[0], r[1], r[2], r[3],
				eq ? GREEN : (hov ? Colors.alpha(c.rarity.color, 0.9f) : Colors.alpha(c.rarity.color, 0.25f)));

			String title = c.title;
			int maxW = r[2] - 20;
			if (textRenderer.getWidth(title) > maxW) {
				title = textRenderer.trimToWidth(title, maxW - 6) + "..";
			}
			boolean small = r[3] < 40;
			ctx.drawText(textRenderer, title, r[0] + 12, r[1] + (small ? 4 : 8), TEXT, true);
			ctx.drawText(textRenderer, c.rarity.label, r[0] + 12, r[1] + (small ? 16 : 22), c.rarity.color, false);

			int chipX = r[0] + r[2] - 8;
			if (eq) {
				String s = "AUSGERÜSTET";
				int w = textRenderer.getWidth(s) + 8;
				chipX -= w;
				ctx.fill(chipX, r[1] + (small ? 3 : 6), chipX + w, r[1] + (small ? 14 : 17), Colors.alpha(GREEN, 0.25f));
				ctx.drawText(textRenderer, s, chipX + 4, r[1] + (small ? 5 : 8), GREEN, false);
				chipX -= 4;
			}
			if (c.christmas) {
				String s = "XMAS";
				int w = textRenderer.getWidth(s) + 8;
				chipX -= w;
				ctx.fill(chipX, r[1] + (small ? 3 : 6), chipX + w, r[1] + (small ? 14 : 17), 0xFFD0202E);
				ctx.drawText(textRenderer, s, chipX + 4, r[1] + (small ? 5 : 8), 0xFFFFFFFF, false);
			}
		}
	}

	private void drawPicker(DrawContext ctx, int mouseX, int mouseY, float t) {
		int y0 = pickerY();
		ctx.fill(listX, y0 - 3, listX + listW, y0 - 2, Colors.alpha(ACCENT, 0.4f));
		ctx.drawText(textRenderer, "HALO-FARBE (Farb-Heiligenschein)", listX, y0, TEXT_DIM, false);
		boolean custom = CosmeticState.isEquipped(Cosmetic.HALO_CUSTOM);
		for (int i = 0; i <= HALO_PRESETS.length; i++) {
			int[] r = swatchRect(i);
			boolean rainbow = i == HALO_PRESETS.length;
			if (rainbow) {
				int slices = 6;
				for (int k = 0; k < slices; k++) {
					int x1 = r[0] + r[2] * k / slices;
					int x2 = r[0] + r[2] * (k + 1) / slices;
					ctx.fill(x1, r[1], x2, r[1] + r[3], Colors.hsv(t * 0.2f + k / (float) slices, 0.9f, 1f));
				}
			} else {
				ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0xFF000000 | HALO_PRESETS[i]);
			}
			boolean selected = custom && (rainbow ? CosmeticState.haloRainbow()
				: (!CosmeticState.haloRainbow() && lastHue < 0f && (CosmeticState.haloRgb() & 0xFFFFFF) == HALO_PRESETS[i]));
			boolean hov = in(mouseX, mouseY, r);
			ctx.drawStrokedRectangle(r[0], r[1], r[2], r[3], selected ? 0xFFFFFFFF : (hov ? 0xFFB6BEDB : 0x55000000));
			if (selected) {
				ctx.drawStrokedRectangle(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, GREEN);
			}
		}
		int[] hr = hueRect();
		int slices = Math.max(8, hr[2] / 3);
		for (int k = 0; k < slices; k++) {
			int x1 = hr[0] + hr[2] * k / slices;
			int x2 = hr[0] + hr[2] * (k + 1) / slices;
			ctx.fill(x1, hr[1], x2, hr[1] + hr[3], Colors.hsv(k / (float) slices, 0.9f, 1f));
		}
		ctx.drawStrokedRectangle(hr[0], hr[1], hr[2], hr[3], 0x55000000);
		if (custom && !CosmeticState.haloRainbow() && lastHue >= 0f) {
			int mx = hr[0] + (int) (lastHue * (hr[2] - 1));
			ctx.fill(mx - 1, hr[1] - 2, mx + 2, hr[1] + hr[3] + 2, 0xFFFFFFFF);
		}
	}

	private void drawFooter(DrawContext ctx, int mouseX, int mouseY) {
		int fy = py + ph - 52;
		ctx.fill(px + 12, fy, px + pw - 12, fy + 1, Colors.alpha(ACCENT, 0.4f));
		// Beschreibung (überfahrenes oder ausgerüstetes Cosmetic)
		Cosmetic shown = hovered != null ? hovered : CosmeticState.equipped(tab);
		int textW = pw - 24 - 130;
		if (shown != null) {
			ctx.drawText(textRenderer, shown.title, px + 14, fy + 7, shown.rarity.color, true);
			List<OrderedText> lines = textRenderer.wrapLines(Text.literal(shown.description), textW);
			for (int i = 0; i < Math.min(lines.size(), 3); i++) {
				ctx.drawText(textRenderer, lines.get(i), px + 14, fy + 19 + i * 10, 0xFFB6BEDB, false);
			}
		} else {
			ctx.drawText(textRenderer, "Fahr über ein Cosmetic für eine Vorschau, klick zum Ausrüsten.", px + 14, fy + 12, TEXT_DIM, false);
		}
		// Buttons
		int[] clear = clearRect();
		int[] close = closeRect();
		button(ctx, clear, "Alles ablegen", mouseX, mouseY, 0xFFE0566B);
		button(ctx, close, "Schließen", mouseX, mouseY, ACCENT);
	}

	private int[] clearRect() {
		return new int[] {px + pw - 12 - 112, py + ph - 46, 112, 16};
	}

	private int[] closeRect() {
		return new int[] {px + pw - 12 - 112, py + ph - 26, 112, 16};
	}

	private void button(DrawContext ctx, int[] r, String label, int mx, int my, int color) {
		boolean hov = in(mx, my, r);
		ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], Colors.alpha(color, hov ? 0.55f : 0.25f));
		ctx.drawStrokedRectangle(r[0], r[1], r[2], r[3], Colors.alpha(color, hov ? 1f : 0.6f));
		ctx.drawCenteredTextWithShadow(textRenderer, label, r[0] + r[2] / 2, r[1] + 4, TEXT);
	}

	// ---------------------------------------------------------------- Eingabe

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0) {
			double mx = click.x();
			double my = click.y();
			Category[] cats = Category.values();
			for (int i = 0; i < cats.length; i++) {
				if (in(mx, my, tabRect(i))) {
					tab = cats[i];
					scroll = 0;
					hovered = null;
					CosmeticState.setPreview(null);
					click();
					return true;
				}
			}
			List<Cosmetic> items = Cosmetic.of(tab);
			for (int i = 0; i < items.size(); i++) {
				if (visible(i) && in(mx, my, cardRect(i))) {
					CosmeticState.toggle(items.get(i));
					click();
					return true;
				}
			}
			if (scrollbarVisible()) {
				if (in(mx, my, upRect())) {
					scroll--;
					clampScroll();
					return true;
				}
				if (in(mx, my, downRect())) {
					scroll++;
					clampScroll();
					return true;
				}
				int[] tr = trackRect();
				if (mx >= tr[0] - 3 && mx < tr[0] + tr[2] + 3 && my >= tr[1] && my < tr[1] + tr[3]) {
					scroll = Math.round(maxScroll() * (float) ((my - tr[1]) / tr[3]));
					clampScroll();
					return true;
				}
			}
			if (tab == Category.HALOS) {
				for (int i = 0; i <= HALO_PRESETS.length; i++) {
					if (in(mx, my, swatchRect(i))) {
						if (i == HALO_PRESETS.length) {
							CosmeticState.setHaloRainbow();
						} else {
							lastHue = -1f;
							CosmeticState.setHaloColor(HALO_PRESETS[i]);
						}
						click();
						return true;
					}
				}
				int[] hr = hueRect();
				if (in(mx, my, hr)) {
					lastHue = Math.max(0f, Math.min(1f, (float) ((mx - hr[0]) / hr[2])));
					CosmeticState.setHaloColor(Colors.hsv(lastHue, 0.9f, 1f));
					click();
					return true;
				}
			}
			if (in(mx, my, clearRect())) {
				CosmeticState.clearAll();
				click();
				return true;
			}
			if (in(mx, my, closeRect())) {
				click();
				close();
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	/** Mausrad: eine Karte pro Raste (ohne @Override: sollte sich die Signatur ändern, bleiben Pfeile und Scrollbalken). */
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount != 0 && scrollbarVisible()) {
			scroll -= (int) Math.signum(verticalAmount);
			clampScroll();
			return true;
		}
		return false;
	}

	/** Platzhalter für einen Klick-Sound (die Sound-API hat sich in 1.21.11 geändert, daher aktuell ohne Ton). */
	private void click() {
	}

	private static float frac(float v) {
		return v - (float) Math.floor(v);
	}
}
