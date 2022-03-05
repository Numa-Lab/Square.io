package net.numalab.tetra

import dev.kotx.flylib.flyLib
import org.bukkit.plugin.java.JavaPlugin

class Tetra : JavaPlugin() {
    private val config = TetraConfig(this).also {
        it.saveConfigIfAbsent()
        it.loadConfig()
    }

    init {
        flyLib {
            command(TetraCommand(config))
        }
    }

    override fun onEnable() {
        // Plugin startup logic
        BlockManager(config, this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
        config.saveConfigIfPresent()
    }
}