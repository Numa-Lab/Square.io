package net.numalab.tetra.geo

import org.bukkit.Location
import org.bukkit.World

class MinecraftAdapter {
    companion object {
        fun toPos(location: Location): Pos {
            return Pos(location.blockX, location.blockZ)
        }

        fun toLocation(pos: Pos, world: World, y: Double): Location {
            return Location(world, pos.x.toDouble(), y, pos.z.toDouble())
        }
    }
}