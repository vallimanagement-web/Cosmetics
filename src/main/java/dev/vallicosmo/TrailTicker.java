package dev.vallicosmo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

/** Spawnt die Partikel der Spuren (Schnee, Flammen, Funken) am eigenen Spieler. */
public final class TrailTicker {
	private static double lastX;
	private static double lastZ;
	private static boolean hasLast;

	private TrailTicker() {
	}

	public static void tick(MinecraftClient client) {
		ClientPlayerEntity p = client.player;
		if (p == null || client.world == null || client.isPaused()) {
			hasLast = false;
			return;
		}
		double dx = p.getX() - lastX;
		double dz = p.getZ() - lastZ;
		boolean moving = hasLast && dx * dx + dz * dz > 0.0004;
		lastX = p.getX();
		lastZ = p.getZ();
		hasLast = true;

		Cosmetic trail = CosmeticState.equipped(Category.TRAILS);
		if (trail == null || p.isSpectator() || p.isInvisible()) {
			return;
		}
		Random r = p.getRandom();
		switch (trail) {
			case TRAIL_SNOW -> {
				for (int i = 0; i < 2; i++) {
					spawn(client, ParticleTypes.SNOWFLAKE,
						p.getX() + (r.nextDouble() - 0.5) * 1.0,
						p.getY() + 0.3 + r.nextDouble() * 1.7,
						p.getZ() + (r.nextDouble() - 0.5) * 1.0,
						(r.nextDouble() - 0.5) * 0.02, -0.02, (r.nextDouble() - 0.5) * 0.02);
				}
			}
			case TRAIL_FLAME -> {
				if (moving) {
					spawn(client, ParticleTypes.FLAME,
						p.getX() + (r.nextDouble() - 0.5) * 0.4, p.getY() + 0.1, p.getZ() + (r.nextDouble() - 0.5) * 0.4,
						0.0, 0.02, 0.0);
				}
			}
			case TRAIL_SPARK -> {
				if (moving) {
					spawn(client, ParticleTypes.ELECTRIC_SPARK,
						p.getX() + (r.nextDouble() - 0.5) * 0.5, p.getY() + 0.1 + r.nextDouble() * 0.3, p.getZ() + (r.nextDouble() - 0.5) * 0.5,
						(r.nextDouble() - 0.5) * 0.2, (r.nextDouble() - 0.5) * 0.2, (r.nextDouble() - 0.5) * 0.2);
				}
			}
			default -> {
			}
		}
	}

	private static void spawn(MinecraftClient client, ParticleEffect type, double x, double y, double z, double vx, double vy, double vz) {
		client.particleManager.addParticle(type, x, y, z, vx, vy, vz);
	}
}
