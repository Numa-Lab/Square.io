package net.numalab.tetra.test

import net.numalab.tetra.geo.BlockLocation
import net.numalab.tetra.geo.autoSelect
import net.numalab.tetra.isWool
import org.bukkit.entity.Player

class AutoSelectTest(private val location: BlockLocation) {
    fun autoSelect(player: Player) {
        val type = location.loc.block.type
        if (type.isWool()) {
            val auto = location.autoSelect({ it.block.type == type })
            player.sendMessage("selected: ${auto.joinToString(separator = ";") { "[X:${it.loc.blockX},Z:${it.loc.blockZ}]" }}")
        }
    }
}