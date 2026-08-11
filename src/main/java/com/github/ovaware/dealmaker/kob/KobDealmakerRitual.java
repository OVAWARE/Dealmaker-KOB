package com.github.ovaware.dealmaker.kob;

import com.github.ovaware.dealmaker.registry.DealmakerCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Grants the Core Dealmaker mark through the KOB Human/Demon fire ritual. */
final class KobDealmakerRitual {
    private static final String RITUAL_TAG = "dealmaker_kob_ritual_complete";
    private static final double RITUAL_RADIUS = 3.0D;

    private KobDealmakerRitual() {}

    @SubscribeEvent
    static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!player.isOnFire() || player.getTags().contains(RITUAL_TAG)
                || DealmakerCapabilities.data(player).dealmaker()) return;
        if (!isHumanOrDemon(player)) return;

        for (ItemEntity item : player.serverLevel().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(RITUAL_RADIUS))) {
            if (!item.isOnFire() || !item.getItem().is(Items.WRITABLE_BOOK)) continue;
            item.getItem().shrink(1);
            if (item.getItem().isEmpty()) item.discard();
            DealmakerCapabilities.data(player).setDealmaker(true);
            player.addTag(RITUAL_TAG);
            player.sendSystemMessage(Component.literal("The burning Book and Quill binds you as a Dealmaker.")
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
    }

    private static boolean isHumanOrDemon(ServerPlayer player) {
        var objective = player.server.getScoreboard().getObjective("kob.race");
        if (objective == null) return false;
        int race = player.server.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
        return race == 1 || race == 5;
    }
}
