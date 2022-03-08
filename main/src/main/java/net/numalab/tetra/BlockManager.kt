package net.numalab.tetra

import net.numalab.tetra.geo.*
import org.bukkit.*
import org.bukkit.entity.Player
import java.util.*

/**
 * チームごとに羊毛、色付きガラスの準備をして地面に設置するクラス
 */
class BlockManager(val config: TetraConfig, plugin: Tetra) {
    companion object {
        private fun setColoredWoolAt(location: Location, color: DyeColor) {
            location.block.type = when (color) {
                DyeColor.WHITE -> Material.WHITE_WOOL
                DyeColor.ORANGE -> Material.ORANGE_WOOL
                DyeColor.MAGENTA -> Material.MAGENTA_WOOL
                DyeColor.LIGHT_BLUE -> Material.LIGHT_BLUE_WOOL
                DyeColor.YELLOW -> Material.YELLOW_WOOL
                DyeColor.LIME -> Material.LIME_WOOL
                DyeColor.PINK -> Material.PINK_WOOL
                DyeColor.GRAY -> Material.GRAY_WOOL
                DyeColor.LIGHT_GRAY -> Material.LIGHT_GRAY_WOOL
                DyeColor.CYAN -> Material.CYAN_WOOL
                DyeColor.PURPLE -> Material.PURPLE_WOOL
                DyeColor.BLUE -> Material.BLUE_WOOL
                DyeColor.BROWN -> Material.BROWN_WOOL
                DyeColor.GREEN -> Material.GREEN_WOOL
                DyeColor.RED -> Material.RED_WOOL
                DyeColor.BLACK -> Material.BLACK_WOOL
            }
        }

        private fun setColoredGlassAt(location: Location, dyeColor: DyeColor) {
            location.block.type = when (dyeColor) {
                DyeColor.WHITE -> Material.WHITE_STAINED_GLASS
                DyeColor.ORANGE -> Material.ORANGE_STAINED_GLASS
                DyeColor.MAGENTA -> Material.MAGENTA_STAINED_GLASS
                DyeColor.LIGHT_BLUE -> Material.LIGHT_BLUE_STAINED_GLASS
                DyeColor.YELLOW -> Material.YELLOW_STAINED_GLASS
                DyeColor.LIME -> Material.LIME_STAINED_GLASS
                DyeColor.PINK -> Material.PINK_STAINED_GLASS
                DyeColor.GRAY -> Material.GRAY_STAINED_GLASS
                DyeColor.LIGHT_GRAY -> Material.LIGHT_GRAY_STAINED_GLASS
                DyeColor.CYAN -> Material.CYAN_STAINED_GLASS
                DyeColor.PURPLE -> Material.PURPLE_STAINED_GLASS
                DyeColor.BLUE -> Material.BLUE_STAINED_GLASS
                DyeColor.BROWN -> Material.BROWN_STAINED_GLASS
                DyeColor.GREEN -> Material.GREEN_STAINED_GLASS
                DyeColor.RED -> Material.RED_STAINED_GLASS
                DyeColor.BLACK -> Material.BLACK_STAINED_GLASS
            }
        }

        fun setColoredWoolAt(location: Location, color: ColorHelper) {
            setColoredWoolAt(location, color.dye)
        }

        fun setColoredGlassAt(location: Location, color: ColorHelper) {
            setColoredGlassAt(location, color.dye)
        }
    }

    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { task() }, 1, 1)
    }

    /**
     * 陣地のマップ
     */
    private val territoryMap = mutableMapOf<ColorHelper, PosSet>()

    /**
     * プレイヤーごとの線
     */
    private val playerLineMap = mutableMapOf<UUID, PosSet>()

    private fun task() {
        if (config.isGoingOn.value()) {
            // 地面に設置する
            val teams = config.getJoinedTeams()
            for (team in teams) {
                val teamColor = ColorHelper.getBy(team) ?: continue

                team.entries
                    .mapNotNull { e -> Bukkit.getOnlinePlayers().find { it.name == e } }
                    .filter { it.gameMode == GameMode.SURVIVAL }
                    .forEach {
                        val bottomLocation = it.location.block.getRelative(0, -1, 0).location
                        if (bottomLocation.block.type.isWool()) {
                            addTerritory(teamColor, MinecraftAdapter.toPos(bottomLocation))
                            if (playerHasLine(it)) {
                                // 線がある場合は塗りつぶしをする
                                val line = playerLineMap[it.uniqueId]!!
                                fillWith(teamColor, line, bottomLocation.blockY.toDouble(), bottomLocation.world)
                                playerLineMap.remove(it.uniqueId)   // 線をリセット
                            }
                        } else {
                            setColoredGlassAt(bottomLocation, teamColor)
                            addPlayerLineRecord(it, MinecraftAdapter.toPos(bottomLocation))
                        }
                    }
            }
        }
    }

    private fun isLineValid(line: PosSet, teamColor: ColorHelper): Boolean {
        val territory = territoryMap[teamColor] ?: return false
        return line.all { territory.contains(it) }
    }

    private fun addTerritory(color: ColorHelper, pos: Pos) {
        if (territoryMap.containsKey(color)) {
            territoryMap[color] = territoryMap[color]!! + pos
        } else {
            territoryMap[color] = listOf(pos)
        }
    }

    private fun fillWith(color: ColorHelper, line: PosSet, y: Double, world: World) {
        val territory = territoryMap[color]
        if (territory != null) {
            val toFill = fillWithForceData(line, territory)
            if (toFill != null) {
                println("ToFill:${toFill.size}")
                val r = ColorHelper.random()
                toFill.forEach {
                    setColoredWoolAt(MinecraftAdapter.toLocation(it, world, y), /*color*/ r)
                }
            } else {
                println("Filling was failed.")
            }
//
//            debugFilling(territory, line, color, y, world)
        } else {
            // 領地がないのに塗ろうとしてる
            // 多分通らない
        }
    }

    private fun playerHasLine(player: Player): Boolean {
        return playerLineMap.containsKey(player.uniqueId) && playerLineMap[player.uniqueId]!!.isNotEmpty()
    }

    private fun addPlayerLineRecord(player: Player, pos: Pos) {
        val uuid = player.uniqueId
        val line = playerLineMap[uuid]
        if (line == null) {
            playerLineMap[uuid] = listOf(pos)
        } else {
            if (line.last() != pos) {
                playerLineMap[uuid] = line + pos
            }
        }
    }

    private fun debugFilling(territory: PosSet, line: PosSet, color: ColorHelper, y: Double, world: World) {
        val added = territory + line
        val rayTracePX = rayTraceFromPX(added)
        val rayTracePZ = rayTraceFromPZ(added)
        val rayTraceNX = rayTraceFromNX(added)
        val rayTraceNZ = rayTraceFromNZ(added)
        val rayTraceAll = rayTracePX + rayTracePZ + rayTraceNX + rayTraceNZ

        fillAt(rayTraceAll, ColorHelper.random(), y, world)

        val inside = rayTraceAll.forceCast().getInside()
        if (inside == null) {
            println("Inside is null.")
        } else {
            fillAt(inside, ColorHelper.random(), y, world)
        }
    }

    @JvmName("fillAtN")
    private fun fillAt(posn: PosSetNullable, color: ColorHelper, y: Double, world: World) {
        fillAt(posn.filterNotNull(), color, y, world)
    }

    private fun fillAt(posset: PosSet, color: ColorHelper, y: Double, world: World) {
        posset.forEach {
            setColoredWoolAt(MinecraftAdapter.toLocation(it, world, y), color)
        }
    }
}

private val wools = mapOf(
    DyeColor.WHITE to Material.WHITE_WOOL,
    DyeColor.ORANGE to Material.ORANGE_WOOL,
    DyeColor.MAGENTA to Material.MAGENTA_WOOL,
    DyeColor.LIGHT_BLUE to Material.LIGHT_BLUE_WOOL,
    DyeColor.YELLOW to Material.YELLOW_WOOL,
    DyeColor.LIME to Material.LIME_WOOL,
    DyeColor.PINK to Material.PINK_WOOL,
    DyeColor.GRAY to Material.GRAY_WOOL,
    DyeColor.LIGHT_GRAY to Material.LIGHT_GRAY_WOOL,
    DyeColor.CYAN to Material.CYAN_WOOL,
    DyeColor.PURPLE to Material.PURPLE_WOOL,
    DyeColor.BLUE to Material.BLUE_WOOL,
    DyeColor.BROWN to Material.BROWN_WOOL,
    DyeColor.GREEN to Material.GREEN_WOOL,
    DyeColor.RED to Material.RED_WOOL,
    DyeColor.BLACK to Material.BLACK_WOOL
)

private val glasses = mapOf(
    DyeColor.WHITE to Material.WHITE_STAINED_GLASS,
    DyeColor.ORANGE to Material.ORANGE_STAINED_GLASS,
    DyeColor.MAGENTA to Material.MAGENTA_STAINED_GLASS,
    DyeColor.LIGHT_BLUE to Material.LIGHT_BLUE_STAINED_GLASS,
    DyeColor.YELLOW to Material.YELLOW_STAINED_GLASS,
    DyeColor.LIME to Material.LIME_STAINED_GLASS,
    DyeColor.PINK to Material.PINK_STAINED_GLASS,
    DyeColor.GRAY to Material.GRAY_STAINED_GLASS,
    DyeColor.LIGHT_GRAY to Material.LIGHT_GRAY_STAINED_GLASS,
    DyeColor.CYAN to Material.CYAN_STAINED_GLASS,
    DyeColor.PURPLE to Material.PURPLE_STAINED_GLASS,
    DyeColor.BLUE to Material.BLUE_STAINED_GLASS,
    DyeColor.BROWN to Material.BROWN_STAINED_GLASS,
    DyeColor.GREEN to Material.GREEN_STAINED_GLASS,
    DyeColor.RED to Material.RED_STAINED_GLASS,
    DyeColor.BLACK to Material.BLACK_STAINED_GLASS
)

fun Material.isWool(): Boolean {
    return wools.values.contains(this)
}

fun Material.isGlass(): Boolean {
    return glasses.values.contains(this)
}

fun Material.sameColoredWool(dyeColor: DyeColor): Boolean {
    if (!this.isWool()) return false
    else {
        return wools[dyeColor] == this
    }
}

fun Material.sameColoredGlass(dyeColor: DyeColor): Boolean {
    if (!this.isGlass()) return false
    else {
        return glasses[dyeColor] == this
    }
}