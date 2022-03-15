package net.numalab.tetra

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.java.JavaPlugin

class DeathMessenger(plugin: JavaPlugin) : Listener {
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    private val queue = mutableListOf<Pair<Player, Component>>()

    @EventHandler
    private fun onDeath(e: PlayerDeathEvent) {
        val found = queue.find { it.first.uniqueId == e.entity.uniqueId }
        if (found != null) {
            e.deathMessage(found.second)
        }
    }

    fun addQueue(dead: Player, killer: Player?, isLastDeath: Boolean) {
        queue.add(Pair(dead, getDeathMessage(dead, killer, isLastDeath)))
    }

    private fun getDeathMessage(dead: Player, killer: Player?, isLastDeath: Boolean): Component {
        var deathMessage = "${dead.name} は"
        if (killer != null) {
            deathMessage += "${killer.name} によって殺された"
        } else {
            deathMessage += "死んだ"
        }
        if (isLastDeath) {
            deathMessage += "（チーム最後の生き残り）"
        }
        return Component.text(deathMessage)
    }
}