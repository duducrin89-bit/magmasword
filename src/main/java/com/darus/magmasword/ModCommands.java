package com.darus.magmasword;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ModCommands {
	private ModCommands() {
	}

	public static void initialize() {
		// /magmasword resetcooldown <player> - operators only.
		CommandRegistrationCallback.EVENT
			.register(
				(dispatcher, buildContext, selection) -> dispatcher.register(
					Commands.literal("magmasword")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(
							Commands.literal("resetcooldown")
								.then(
									Commands.argument("player", EntityArgument.player())
										.executes(c -> resetCooldown(c.getSource(), EntityArgument.getPlayer(c, "player")))
								)
						)
				)
			);
	}

	private static int resetCooldown(final CommandSourceStack source, final ServerPlayer player) throws CommandSyntaxException {
		MagmaAbilities.resetCooldowns(player);
		player.sendSystemMessage(Component.literal("Magma Sword: your cooldowns were reset.").withStyle(ChatFormatting.GOLD));
		source.sendSuccess(() -> Component.literal("Reset Magma Sword cooldowns for ").append(player.getDisplayName()), true);
		return 1;
	}
}
