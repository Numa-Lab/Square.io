package net.numalab.tetra

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.Plugin

class WorldFiller(val plugin: Plugin, val config: TetraConfig) {
    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            tick()
        }, 0, 1)
    }

    private val internal = mutableMapOf<Location, Material>()
    private val toUpdate = mutableListOf<Location>()

    private fun tick() {
        if (!config.isGoingOn.value()) return
        if (toUpdate.isEmpty()) return
        val toApply = toUpdate.filterIndexed { i, _ -> i + 1 <= config.maxBlockChangePerTick.value() }
        toApply.forEach {
            val material = internal[it] ?: return@forEach
            it.block.type = material
        }

        toUpdate.removeAll(toApply)
    }

    fun addQueue(list: List<Location>, color: ColorHelper) {
        val material = wools[color.dye]!!
        addQueue(list, material)
    }

    fun addQueue(list: List<Location>, material: Material) {
        list.forEach {
            internal[it] = material
            toUpdate.add(it)
        }
    }

    fun addQueue(location: Location, material: Material) {
        internal[location] = material
        toUpdate.add(location)
    }

    fun clear() {
        internal.clear()
        toUpdate.clear()
    }
}