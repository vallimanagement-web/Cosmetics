package dev.vallicosmo;

import dev.vallicosmo.gui.CosmeticScreen;
import dev.vallicosmo.render.CosmeticFeatureRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityType;

public class VallicosmoClient implements ClientModInitializer {
	@Override
	@SuppressWarnings("unchecked")
	public void onInitializeClient() {
		CosmeticState.load();

		// Cosmetics an alle Spieler-Renderer hängen (gezeichnet wird nur beim eigenen Spieler).
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityType == EntityType.PLAYER) {
				registrationHelper.register(new CosmeticFeatureRenderer(
					(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) (Object) entityRenderer));
			}
		});

		// /vallicosmo  -> Menü, /vallicosmo clear -> alles ablegen
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommandManager.literal("vallicosmo")
				.executes(ctx -> {
					CosmeticState.openRequested = true;
					return 1;
				})
				.then(ClientCommandManager.literal("clear").executes(ctx -> {
					CosmeticState.clearAll();
					return 1;
				}))));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (CosmeticState.openRequested) {
				CosmeticState.openRequested = false;
				client.setScreen(new CosmeticScreen());
			}
			TrailTicker.tick(client);
		});
	}
}
