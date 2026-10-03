package com.darus.magmasword;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MagmaSwordItem extends Item {
	private static final float IGNITE_SECONDS = 5.0F;

	public MagmaSwordItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			return MagmaAbilities.tryLaunch(serverPlayer) ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
		}

		// The client cannot see the cooldown, so it only checks where the launch may start from.
		return MagmaAbilities.canLaunchFrom(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
	}

	@Override
	public void hurtEnemy(final ItemStack itemStack, final LivingEntity mob, final LivingEntity attacker) {
		mob.igniteForSeconds(IGNITE_SECONDS);
	}
}
