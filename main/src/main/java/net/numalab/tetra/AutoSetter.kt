package net.numalab.tetra

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.util.*

class AutoSetter(plugin: Tetra, val config: TetraConfig) {
    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, 0, 1)
    }

    private val map = mutableMapOf<UUID, Location>()

    private fun tick() {
        if (!config.isGoingOn.value()) {
            val toProcess = Bukkit.getOnlinePlayers().filter { it.gameMode == config.targetGameMode.value() }
            toProcess.associateWith { getColor(it) }
                .onEach { (player, team) ->
                    if (team != null) {
                        if (!team.entries.contains(player.name)) team.addEntry(player.name)
                        map[player.uniqueId] = player.location.block.location.clone()
                    }
                }
        }
    }

    fun onStart() {
        map.forEach { (t, u) ->
            Bukkit.getPlayer(t)?.teleport(u)
        }

        Bukkit.getOnlinePlayers()
            .filter { it.gameMode == config.targetGameMode.value() && !map.containsKey(it.uniqueId) }
            .forEach {
                it.gameMode = GameMode.SPECTATOR
            }

        clear()
    }

    fun clear() {
        map.clear()
    }

    private fun getColor(player: Player): Team? {
        val bottomBlock = player.location.add(0.0, -1.0, 0.0).block
        val color = bottomBlock.type.getWoolDyeColor()
        if (color != null) {
            val colorHelper = ColorHelper.getBy(color) ?: return null
            val team =
                config.getJoinedTeams().filter { it.color() == colorHelper.textColor }.firstOrNull() ?: return null
            return team
        }
        return null
    }
}