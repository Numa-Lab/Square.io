package net.numalab.tetra

import net.numalab.tetra.geo.MinecraftAdapter
import net.numalab.tetra.geo.PosSet
import net.numalab.tetra.thread.FillOrder
import net.numalab.tetra.thread.WorldFillOrder
import net.numalab.tetra.thread.resolve
import org.bukkit.Bukkit
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.util.*
import java.util.function.BiFunction

/**
 * チームごとに羊毛、色付きガラスの準備をして地面に設置するクラス
 */
class BlockManager(
    private val config: TetraConfig,
    val plugin: Tetra,
    val autoSetter: AutoSetter,
    val filler: WorldFiller
) {
    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { task() }, 1, 1)

        config.isGoingOn.onModify(BiFunction { bool, _ ->
            if (!bool) {
                // すべてリセット
                reset()
            }

            return@BiFunction false
        })
    }

    /**
     * 陣地のマップ
     */
    val territoryMap = mutableMapOf<ColorHelper, PosSet>()

    /**
     * プレイヤーごとの線
     */
    val playerLineMap = mutableMapOf<UUID, List<Pair<Int, Int>>>()

    private val scoreMap = mutableMapOf<Team, Int>()

    private val scoreBoardManager = ScoreBoardManager()
    private val deathMessenger = DeathMessenger(plugin, config)
    private val killManager = KillManager(deathMessenger, this, config)

    private fun task() {
        if (config.isGoingOn.value()) {
            // 地面に設置する
            val teams = config.getJoinedTeams()
            for (team in teams) {
                val teamColor = ColorHelper.getBy(team) ?: continue

                team.entries
                    .mapNotNull { e -> Bukkit.getPlayer(e) }
                    .filter { it.gameMode == config.targetGameMode.value() }
                    .forEach {
                        val bottomLocation = it.location.block.getRelative(0, -1, 0).location
                        if (bottomLocation.block.type.isWool()) {
                            if (bottomLocation.block.type.sameColoredWool(teamColor.dye)) {
                                addTerritory(teamColor, MinecraftAdapter.toPos(bottomLocation))
                                if (playerHasLine(it)) {
                                    // 線がある場合は塗りつぶしをする
                                    val line = playerLineMap[it.uniqueId]!!
                                    fillWith(
                                        teamColor,
                                        line,
                                        bottomLocation.blockY.toDouble(),
                                        bottomLocation.world,
                                        it
                                    )
                                    playerLineMap.remove(it.uniqueId)   // 線をリセット
                                }
                                // スコアボードを更新
                                updateScore()
                            } else {
                                // 線追加
                                filler.immediate(bottomLocation, glasses[teamColor.dye]!!)
                                addPlayerLineRecord(it, MinecraftAdapter.toPos(bottomLocation))
                            }
                        } else {
                            if (bottomLocation.block.type.isGlass() && !bottomLocation.block.type.sameColoredGlass(
                                    teamColor.dye
                                )
                            ) {
                                // キル処理
                                killManager.killPlayer(
                                    it,
                                    bottomLocation.blockX to bottomLocation.blockZ,
                                    bottomLocation.world,
                                    bottomLocation.blockY
                                )
                            } else {
                                // 線追加
                                filler.immediate(bottomLocation, glasses[teamColor.dye]!!)
                                addPlayerLineRecord(it, MinecraftAdapter.toPos(bottomLocation))
                            }
                        }
                    }
            }
        }
        // スコアボード同期
        scoreBoardManager.updateGameState(config)
    }

    fun getScore(team: Team): Int {
        return scoreMap[team] ?: -1
    }

    private fun updateScore() {
        config.getJoinedTeams()
            .map { it to ColorHelper.getBy(it) }
            .filter { it.second != null }
            .forEach { updateScore(it.first, it.second!!) }
    }

    private fun updateScore(team: Team, teamColor: ColorHelper) {
        val n = territoryMap[teamColor]?.getNotZeros()
        if (n != null) {
            scoreMap[team] = n.size
        }
        scoreBoardManager.updateScoreBoard(team, getScore(team))
    }

    private fun addTerritory(color: ColorHelper, pos: Pair<Int, Int>) {
        if (territoryMap.containsKey(color)) {
            territoryMap[color] = territoryMap[color]!!.add(pos.first, pos.second, 1.toByte())
        } else {
            territoryMap[color] =
                PosSet(pos.first, pos.second, pos.first, pos.second).also {
                    it[pos.first, pos.second, false] = 1.toByte()
                }
        }
    }

    private fun fillWith(color: ColorHelper, line: List<Pair<Int, Int>>, y: Double, world: World, drawer: Player) {
        val territory = territoryMap[color]
        if (territory != null) {
            val fillOrder = FillOrder(
                line.toList(),
                territory.clone(),
                config.fillAlgorithm.value()
            )
            val worldFillOrder = WorldFillOrder(world, y.toInt(), color, fillOrder, drawer, filler) { toFill ->
                territoryMap[color] = territory + toFill

                // 増加したブロックの上に敵チームがいたらキルする
                ((territory + toFill) - territory).also { gained ->
                    killManager.killPlayer(gained, drawer)
                }

                // 他チームの領土をちゃんと削って反映、
                // 領土が0になったらチーム敗北判定
                (territoryMap.keys - color).forEach {
                    val removed = territoryMap[it]!! - toFill
                    if (removed.getNotZeros().isEmpty()) {
                        // このチームが塗りつぶした領土がなくなった
                        killManager.killEntireTeam(it)
                        territoryMap.remove(it)
                    } else {
                        territoryMap[it] = removed
                    }
                }
            }

            worldFillOrder.resolve(plugin)
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

    fun reset() {
        territoryMap.clear()
        playerLineMap.clear()
        autoSetter.clear()
        scoreBoardManager.reset()
        scoreMap.clear()
        filler.clear()
    }
}

val wools = mapOf(
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

val glasses = mapOf(
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

fun Material.getWoolColor(): DyeColor? {
    return if (this.isWool()) {
        wools.filter { it.value == this }.keys.first()
    } else {
        null
    }
}