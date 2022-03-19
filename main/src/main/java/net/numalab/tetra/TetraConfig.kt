package net.numalab.tetra

import net.kunmc.lab.configlib.BaseConfig
import net.kunmc.lab.configlib.value.BooleanValue
import net.kunmc.lab.configlib.value.DoubleValue
import net.kunmc.lab.configlib.value.collection.StringListValue
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scoreboard.Team

class TetraConfig(plugin: Plugin) : BaseConfig(plugin) {
    private val joinedTeams = StringListValue(listOf())
    fun getJoinedTeams(): List<Team> {
        return joinedTeams.value().mapNotNull { Bukkit.getScoreboardManager().mainScoreboard.getTeam(it) }.distinct()
    }

    fun getJoinedPlayer(containNotSurvival: Boolean): List<Player> {
        return if (containNotSurvival) {
            getJoinedTeams().map { it.entries }.flatten().mapNotNull { Bukkit.getPlayer(it) }
        } else {
            getJoinedTeams().map { it.entries }.flatten().mapNotNull { Bukkit.getPlayer(it) }
                .filter { it.gameMode == GameMode.SURVIVAL }
        }
    }

    fun clearJoinedTeam() {
        joinedTeams.clear()
    }

    fun addJoinedTeam(team: Team) {
        val added = (joinedTeams.value() + team.name).distinct()
        joinedTeams.value(added)
    }

    fun removeJoinedTeam(team: Team) {
        val removed = joinedTeams.value() - team.name
        joinedTeams.value(removed)
    }

    /**
     * 地面へのブロックの設置や、強制移動の切り替え
     */
    val isGoingOn = BooleanValue(false)

    /**
     * 強制移動の速度
     */
    val moveSpeed = DoubleValue(0.3)
}