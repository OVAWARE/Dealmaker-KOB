package com.github.ovaware.dealmaker.kob;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Consumer;

/** Extracts installed KOB eye state into the matching portable item. */
final class KobEyes {
    private static final List<String> EYES = List.of("left_sharingan", "right_sharingan", "left_rinnegan",
            "right_rinnegan", "byakugan_eye", "six_eyes", "eye_of_balor");

    private KobEyes() {}

    static boolean extract(ServerPlayer owner, String eye, Consumer<ItemStack> receiver) {
        if (!EYES.contains(eye)) return false;
        if (removeInstalled(owner, eye) || removeItem(owner, eye, receiver)) {
            receiver.accept(new ItemStack(item(eye)));
            return true;
        }
        return false;
    }

    static boolean extractAll(ServerPlayer owner, Consumer<ItemStack> receiver) {
        boolean extracted = false;
        for (String eye : EYES) extracted |= extract(owner, eye, receiver);
        return true; // "all eyes" is deliberately a no-op when none are present.
    }

    private static boolean removeInstalled(ServerPlayer player, String eye) {
        ServerScoreboard board = player.server.getScoreboard();
        return switch (eye) {
            case "left_sharingan" -> clear(board, player, "kob.left.sharingan");
            case "right_sharingan" -> clear(board, player, "kob.right.sharingan");
            case "left_rinnegan" -> clear(board, player, "kob.rinnegan.left");
            case "right_rinnegan" -> clear(board, player, "kob.rinnegan.right");
            case "byakugan_eye" -> clearEither(board, player, "kob.byakugan.left", "kob.byakugan.right");
            case "six_eyes" -> clearEither(board, player, "kob.six_eyes.left", "kob.six_eyes.right");
            case "eye_of_balor" -> clearEither(board, player, "kob.eye_of_balor.left", "kob.eye_of_balor.right");
            default -> false;
        };
    }

    private static boolean clear(ServerScoreboard board, ServerPlayer player, String objectiveId) {
        var objective = board.getObjective(objectiveId);
        if (objective == null) return false;
        var score = board.getOrCreatePlayerScore(player.getScoreboardName(), objective);
        if (score.getScore() < 1) return false;
        score.setScore(0);
        return true;
    }

    private static boolean clearEither(ServerScoreboard board, ServerPlayer player, String left, String right) {
        return clear(board, player, left) || clear(board, player, right);
    }

    private static boolean removeItem(ServerPlayer player, String eye, Consumer<ItemStack> receiver) {
        Item item = item(eye);
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(item)) continue;
            stack.shrink(1);
            return true;
        }
        return false;
    }

    private static Item item(String eye) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse("knights_of_britannia:" + eye));
        if (item == null) throw new IllegalStateException("Missing KOB eye item " + eye);
        return item;
    }
}
