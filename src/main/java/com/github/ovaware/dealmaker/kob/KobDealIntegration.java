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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

    @Override
    public String aiInstructions() {
        return """
                Knights of Britannia is installed. Its transferable current resources are mana and stamina.
                For “give/pay/take/transfer N mana”, emit TRANSFER_RESOURCE_AMOUNT with assetId kob:mana.
                For a percentage of mana, emit TRANSFER_RESOURCE_PERCENT with assetId kob:mana.
                For “drain/remove N mana” without crediting the other party, emit DRAIN_RESOURCE_AMOUNT with assetId kob:mana.
                Use the matching STAMINA forms with assetId kob:stamina. These transfer current resource only, never maximum resource.
                Portable KOB eye items may be transferred with TRANSFER_SKILL, amount 0, and one assetId from:
                kob:eye/left_sharingan, kob:eye/right_sharingan, kob:eye/left_rinnegan,
                kob:eye/right_rinnegan, kob:eye/byakugan_eye, kob:eye/six_eyes, kob:eye/eye_of_balor.
                The source must actually possess the physical eye item. Never transfer KOB race, subclass, installed powers,
                special techniques, transformations, or maximum mana/stamina.
                """;
    }

    @Override
    public void extendAiSchema(JsonObject schema) {
        JsonObject clause = schema.getAsJsonObject("properties").getAsJsonObject("clauses")
                .getAsJsonObject("items");
        JsonArray kinds = clause.getAsJsonObject("properties").getAsJsonObject("kind").getAsJsonArray("enum");
        addEnum(kinds, "TRANSFER_RESOURCE_AMOUNT");
        addEnum(kinds, "TRANSFER_RESOURCE_PERCENT");
        addEnum(kinds, "DRAIN_RESOURCE_AMOUNT");
        addEnum(kinds, "DRAIN_RESOURCE_PERCENT");
        addEnum(kinds, "TRANSFER_SKILL");
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

    private static void addEnum(JsonArray values, String value) {
        for (var element : values) if (value.equals(element.getAsString())) return;
        values.add(value);
    }
}
