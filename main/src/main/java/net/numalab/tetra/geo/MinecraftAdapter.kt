package net.numalab.tetra.geo

import org.bukkit.Location
import org.bukkit.World

class MinecraftAdapter {
    companion object {
        fun toPos(location: Location): Pair<Int, Int> {
            return Pair(location.blockX, location.blockZ)
        }

        fun toLocation(pos: Pair<Int, Int>, world: World, y: Double): Location {
            return Location(world, pos.first.toDouble(), y, pos.second.toDouble())
        }
    }
}