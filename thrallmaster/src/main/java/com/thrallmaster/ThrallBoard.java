package com.thrallmaster;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ThrallBoard 
{
    private UUID playerID;
    private Scoreboard scoreboard;
    private Objective healthList;

    public ThrallBoard(PlayerStats player)
    {
        playerID = player.getPlayerID();
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        
        generateObjectives();
        updateBoard(player);
        registerBoard();
    }

    public void registerBoard()
    {
        Player player = Bukkit.getPlayer(playerID);
        if (player == null)
        {
            return;
        }

        if (scoreboard != null)
        {
            player.setScoreboard(scoreboard);
        }
    }

    public void updateBoard(PlayerStats player)
    {
        List<ThrallState> thralls = player.getThralls().collect(Collectors.toList());
        if (thralls.size() == 0 || scoreboard == null)
        {
            return;
        }

        Objective healthList = scoreboard.getObjective("health");
        Team team = scoreboard.getTeam("Thralls");
        
        thralls.forEach(state -> 
        {
            if (state.isValidEntity())
           {
                LivingEntity entity = (LivingEntity) state.getEntity();
                int health = (int) entity.getHealth();
                team.addEntity(entity);

                Score score = healthList.getScore(entity.getUniqueId().toString());

                score.setScore(health);
                score.customName(Component.text("Thrall ")
                    .append(Component.text("[ " + state.getBehavior().getBehaviorName() + "]").color(NamedTextColor.WHITE)));
            }
        });
    }

    private void generateObjectives()
    {
        Team team = scoreboard.registerNewTeam("Thralls");
        team.color(NamedTextColor.GREEN);

        healthList = scoreboard.registerNewObjective("health", Criteria.DUMMY, Component.text("Thralls"));
        healthList.setDisplaySlot(DisplaySlot.SIDEBAR);
        healthList.setRenderType(RenderType.HEARTS);
    }

    public void clearBoard() 
    {
        for(Objective obj : scoreboard.getObjectives())
        {
            obj.unregister();
        }

        for(Team team : scoreboard.getTeams())
        {
            team.unregister();
        }

        generateObjectives();
    }

}
