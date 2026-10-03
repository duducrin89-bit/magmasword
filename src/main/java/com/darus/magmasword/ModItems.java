package com.darus.magmasword;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;

public final class ModItems {
	public static final ResourceKey<Item> MAGMA_SWORD_KEY = ResourceKey.create(Registries.ITEM, MagmaSword.id("magma_sword"));

	// Baseline 5 + netherite's bonus 4 + the player's base 1 = 10 attack damage; -2.4 gives 1.6 attack speed.
	public static final Item MAGMA_SWORD = Registry.register(
		BuiltInRegistries.ITEM,
		MAGMA_SWORD_KEY,
		new MagmaSwordItem(
			new Item.Properties()
				.setId(MAGMA_SWORD_KEY)
				.sword(ToolMaterial.NETHERITE, 5.0F, -2.4F)
				.fireResistant()
				.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
		)
	);

	private ModItems() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
			.register(output -> output.insertAfter(Items.NETHERITE_SWORD, MAGMA_SWORD));
	}
}
