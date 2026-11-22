package dev.vallicosmo.render;

import java.util.function.Consumer;

import dev.vallicosmo.Category;
import dev.vallicosmo.Cosmetic;
import dev.vallicosmo.CosmeticState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Zeichnet Flügel, Heiligenschein und Hüte am eigenen Spieler (auch in der Menü-Vorschau).
 * Alles ist prozedurale Geometrie (siehe WingGeometry / HeadGeometry), es werden keine Modelldateien gebraucht.
 */
public class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
	private static final Identifier WHITE = Identifier.of("vallicosmo", "textures/misc/white.png");

	public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
		super(context);
	}

	@Override
	public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float yaw, float pitch) {
		ClientPlayerEntity me = MinecraftClient.getInstance().player;
		if (me == null || state.id != me.getId() || state.invisible) {
			return;
		}
		float t = state.age;
		PlayerEntityModel model = getContextModel();

		Cosmetic wings = CosmeticState.shown(Category.WINGS);
		if (wings != null) {
			WingGeometry.Style st = WingGeometry.styleOf(wings);
			if (st != null) {
				float flap = WingGeometry.flapAngle(t, state.isInSneakingPose, state.isGliding);
				matrices.push();
				model.body.applyTransform(matrices); // folgt Schleichen/Körperneigung
				matrices.translate(0f, 0.22f, 0.14f);
				matrices.scale(1.15f, 1.15f, 1.15f);
				submit(queue, matrices, light, false, m -> WingGeometry.base(m, st, t, flap));
				submit(queue, matrices, light, true, m -> WingGeometry.glow(m, st, t, flap));
				matrices.pop();
			}
		}

		Cosmetic halo = CosmeticState.shown(Category.HALOS);
		if (halo != null) {
			matrices.push();
			model.head.applyTransform(matrices);
			matrices.translate(0f, -0.72f, 0f);
			submit(queue, matrices, light, false, m -> HeadGeometry.haloBase(m, halo, t));
			submit(queue, matrices, light, true, m -> HeadGeometry.haloGlow(m, halo, t));
			matrices.pop();
		}

		Cosmetic hat = CosmeticState.shown(Category.HATS);
		if (hat != null) {
			matrices.push();
			model.head.applyTransform(matrices);
			matrices.translate(0f, -0.46f, 0f);
			submit(queue, matrices, light, false, m -> HeadGeometry.hat(m, hat, t));
			matrices.pop();
		}
	}

	/**
	 * Schickt eine Mesh-Gruppe an die Render-Queue.
	 * additive = Leuchtschicht (RenderLayers.eyes ist additiv und ignoriert das Umgebungslicht);
	 * sonst eine solide, normal beleuchtete Schicht ohne Backface-Culling.
	 * Die Geometrie nutzt y = oben, das Entity-Koordinatensystem y = unten, daher wird y hier gespiegelt.
	 */
	private static void submit(OrderedRenderCommandQueue queue, MatrixStack matrices, int light, boolean additive, Consumer<Mesh> body) {
		RenderLayer layer = additive ? RenderLayers.eyes(WHITE) : RenderLayers.entityCutoutNoCull(WHITE);
		queue.submitCustom(matrices, layer, (entry, vc) -> {
			Mesh mesh = new Mesh((x, y, z, argb, nx, ny, nz) -> {
				int color = additive ? premultiply(argb) : argb;
				vc.vertex(entry, x, -y, z)
					.color(color)
					.texture(0.5f, 0.5f)
					.overlay(OverlayTexture.DEFAULT_UV)
					.light(light)
					.normal(entry, nx, -ny, nz);
			});
			body.accept(mesh);
		});
	}

	/** Additive Schicht: Helligkeit steckt in den RGB-Werten, Alpha bleibt 255 (funktioniert für ONE/ONE und SRC_ALPHA/ONE gleich). */
	private static int premultiply(int argb) {
		float a = ((argb >>> 24) & 0xFF) / 255f;
		int r = (int) (((argb >>> 16) & 0xFF) * a);
		int g = (int) (((argb >>> 8) & 0xFF) * a);
		int b = (int) ((argb & 0xFF) * a);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}
}
