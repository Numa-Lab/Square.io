package net.numalab.tetra

import org.bukkit.Location
import org.bukkit.plugin.Plugin

class WorldFiller(val plugin: Plugin, val config: TetraConfig) {
    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            tick()
        }, 0, 1)
    }

    private val internal = mutableMapOf<Location, ColorHelper>()
    private val toUpdate = mutableListOf<Location>()

    private fun tick() {
        if (!config.isGoingOn.value()) return
        if (toUpdate.isEmpty()) return
        val toApply = toUpdate.filterIndexed { i, _ -> i + 1 <= config.maxBlockChangePerTick.value() }
        toApply.forEach {
            val color = internal[it] ?: return@forEach
            if (!it.block.type.isGlass() || it.block.type.sameColoredGlass(color.dye)) {
                BlockManager.setColoredWoolAt(it, color)
            }
        }

        toUpdate.removeAll(toApply)
    }

    fun addQueue(list: List<Location>, color: ColorHelper) {
        list.forEach {
            internal[it] = color
            toUpdate.add(it)
        }
    }
}