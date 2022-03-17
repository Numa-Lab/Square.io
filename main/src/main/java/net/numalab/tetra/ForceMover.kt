package net.numalab.tetra

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ForceMover(plugin: JavaPlugin, val config: TetraConfig) {
    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { onTick() }, 1, 1)
    }

    private fun onTick() {
        if (!config.isGoingOn.value()) return
        val players = config.getJoinedPlayer(false)
        players.filter { it.gameMode == GameMode.SURVIVAL && it.isValid }.forEach {
            setVelocity(it)
        }
    }

    private fun setVelocity(player: Player) {
        val v = player.location.direction.normalize().setY(0.0).normalize().multiply(
            if (player.isOnGround) {
                .15
            } else {
                .05
            }
        )

        player.velocity = player.velocity.clone().add(v)
    }
}