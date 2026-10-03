package com.darus.magmasword;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> MAGMA_FIREBALL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, MagmaSword.id("magma_fireball"));

	public static final EntityType<MagmaFireball> MAGMA_FIREBALL = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		MAGMA_FIREBALL_KEY,
		EntityType.Builder.<MagmaFireball>of(MagmaFireball::new, MobCategory.MISC)
			.noLootTable()
			.sized(2.0F, 2.0F)
			.clientTrackingRange(10)
			.updateInterval(2)
			.build(MAGMA_FIREBALL_KEY)
	);

	private ModEntities() {
	}

	public static void initialize() {
	}
}
