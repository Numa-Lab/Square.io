package net.numalab.tetra

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team

class KillManager(val deathMessenger: DeathMessenger, val blockManager: BlockManager, val config: TetraConfig) {
    /**
     * 敵チームの線に触れた人を殺す
     */
    fun killPlayer(killed: Player, lineLocation: Pair<Int, Int>, world: World, y: Int) {
        val killerUUID = blockManager.playerLineMap.toList().find { it.second.contains(lineLocation) }
        val killer = killerUUID?.first?.let { Bukkit.getPlayer(it) }
        val killedTeam = getTeam(killed)

        // プレイヤーの線リセット
        resetPlayerLine(killed, world, y)


        if (killedTeam != null) {
            val isLastDeath = isLastDeath(killed, killedTeam)
            deathMessenger.addDeadQueue(killed, killer, isLastDeath)
            if (isLastDeath) {
                killTeam(killedTeam, world, y)
                if (isLastTeam(killedTeam)) {
                    // ゲーム終了処理
                    val lastTeam = getLastTeam(killedTeam)
                    deathMessenger.broadCastResult(
                        lastTeam to blockManager.getScore(lastTeam),
                        config.getJoinedTeams().map { it to blockManager.getScore(it) }
                    )

                    if (config.isAutoOff.value()) {
                        config.isGoingOn.value(false)
                        blockManager.reset()
                    }
                }
            }
        } else {
            deathMessenger.addDeadQueue(killed, killer, true)
        }


        // 死亡処理
        killed.health = 0.0
        killed.gameMode = GameMode.SPECTATOR
    }

    /**
     * 倒れた人がチーム内で最後の人かどうか
     */
    private fun isLastDeath(toDead: Player, team: Team): Boolean {
        val remain = (team.entries - toDead.name).mapNotNull { Bukkit.getPlayer(it) }.filter { isValidPlayer(it) }
        return remain.isEmpty()
    }

    private fun isValidPlayer(player: Player): Boolean {
        return player.gameMode == config.targetGameMode.value() && !player.isDead
    }

    private fun isValidTeam(team: Team): Boolean {
        return config.getJoinedTeams().contains(team) &&
                team.entries.mapNotNull { Bukkit.getPlayer(it) }
                    .any { it.gameMode == config.targetGameMode.value() }
    }

    /**
     * 残りのチーム数がが1以下であればtrueを返す
     */
    private fun isLastTeam(toDead: Team): Boolean {
        val remain = config.getJoinedTeams()
            .filter { it.name != toDead.name && isValidTeam(it) }
        return remain.size <= 1
    }

    private fun getLastTeam(toDead: Team): Team {
        val remain = config.getJoinedTeams()
            .filter { it.name != toDead.name && isValidTeam(it) }
        return remain.first()
    }

    private fun getTeam(player: Player): Team? {
        return Bukkit.getScoreboardManager().mainScoreboard.teams.find { it.entries.contains(player.name) }
    }

    /**
     * チームを消す
     */
    private fun killTeam(team: Team, world: World, y: Int) {
        deathMessenger.onTeamDeath(team, blockManager.getScore(team))
        val color = ColorHelper.getBy(team) ?: return
        fillTeamTerritory(color, Material.WHITE_WOOL, world, y)
        blockManager.territoryMap.remove(color)
    }

    private fun fillTeamTerritory(color: ColorHelper, material: Material, world: World, y: Int) {
        val toFill = blockManager.territoryMap[color] ?: return
        val toFillList =
            toFill.getNotZeros().map { Location(world, it.first.toDouble(), y.toDouble(), it.second.toDouble()) }
        blockManager.filler.addQueue(toFillList, material)
    }

    private fun fillPlayerLine(player: Player, material: Material, world: World, y: Int) {
        val toFill = blockManager.playerLineMap[player.uniqueId] ?: return
        val toFillList = toFill.map { Location(world, it.first.toDouble(), y.toDouble(), it.second.toDouble()) }
        blockManager.filler.addQueue(toFillList, material)
    }

    private fun resetPlayerLine(player: Player, world: World, y: Int) {
        val toReset = blockManager.playerLineMap[player.uniqueId] ?: return
        val tl = blockManager.territoryMap.toList()
        toReset.forEach {
            val t = tl.find { l -> l.second[it.first, it.second] != 0.toByte() }
            val location = Location(world, it.first.toDouble(), y.toDouble(), it.second.toDouble())
            if (t != null) {
                location.block.type = wools[t.first.dye]!!
            } else {
                location.block.type = Material.WHITE_WOOL
            }
        }

        blockManager.playerLineMap.remove(player.uniqueId)
    }
}