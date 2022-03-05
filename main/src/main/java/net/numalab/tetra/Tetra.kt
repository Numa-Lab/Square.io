package net.numalab.tetra

import dev.kotx.flylib.flyLib
import org.bukkit.plugin.java.JavaPlugin

class Tetra : JavaPlugin() {
    val config = TetraConfig(this).also {
        it.saveConfigIfAbsent()
        it.loadConfig()
    }

    init {
        flyLib {

        }
    }

    override fun onEnable() {
        // Plugin startup logic
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}