package me.lolieg.utility116.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ScoreboardUtil {
    public static String NameByScore(Scoreboard scoreboard, ScoreboardObjective scoreboardObjective, int Score) {
        for (ScoreboardPlayerScore playerScore : scoreboard.getAllPlayerScores(scoreboardObjective)) {
            if (playerScore.getScore() == Score) {
                return playerScore.getPlayerName();
            }
        }
        return null;
    }

    public static void tick(MinecraftServer t){
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        ScoreboardObjective server = t.getScoreboard().getObjective("server");

        ArrayList<String> elements = new ArrayList<>();

        elements.add("§a<---------------->");
        elements.add(MessageFormat.format("§5Players ({0}/{1}):", t.getCurrentPlayerCount(), t.getMaxPlayerCount()));
        for (PlayerEntity playerEntity : t.getPlayerManager().getPlayerList()) {
            elements.add("§d" + playerEntity.getDisplayName().asString() + ": " + Math.round(playerEntity.getX()) + "/" + Math.round(playerEntity.getY()) + "/" + Math.round(playerEntity.getZ()));
        }
        elements.add("§a<---------------->");
        elements.add("§bTime: " + formatter.format(date));
        Collections.reverse(elements);

        for (int i = 0; i < elements.size(); i++) {
            t.getScoreboard().resetPlayerScore(NameByScore(t.getScoreboard(), server, i), server);
            t.getScoreboard().getPlayerScore(elements.get(i), server).setScore(i);
        }
    }

    public static void start(MinecraftServer t){
        ScoreboardObjective server = t.getScoreboard().getObjective("server");
        if (server != null) {
            t.getScoreboard().removeObjective(server);
        }
        server = t.getScoreboard().addObjective("server", ScoreboardCriterion.DUMMY, new LiteralText("Avalanche Server").formatted(Formatting.BOLD, Formatting.GOLD), ScoreboardCriterion.RenderType.INTEGER);
        t.getScoreboard().setObjectiveSlot(1, server);
    }
}
