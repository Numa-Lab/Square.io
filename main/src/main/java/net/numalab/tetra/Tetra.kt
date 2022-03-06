package net.numalab.tetra

import dev.kotx.flylib.flyLib
import net.numalab.tetra.geo.BlockLocation
import net.numalab.tetra.test.AutoSelectTest
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.plugin.java.JavaPlugin

class Tetra : JavaPlugin() {
    private val config = TetraConfig(this).also {
        it.saveConfigIfAbsent()
        it.loadConfig()
    }

    init {
        flyLib {
            command(TetraCommand(config))

            listen(
                BlockBreakEvent::class.java,
                action = {
                    AutoSelectTest(BlockLocation(it.block.location)).autoSelect(it.player)
                    it.isCancelled = true
                })
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