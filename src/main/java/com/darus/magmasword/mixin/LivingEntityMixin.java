package com.darus.magmasword.mixin;

import com.darus.magmasword.MagmaAbilities;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	// The horizontal drag water applies under Dolphin's Grace; vanilla lava uses 0.5.
	@Unique
	private static final double MAGMASWORD_LAVA_DRAG = 0.96;

	@Shadow
	public abstract Vec3 getFluidFallingAdjustedMovement(double baseGravity, boolean isFalling, Vec3 movement);

	@Shadow
	private void jumpOutOfFluid(final double oldY) {
	}

	// Runs on both sides: player movement is simulated by the client and checked by the server.
	@Inject(method = "travelInLava", at = @At("HEAD"), cancellable = true)
	private void magmasword$swimFastInLava(final Vec3 input, final double baseGravity, final boolean isFalling, final double oldY, final CallbackInfo ci) {
		LivingEntity self = (LivingEntity)(Object)this;
		if (!(self instanceof Player) || !MagmaAbilities.isHoldingSword(self)) {
			return;
		}

		self.moveRelative(0.02F, input);
		self.move(MoverType.SELF, self.getDeltaMovement());
		Vec3 movement = self.getDeltaMovement().multiply(MAGMASWORD_LAVA_DRAG, 0.8F, MAGMASWORD_LAVA_DRAG);
		self.setDeltaMovement(this.getFluidFallingAdjustedMovement(baseGravity, isFalling, movement));
		this.jumpOutOfFluid(oldY);
		ci.cancel();
	}
}
