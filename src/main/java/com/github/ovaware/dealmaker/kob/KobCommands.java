package com.github.ovaware.dealmaker.kob;

import com.github.ovaware.dealmaker.deal.DealService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Soul-custody authority for the KOB resource scoreboards and portable eye relics. */
final class KobCommands {
    private static final String MANA = "kob.mana";
    private static final String STAMINA = "kob.stamina";
    private static final String MANA_MAX = "kob.mana.max";
    private static final String STAMINA_MAX = "kob.stamina.max";
    private static final List<String> EYES = List.of(
            "knights_of_britannia:left_sharingan", "knights_of_britannia:right_sharingan",
            "knights_of_britannia:left_rinnegan", "knights_of_britannia:right_rinnegan",
            "knights_of_britannia:byakugan_eye", "knights_of_britannia:six_eyes",
            "knights_of_britannia:eye_of_balor");

    private KobCommands() {}

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealmaker")
                .then(Commands.literal("kob").then(Commands.literal("soul")
                        .then(resource("take_mana", MANA, false))
                        .then(resource("take_stamina", STAMINA, false))
                        .then(resource("take_max_mana", MANA_MAX, false))
                        .then(resource("take_max_stamina", STAMINA_MAX, false))
                        .then(resource("drain_mana", MANA, true))
                        .then(resource("drain_stamina", STAMINA, true))
                        .then(resource("drain_max_mana", MANA_MAX, true))
                        .then(resource("drain_max_stamina", STAMINA_MAX, true))
                        .then(Commands.literal("extract_eye").then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("eye", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .executes(context -> extractEye(context.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(context, "target"),
                                                com.mojang.brigadier.arguments.StringArgumentType.getString(context, "eye")))))))));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<net.minecraft.commands.CommandSourceStack, ?> resource(String name, String objective, boolean drain) {
        var target = Commands.argument("target", EntityArgument.player());
        if (drain) return Commands.literal(name).then(target.executes(context -> transfer(
                context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "target"), objective, Integer.MAX_VALUE)));
        return Commands.literal(name).then(target.then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(context -> transfer(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "target"),
                        objective, IntegerArgumentType.getInteger(context, "amount")))));
    }

    private static int transfer(ServerPlayer holder, ServerPlayer owner, String objectiveId, int requested) {
        if (!DealService.hasCustody(holder, owner.getUUID())) return reply(holder, "You do not possess that player's soul.");
        ServerScoreboard scoreboard = holder.server.getScoreboard();
        var objective = scoreboard.getObjective(objectiveId);
        if (objective == null) return reply(holder, "KOB has not created " + objectiveId + " yet.");
        int available = scoreboard.getOrCreatePlayerScore(owner.getScoreboardName(), objective).getScore();
        int amount = Math.min(Math.max(0, available), requested);
        scoreboard.getOrCreatePlayerScore(owner.getScoreboardName(), objective).setScore(available - amount);
        scoreboard.getOrCreatePlayerScore(holder.getScoreboardName(), objective).setScore(
                scoreboard.getOrCreatePlayerScore(holder.getScoreboardName(), objective).getScore() + amount);
        return reply(holder, "Took " + amount + " " + resourceName(objectiveId) + " from " + owner.getName().getString() + ".");
    }

    private static int extractEye(ServerPlayer holder, ServerPlayer owner, String eye) {
        if (!DealService.hasCustody(holder, owner.getUUID())) return reply(holder, "You do not possess that player's soul.");
        String id = "knights_of_britannia:" + eye.toLowerCase(java.util.Locale.ROOT);
        if (!EYES.contains(id)) return reply(holder, "Unsupported eye. Use a portable KOB eye item id without its namespace.");
        Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id));
        if (item == null) return reply(holder, "That KOB eye item is unavailable.");
        for (ItemStack stack : owner.getInventory().items) {
            if (!stack.is(item)) continue;
            ItemStack extracted = stack.split(1);
            if (!holder.getInventory().add(extracted)) holder.drop(extracted, false);
            return reply(holder, "Extracted " + eye + " from " + owner.getName().getString() + ".");
        }
        return reply(holder, "The soul owner does not carry that portable eye item. Installed KOB eye powers are intentionally not stripped.");
    }

    private static int reply(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static String resourceName(String objective) {
        return switch (objective) {
            case MANA -> "mana";
            case STAMINA -> "stamina";
            case MANA_MAX -> "maximum mana";
            case STAMINA_MAX -> "maximum stamina";
            default -> "resource";
        };
    }
}
