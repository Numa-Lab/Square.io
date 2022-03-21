package net.numalab.tetra

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

class AutoSetter(plugin: Tetra, val config: TetraConfig) {
    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, 0, 1)
    }

    private fun tick() {
        if (!config.isGoingOn.value()) {
            val toProcess = Bukkit.getOnlinePlayers().filter { it.gameMode == GameMode.SURVIVAL }
            toProcess.associateWith { getColor(it) }
                .onEach { (player, team) ->
                    if (team != null) {
                        this.config.getJoinedTeams()
                            .forEach { toLeave -> toLeave.removeEntry(player.name) }
                        team.addEntry(player.name)
                    }
                }
        }
    }

    private fun getColor(player: Player): Team? {
        val bottomBlock = player.location.add(0.0, -1.0, 0.0).block
        val color = bottomBlock.type.getWoolColor()
        if (color != null) {
            val colorHelper = ColorHelper.getBy(color) ?: return null
            val team =
                config.getJoinedTeams().filter { it.color() == colorHelper.textColor }.firstOrNull() ?: return null
            return team
        }
        return null
    }
}