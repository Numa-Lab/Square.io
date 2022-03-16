package net.numalab.tetra

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class BlockBreakPrevent(plugin: JavaPlugin, val config: TetraConfig) : Listener {
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onBlockBreak(event: org.bukkit.event.block.BlockBreakEvent) {
        if (config.isGoingOn.value() &&
            event.player.gameMode == GameMode.SURVIVAL &&
            config.getJoinedTeams()
                .any { it.entries.mapNotNull { e -> Bukkit.getPlayer(e) }.contains(event.player) }
        ) {
            // ゲーム中はブロック破壊不可
            event.isCancelled = true
        }
    }
}