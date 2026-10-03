package com.darus.magmasword.mixin;

import com.darus.magmasword.MagmaAbilities;

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	// Injected after the hand-off to the server thread, so this never runs on the network thread.
	@Inject(
		method = "handlePlayerAction",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER),
		cancellable = true
	)
	private void magmasword$fireballInsteadOfSwap(final ServerboundPlayerActionPacket packet, final CallbackInfo ci) {
		if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND
			|| this.player.isSpectator()
			|| !MagmaAbilities.isHoldingSword(this.player)) {
			return;
		}

		// The swap is cancelled even on cooldown, so the sword always stays in the main hand.
		MagmaAbilities.tryFireball(this.player);
		ci.cancel();
	}
}
