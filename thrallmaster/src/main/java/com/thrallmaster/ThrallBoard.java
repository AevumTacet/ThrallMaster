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

import com.thrallmaster.States.PlayerState;
import com.thrallmaster.States.ThrallState;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ThrallBoard 
{
    private UUID playerID;
    private Scoreboard scoreboard;
    private Objective healthList;

    public ThrallBoard(PlayerState player)
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

    public void updateBoard(PlayerState player)
    {
        List<ThrallState> thralls = player.getThralls().collect(Collectors.toList());
        if (thralls.size() == 0 || scoreboard == null)
        {
            return;
        }

        Objective healthList = scoreboard.getObjective("health");
        Team team_hostile = scoreboard.getTeam("Hostile");
        Team team_defensive = scoreboard.getTeam("Defensive");
        
        thralls.forEach(state -> 
        {
            if (state.isValidEntity())
           {
                LivingEntity entity = (LivingEntity) state.getEntity();
                int health = (int) entity.getHealth();

                if (state.aggressionState == AggressionState.HOSTILE)
                {
                    team_defensive.removeEntity(entity);
                    team_hostile.addEntity(entity);
                }
                else if (state.aggressionState == AggressionState.DEFENSIVE)
                {
                    team_hostile.removeEntity(entity);
                    team_defensive.addEntity(entity);
                }

                Score score = healthList.getScore(entity.getUniqueId().toString());

                score.setScore(health);
                score.customName(Component.text(entity.getName())
                    .append(Component.text("[ " + state.getBehavior().getBehaviorName() + "]")
                    .color(NamedTextColor.WHITE))
                    .append( Component.text(state.isSelected() ? "*" : "")));
            }
        });
    }

    private void generateObjectives()
    {
        Team team_hostile = scoreboard.registerNewTeam("Hostile");
        Team team_defensive = scoreboard.registerNewTeam("Defensive");
        team_hostile.color(NamedTextColor.RED);
        team_defensive.color(NamedTextColor.GREEN);

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
