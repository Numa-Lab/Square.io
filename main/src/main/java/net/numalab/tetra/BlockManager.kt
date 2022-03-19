package net.numalab.tetra

import net.numalab.tetra.geo.MinecraftAdapter
import net.numalab.tetra.geo.PosSet
import net.numalab.tetra.geo.fill
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.util.*
import java.util.function.BiFunction

/**
 * チームごとに羊毛、色付きガラスの準備をして地面に設置するクラス
 */
class BlockManager(private val config: TetraConfig, plugin: Tetra) {
    companion object {
        private fun setColoredWoolAt(location: Location, color: DyeColor) {
            location.block.type = wools[color]!!
        }

        private fun setColoredGlassAt(location: Location, dyeColor: DyeColor) {
            location.block.type = glasses[dyeColor]!!
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

        config.isGoingOn.onModify(BiFunction { bool, _ ->
            if (!bool) {
                // すべてリセット
                territoryMap.clear()
                playerLineMap.clear()
                scoreBoardManager.reset()
            }

            return@BiFunction false
        })
    }

    /**
     * 陣地のマップ
     */
    private val territoryMap = mutableMapOf<ColorHelper, PosSet>()

    /**
     * プレイヤーごとの線
     */
    private val playerLineMap = mutableMapOf<UUID, List<Pair<Int, Int>>>()

    private val scoreBoardManager = ScoreBoardManager()
    private val deathMessenger = DeathMessenger(plugin, config)

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
                            if (bottomLocation.block.type.sameColoredWool(teamColor.dye)) {
                                addTerritory(teamColor, MinecraftAdapter.toPos(bottomLocation))
                                if (playerHasLine(it)) {
                                    // 線がある場合は塗りつぶしをする
                                    val line = playerLineMap[it.uniqueId]!!
                                    fillWith(teamColor, line, bottomLocation.blockY.toDouble(), bottomLocation.world)
                                    playerLineMap.remove(it.uniqueId)   // 線をリセット
                                }
                                // スコアボードを更新
                                updateScoreBoard(team, teamColor)
                            } else {
                                // 線追加
                                setColoredGlassAt(bottomLocation, teamColor)
                                addPlayerLineRecord(it, MinecraftAdapter.toPos(bottomLocation))
                            }
                        } else {
                            if (bottomLocation.block.type.isGlass() && !bottomLocation.block.type.sameColoredGlass(
                                    teamColor.dye
                                )
                            ) {
                                // 他のチームの線に触れた
                                //キルログ処理
                                val toKillUUID = playerLineMap.filter { (_, line) ->
                                    line.contains(
                                        Pair(
                                            bottomLocation.blockX,
                                            bottomLocation.blockZ
                                        )
                                    )
                                }.keys.firstOrNull()

                                val toKill = if (toKillUUID != null) Bukkit.getPlayer(toKillUUID) else null
                                if (toKill != null) {
                                    val toKillName = toKill.name
                                    val toKillTeam =
                                        config.getJoinedTeams().find { t -> t.entries.contains(toKillName) }
                                    if (toKillTeam != null) {
                                        val isFinalKill =
                                            (toKillTeam.entries.filter { e ->
                                                val p =
                                                    Bukkit.getPlayer(e); p != null && p.gameMode == GameMode.SURVIVAL
                                            } - toKillName).isNotEmpty()

                                        deathMessenger.addDeadQueue(toKill, it, isFinalKill)
                                        toKill.health = 0.0
                                        toKill.gameMode = GameMode.SPECTATOR

// チーム死亡判定・ゲーム終了判定
                                        if (isFinalKill) {
                                            val lastTeam = deathMessenger.onTeamDeath(
                                                toKillTeam,
                                                getScore(ColorHelper.getBy(toKillTeam))
                                            )
                                            if (lastTeam.first) {
                                                // ゲーム終了
                                                val winner = lastTeam.second
                                                if (winner != null) {
                                                    deathMessenger.broadCastResult(
                                                        winner to getScore(ColorHelper.getBy(winner)),
                                                        (config.getJoinedTeams() - winner).map { t ->
                                                            t to getScore(
                                                                ColorHelper.getBy(
                                                                    t
                                                                )
                                                            )
                                                        })
                                                } else {
                                                    // 勝者がいない
                                                    deathMessenger.broadCastResult(
                                                        null,
                                                        (config.getJoinedTeams()).map { t ->
                                                            t to getScore(
                                                                ColorHelper.getBy(
                                                                    t
                                                                )
                                                            )
                                                        })
                                                }

                                                if (config.isAutoOff.value()) {
                                                    // 自動的に終了
                                                    config.isGoingOn.value(false)
                                                }
                                            }
                                        }
                                    }
                                    // 引いてきた線を元に戻す
                                    playerLineMap[toKill.uniqueId]?.forEach { pair ->
                                        val found = territoryMap.entries.firstOrNull { e -> e.value.contains(pair) }
                                        if (found != null) {
                                            // 羊毛に戻してあげる
                                            setColoredWoolAt(
                                                bottomLocation.add(
                                                    pair.first.toDouble(),
                                                    bottomLocation.blockY.toDouble(),
                                                    pair.second.toDouble()
                                                ),
                                                found.key.dye
                                            )
                                        } else {
                                            // 何も無かったことにする
                                            it.world.getBlockAt(pair.first, bottomLocation.blockY, pair.second).type =
                                                Material.AIR
                                        }
                                    }
                                    playerLineMap.remove(toKill.uniqueId)   // 線をリセット
                                }
                            } else {
                                // 線追加
                                setColoredGlassAt(bottomLocation, teamColor)
                                addPlayerLineRecord(it, MinecraftAdapter.toPos(bottomLocation))
                            }
                        }
                    }
            }
        }
    }

    private fun getScore(teamColor: ColorHelper?): Int {
        if (teamColor == null) return -1
        return territoryMap[teamColor]?.getNotZeros()?.size ?: -1
    }

    private fun updateScoreBoard(team: Team, teamColor: ColorHelper) {
        scoreBoardManager.updateScoreBoard(team, getScore(teamColor))
    }

    private fun addTerritory(color: ColorHelper, pos: Pair<Int, Int>) {
        if (territoryMap.containsKey(color)) {
            territoryMap[color] = territoryMap[color]!!.add(pos.first, pos.second, 1.toByte())
        } else {
            territoryMap[color] =
                PosSet(pos.first, pos.second, pos.first, pos.second).also { it[pos.first, pos.second] = 1.toByte() }
        }
    }

    private fun fillWith(color: ColorHelper, line: List<Pair<Int, Int>>, y: Double, world: World) {
        val territory = territoryMap[color]
        if (territory != null) {
            val linePosSet = PosSet.of(line)
            val toFill = fill(linePosSet, territory)
            val notZero = toFill.getNotZeros()
            notZero.forEach {
                setColoredWoolAt(MinecraftAdapter.toLocation(Pair(it.first, it.second), world, y), color)
            }
            territoryMap[color] = territory + toFill
        } else {
            // 領地がないのに塗ろうとしてる
            // 多分通らない
        }
    }

    private fun playerHasLine(player: Player): Boolean {
        return playerLineMap.containsKey(player.uniqueId) && playerLineMap[player.uniqueId]!!.isNotEmpty()
    }

    private fun addPlayerLineRecord(player: Player, pos: Pair<Int, Int>) {
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