package net.numalab.tetra

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class JumpPrevent(plugin: JavaPlugin, val config: TetraConfig) : Listener {
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onJump(event: PlayerJumpEvent) {
        if (config.isGoingOn.value() &&
            config.getJoinedPlayer(false).contains(event.player)
        ) {
            // ゲーム中はジャンプ不可
            event.isCancelled = true
        }
    }
}