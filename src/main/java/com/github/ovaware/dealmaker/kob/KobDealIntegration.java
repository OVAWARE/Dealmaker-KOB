package com.github.ovaware.dealmaker.kob;

import com.github.ovaware.dealmaker.api.DealmakerIntegration;
import com.github.ovaware.dealmaker.deal.ClauseKind;
import com.github.ovaware.dealmaker.deal.DealClause;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Implements only KOB assets that are safely represented by scoreboards or portable items. */
final class KobDealIntegration implements DealmakerIntegration {
    private static final String MANA = "kob:mana";
    private static final String STAMINA = "kob:stamina";
    private static final String EYE_PREFIX = "kob:eye/";
    private static final List<String> EYES = List.of("left_sharingan", "right_sharingan", "left_rinnegan",
            "right_rinnegan", "byakugan_eye", "six_eyes", "eye_of_balor");

    @Override
    public boolean supports(DealClause clause) {
        if (isResource(clause.assetId())) return clause.kind() == ClauseKind.TRANSFER_RESOURCE_AMOUNT
                || clause.kind() == ClauseKind.TRANSFER_RESOURCE_PERCENT || clause.kind() == ClauseKind.DRAIN_RESOURCE_AMOUNT
                || clause.kind() == ClauseKind.DRAIN_RESOURCE_PERCENT;
        return clause.kind() == ClauseKind.TRANSFER_SKILL && clause.assetId().startsWith(EYE_PREFIX);
    }

    @Override
    public void validate(DealClause clause, List<String> errors) {
        if (clause.assetId().startsWith(EYE_PREFIX)) {
            if (!EYES.contains(clause.assetId().substring(EYE_PREFIX.length())) || clause.amount() != 0.0) {
                errors.add("Invalid KOB portable-eye transfer.");
            }
            return;
        }
        if (!(clause.amount() > 0.0) || (isPercent(clause) && clause.amount() > 100.0)) {
            errors.add("Invalid KOB resource transfer amount.");
        }
    }

    @Override
    public boolean execute(DealClause clause, ServerPlayer from, ServerPlayer to) {
        if (clause.assetId().startsWith(EYE_PREFIX)) return moveEye(clause.assetId().substring(EYE_PREFIX.length()), from, to);
        String objectiveId = clause.assetId().equals(MANA) ? "kob.mana" : "kob.stamina";
        ServerScoreboard scoreboard = from.server.getScoreboard();
        var objective = scoreboard.getObjective(objectiveId);
        if (objective == null) return false;
        int available = scoreboard.getOrCreatePlayerScore(from.getScoreboardName(), objective).getScore();
        int amount = isPercent(clause) ? (int) Math.floor(available * clause.amount() / 100.0) : (int) clause.amount();
        if (amount < 1 || available < amount) return false;
        scoreboard.getOrCreatePlayerScore(from.getScoreboardName(), objective).setScore(available - amount);
        if (clause.kind() == ClauseKind.TRANSFER_RESOURCE_AMOUNT || clause.kind() == ClauseKind.TRANSFER_RESOURCE_PERCENT) {
            var recipient = scoreboard.getOrCreatePlayerScore(to.getScoreboardName(), objective);
            recipient.setScore(recipient.getScore() + amount);
        }
        return true;
    }

    private static boolean isResource(String id) {
        return MANA.equals(id) || STAMINA.equals(id);
    }

    private static boolean isPercent(DealClause clause) {
        return clause.kind() == ClauseKind.TRANSFER_RESOURCE_PERCENT || clause.kind() == ClauseKind.DRAIN_RESOURCE_PERCENT;
    }

    private static boolean moveEye(String eye, ServerPlayer from, ServerPlayer to) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse("knights_of_britannia:" + eye));
        if (item == null) return false;
        for (ItemStack stack : from.getInventory().items) {
            if (!stack.is(item) || !to.getInventory().add(stack.copyWithCount(1))) continue;
            stack.shrink(1);
            return true;
        }
        return false;
    }
}
