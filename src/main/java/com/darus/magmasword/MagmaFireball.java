package com.darus.magmasword;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MagmaFireball extends Fireball {
	// A ghast fireball accelerates by 0.1 per tick; this one is three times as fast.
	private static final double ACCELERATION_POWER = 0.3;
	private static final double INITIAL_SPEED = 1.5;
	private static final double MAX_RANGE = 100.0;
	private static final double BLAST_RADIUS = 10.0;
	private static final float BLAST_DAMAGE = 12.0F;
	private static final double BLAST_KNOCKBACK = 1.2;

	private double distanceTravelled;

	public MagmaFireball(final EntityType<? extends MagmaFireball> type, final Level level) {
		super(type, level);
		this.accelerationPower = ACCELERATION_POWER;
	}

	public MagmaFireball(final Level level, final LivingEntity owner, final Vec3 direction) {
		super(ModEntities.MAGMA_FIREBALL, owner, direction, level);
		this.accelerationPower = ACCELERATION_POWER;
		Vec3 aim = direction.normalize();
		// Start in front of the owner's eyes, with the hitbox centred on the line of sight.
		Vec3 start = owner.getEyePosition().add(aim.scale(1.5));
		this.setPos(start.x, start.y - this.getBbHeight() / 2.0, start.z);
		this.setDeltaMovement(aim.scale(INITIAL_SPEED));
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.isAlive()) {
			this.distanceTravelled += this.getDeltaMovement().length();
			if (this.distanceTravelled > MAX_RANGE) {
				this.discard();
			}
		}
	}

	@Override
	protected boolean canHitEntity(final Entity entity) {
		return entity != this.getOwner() && super.canHitEntity(entity);
	}

	@Override
	protected void onHit(final HitResult hitResult) {
		super.onHit(hitResult);
		if (this.level() instanceof ServerLevel serverLevel) {
			this.explode(serverLevel, hitResult.getLocation());
			this.discard();
		}
	}

	// Damages and knocks back everything in the sphere except the owner; no blocks are broken.
	private void explode(final ServerLevel level, final Vec3 center) {
		Entity owner = this.getOwner();
		DamageSource damageSource = this.damageSources().explosion(this, owner);
		AABB area = AABB.ofSize(center, BLAST_RADIUS * 2.0, BLAST_RADIUS * 2.0, BLAST_RADIUS * 2.0);

		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != owner && e.isAlive() && !e.isSpectator())) {
			Vec3 offset = target.getBoundingBox().getCenter().subtract(center);
			if (offset.lengthSqr() > BLAST_RADIUS * BLAST_RADIUS) {
				continue;
			}

			if (target.hurtServer(level, damageSource, BLAST_DAMAGE)) {
				Vec3 push = offset.horizontalDistanceSqr() < 1.0E-4 ? Vec3.ZERO : new Vec3(offset.x, 0.0, offset.z).normalize().scale(BLAST_KNOCKBACK);
				target.push(push.x, 0.5, push.z);
				target.hurtMarked = true;
			}
		}

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 12, 3.5, 3.5, 3.5, 0.0);
		level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 120, 4.0, 4.0, 4.0, 0.0);
		level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 250, 4.0, 4.0, 4.0, 0.15);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 8.0F, 0.6F);
	}

	@Override
	protected ParticleOptions getTrailParticle() {
		return ParticleTypes.FLAME;
	}
}
