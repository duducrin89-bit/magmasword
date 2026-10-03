package com.darus.magmasword.client;

import com.darus.magmasword.ModEntities;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class MagmaSwordClient implements ClientModInitializer {
	// A ghast fireball renders at scale 3.
	private static final float FIREBALL_SCALE = 6.0F;

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.MAGMA_FIREBALL, context -> new ThrownItemRenderer<>(context, FIREBALL_SCALE, true));
	}
}
