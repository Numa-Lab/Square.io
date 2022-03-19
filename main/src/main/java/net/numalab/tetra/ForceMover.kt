package net.numalab.tetra

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.plugin.java.JavaPlugin

class ForceMover(plugin: JavaPlugin, val config: TetraConfig) : Listener {
    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { onTick() }, 1, 1)
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    private fun onTick() {
        if (!config.isGoingOn.value()) return
        val players = config.getJoinedPlayer(false)
        players.filter { it.gameMode == GameMode.SURVIVAL && it.isValid }.forEach {
            setVelocity(it, config.moveSpeed.value())
        }
    }

    private fun setVelocity(player: Player, speed: Double) {
        val v = player.location.direction.normalize().setY(0.0).normalize().multiply(
            if (player.isOnGround) {
                .15 + speed
            } else {
                .05 + speed
            }
        )

        player.velocity = v
    }

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        if (e.from.blockY != e.to.blockY) {
            if (config.isGoingOn.value() && config.getJoinedPlayer(false).contains(e.player)) {
                e.to = e.to.clone().also { it.y = e.from.blockY.toDouble() }    // ラグで高さが変わらないように
            }
        }
    }

    @EventHandler
    fun onSprint(e: PlayerToggleSprintEvent) {
        if (config.isGoingOn.value() && config.getJoinedPlayer(false).contains(e.player)) {
            e.isCancelled = true    // スピードを変更しないように
        }
    }
}