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
            if (isSameColor(it.block.type, material)) {
                it.block.type = material
            }
        }

        toUpdate.removeAll(toApply)
    }

    private fun isSameColor(from: Material, to: Material): Boolean {
        val fromColor = getColor(from)
        val toColor = getColor(to)
        return fromColor == toColor || fromColor == null || toColor == null
    }

    private fun getColor(material: Material): ColorHelper? {
        val glass = material.getGlassColor()
        if (glass != null) return glass
        val wool = material.getWoolColor()
        if (wool != null) return wool
        return null
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

    fun immediate(location: Location, material: Material) {
        location.block.type = material
    }

    fun clear() {
        internal.clear()
        toUpdate.clear()
    }
}