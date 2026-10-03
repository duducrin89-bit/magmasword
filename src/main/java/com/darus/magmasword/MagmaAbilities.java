package com.darus.magmasword;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side logic for the Magma Sword: cooldowns, the launch and smash, the fireball, and the held passives.
 */
public final class MagmaAbilities {
	private static final int LAUNCH_COOLDOWN_TICKS = 45 * 20;
	private static final int FIREBALL_COOLDOWN_TICKS = 90 * 20;

	private static final double LAUNCH_SPEED = 2.0;
	private static final double LAUNCH_MIN_LIFT = 0.5;
	// How long a launch may stay on the ground or in liquid before it counts as never having taken off.
	private static final int LAUNCH_TAKEOFF_TICKS = 40;

	private static final double SMASH_RADIUS = 5.0;
	private static final double SMASH_MIN_DAMAGE = 4.0;
	private static final double SMASH_MAX_DAMAGE = 20.0;
	private static final double SMASH_HEAVY_FALL = 5.0;

	private static final int FIREBALL_EFFECT_TICKS = 10 * 20;

	/** Game times at which each ability is ready again; 0 means ready with no message pending. */
	public record Cooldowns(long launchReadyAt, long fireballReadyAt) {
		public static final Cooldowns NONE = new Cooldowns(0L, 0L);
		public static final Codec<Cooldowns> CODEC = RecordCodecBuilder.create(
			i -> i.group(
					Codec.LONG.fieldOf("launch_ready_at").forGetter(Cooldowns::launchReadyAt),
					Codec.LONG.fieldOf("fireball_ready_at").forGetter(Cooldowns::fireballReadyAt)
				)
				.apply(i, Cooldowns::new)
		);
	}

	public static final AttachmentType<Cooldowns> COOLDOWNS = AttachmentRegistry.<Cooldowns>create(
		MagmaSword.id("cooldowns"), builder -> builder.persistent(Cooldowns.CODEC).copyOnDeath()
	);

	private static final class LaunchState {
		private boolean airborne;
		private double maxY;
		private int ticks;

		private LaunchState(final double startY) {
			this.maxY = startY;
		}
	}

	// Players between a launch and its landing. Not persisted: a relog simply ends the launch.
	private static final Map<UUID, LaunchState> LAUNCHES = new HashMap<>();

	private MagmaAbilities() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(MagmaAbilities::tick);
		ServerPlayerEvents.JOIN.register(MagmaAbilities::clearExpiredCooldowns);
		ServerPlayerEvents.LEAVE.register(player -> LAUNCHES.remove(player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAUNCHES.clear());
		// A launched player takes no fall damage from the landing that ends the launch.
		ServerLivingEntityEvents.ALLOW_DAMAGE
			.register((entity, source, amount) -> !(source.is(DamageTypeTags.IS_FALL) && LAUNCHES.containsKey(entity.getUUID())));
	}

	public static boolean isHoldingSword(final LivingEntity entity) {
		return entity.getMainHandItem().is(ModItems.MAGMA_SWORD);
	}

	public static boolean canLaunchFrom(final Player player) {
		return player.onGround() || player.isInLava();
	}

	public static boolean tryLaunch(final ServerPlayer player) {
		if (!canLaunchFrom(player)) {
			return false;
		}

		long now = gameTime(player);
		Cooldowns cooldowns = player.getAttachedOrElse(COOLDOWNS, Cooldowns.NONE);
		if (now < cooldowns.launchReadyAt()) {
			return false;
		}

		Vec3 velocity = player.getLookAngle().scale(LAUNCH_SPEED);
		if (player.onGround() && velocity.y < LAUNCH_MIN_LIFT) {
			// Without some lift, ground friction would stop a level launch immediately.
			velocity = new Vec3(velocity.x, LAUNCH_MIN_LIFT, velocity.z);
		}

		player.setDeltaMovement(velocity);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);

		LAUNCHES.put(player.getUUID(), new LaunchState(player.getY()));
		player.setAttached(COOLDOWNS, new Cooldowns(now + LAUNCH_COOLDOWN_TICKS, cooldowns.fireballReadyAt()));
		return true;
	}

	public static boolean tryFireball(final ServerPlayer player) {
		long now = gameTime(player);
		Cooldowns cooldowns = player.getAttachedOrElse(COOLDOWNS, Cooldowns.NONE);
		if (now < cooldowns.fireballReadyAt()) {
			return false;
		}

		ServerLevel level = player.level();
		level.addFreshEntity(new MagmaFireball(level, player, player.getLookAngle()));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.5F, 0.7F);

		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FIREBALL_EFFECT_TICKS, 1));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, FIREBALL_EFFECT_TICKS, 3));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, FIREBALL_EFFECT_TICKS, 2));

		player.setAttached(COOLDOWNS, new Cooldowns(cooldowns.launchReadyAt(), now + FIREBALL_COOLDOWN_TICKS));
		return true;
	}

	/** Makes both abilities ready immediately, without the usual ready messages. */
	public static void resetCooldowns(final ServerPlayer player) {
		player.setAttached(COOLDOWNS, Cooldowns.NONE);
	}

	private static void tick(final MinecraftServer server) {
		long now = server.overworld().getGameTime();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickCooldowns(player, now);
			tickLaunch(player);
			if (isHoldingSword(player)) {
				tickHeld(player, now);
			}
		}
	}

	private static void tickCooldowns(final ServerPlayer player, final long now) {
		Cooldowns cooldowns = player.getAttachedOrElse(COOLDOWNS, Cooldowns.NONE);
		boolean launchReady = cooldowns.launchReadyAt() != 0L && now >= cooldowns.launchReadyAt();
		boolean fireballReady = cooldowns.fireballReadyAt() != 0L && now >= cooldowns.fireballReadyAt();
		if (!launchReady && !fireballReady) {
			return;
		}

		if (launchReady) {
			player.sendSystemMessage(readyMessage("Launch"));
		}

		if (fireballReady) {
			player.sendSystemMessage(readyMessage("Fireball"));
		}

		player.setAttached(COOLDOWNS, new Cooldowns(launchReady ? 0L : cooldowns.launchReadyAt(), fireballReady ? 0L : cooldowns.fireballReadyAt()));
	}

	// Cooldowns that ran out while the player was offline are cleared without a ready message.
	private static void clearExpiredCooldowns(final ServerPlayer player) {
		long now = gameTime(player);
		Cooldowns cooldowns = player.getAttachedOrElse(COOLDOWNS, Cooldowns.NONE);
		player.setAttached(
			COOLDOWNS,
			new Cooldowns(now >= cooldowns.launchReadyAt() ? 0L : cooldowns.launchReadyAt(), now >= cooldowns.fireballReadyAt() ? 0L : cooldowns.fireballReadyAt())
		);
	}

	private static void tickLaunch(final ServerPlayer player) {
		LaunchState state = LAUNCHES.get(player.getUUID());
		if (state == null) {
			return;
		}

		if (!player.isAlive()) {
			LAUNCHES.remove(player.getUUID());
			return;
		}

		state.ticks++;
		boolean inLiquid = player.isInWater() || player.isInLava();
		if (!state.airborne) {
			if (!player.onGround() && !inLiquid) {
				state.airborne = true;
				state.maxY = Math.max(state.maxY, player.getY());
			} else if (state.ticks > LAUNCH_TAKEOFF_TICKS) {
				LAUNCHES.remove(player.getUUID());
			}

			return;
		}

		state.maxY = Math.max(state.maxY, player.getY());
		if (inLiquid) {
			LAUNCHES.remove(player.getUUID());
		} else if (player.onGround()) {
			LAUNCHES.remove(player.getUUID());
			smash(player, state.maxY - player.getY());
		}
	}

	// Mace-style landing: damage grows with the fall, from 4 up to 20, and nearby entities are knocked away.
	private static void smash(final ServerPlayer player, final double fallDistance) {
		ServerLevel level = player.level();
		float damage = (float)Mth.clamp(SMASH_MIN_DAMAGE + fallDistance, SMASH_MIN_DAMAGE, SMASH_MAX_DAMAGE);
		boolean heavy = fallDistance > SMASH_HEAVY_FALL;

		level.levelEvent(2013, player.getOnPos(), 750);
		level.playSound(
			null,
			player.getX(),
			player.getY(),
			player.getZ(),
			heavy ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND,
			SoundSource.PLAYERS,
			1.0F,
			1.0F
		);

		DamageSource damageSource = player.damageSources().mace(player);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SMASH_RADIUS), e -> canSmash(player, e))) {
			if (target.distanceToSqr(player) > SMASH_RADIUS * SMASH_RADIUS) {
				continue;
			}

			if (target.hurtServer(level, damageSource, damage)) {
				Vec3 away = target.position().subtract(player.position());
				Vec3 push = away.horizontalDistanceSqr() < 1.0E-4 ? Vec3.ZERO : new Vec3(away.x, 0.0, away.z).normalize().scale(heavy ? 1.0 : 0.6);
				target.push(push.x, 0.5, push.z);
				target.hurtMarked = true;
			}
		}
	}

	private static boolean canSmash(final ServerPlayer player, final LivingEntity target) {
		return target != player
			&& target.isAlive()
			&& !target.isSpectator()
			&& !player.isAlliedTo(target)
			&& !(target instanceof TamableAnimal animal && animal.isTame() && animal.isOwnedBy(player))
			&& !(target instanceof ArmorStand);
	}

	private static void tickHeld(final ServerPlayer player, final long now) {
		if (player.tickCount % 5 == 0) {
			// Refreshed while held, so it runs out about a second after the sword is put away.
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, true, false, true));
			player.sendOverlayMessage(timersMessage(player.getAttachedOrElse(COOLDOWNS, Cooldowns.NONE), now));
		}

		if (player.tickCount % 4 == 0) {
			player.level().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.35, 0.5, 0.35, 0.01);
		}
	}

	private static long gameTime(final ServerPlayer player) {
		return player.level().getServer().overworld().getGameTime();
	}

	private static Component readyMessage(final String ability) {
		return Component.literal("Magma Sword: " + ability + " is ready!").withStyle(ChatFormatting.GOLD);
	}

	private static Component timersMessage(final Cooldowns cooldowns, final long now) {
		return Component.empty()
			.append(timer("Launch", cooldowns.launchReadyAt() - now))
			.append(Component.literal("  |  ").withStyle(ChatFormatting.DARK_GRAY))
			.append(timer("Fireball", cooldowns.fireballReadyAt() - now));
	}

	private static MutableComponent timer(final String ability, final long ticksLeft) {
		if (ticksLeft <= 0L) {
			return Component.literal(ability + ": READY").withStyle(ChatFormatting.GREEN);
		}

		return Component.literal(ability + ": " + (ticksLeft + 19L) / 20L + "s").withStyle(ChatFormatting.RED);
	}
}
