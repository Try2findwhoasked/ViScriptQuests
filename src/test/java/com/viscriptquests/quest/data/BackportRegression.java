package com.viscriptquests.quest.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Deterministic checks against the real Forge 1.20.1 compile/runtime classpath. */
public final class BackportRegression {
    public static void main(String[] args) {
        var board = new Scoreboard();
        var objective = board.addObjective("story", ObjectiveCriteria.DUMMY,
                Component.literal("story"), ObjectiveCriteria.RenderType.INTEGER);
        board.getOrCreatePlayerScore("player", objective).setScore(3);
        board.getOrCreatePlayerScore("chapter", objective).setScore(17);
        require(QuestValueToken.readScoreboardValue(board, "story", " chapter ", "player") == 17,
                "Named holders must override player scores");
        require(QuestValueToken.readScoreboardValue(board, "story", "", "player") == 3,
                "Blank holder must use the player");
        require(QuestValueToken.readScoreboardValue(board, "story", null, "player") == 3,
                "Legacy missing holder must use the player");
        require(QuestValueToken.readScoreboardValue(board, "story", "missing", "player") == 0,
                "Missing holder must read zero");
        require(!board.hasPlayerScore("missing", objective), "Reading must not create a score");
        require(QuestValueToken.readScoreboardValue(board, "missing", "chapter", "player") == 0,
                "Missing objective must read zero");
        for (var hand : InteractionHand.values()) {
            require(ItemUseHand.ANY.matches(hand), "ANY must accept both hands");
            require(ItemUseHand.MAIN_HAND.matches(hand) == (hand == InteractionHand.MAIN_HAND),
                    "Main-hand filter");
            require(ItemUseHand.OFF_HAND.matches(hand) == (hand == InteractionHand.OFF_HAND),
                    "Off-hand filter");
        }
        require(!ItemUseHand.MAIN_HAND.matches(null), "Unknown hand must not pass a specific filter");
        System.out.println("Backport regression checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
