package net.numalab.tetra

import net.kunmc.lab.configlib.BaseConfig
import net.kunmc.lab.configlib.value.BooleanValue
import net.kunmc.lab.configlib.value.collection.StringListValue
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scoreboard.Team

class TetraConfig(plugin: Plugin) : BaseConfig(plugin) {
    private val joinedTeams = StringListValue(listOf())
    fun getJoinedTeams(): List<Team> {
        return joinedTeams.value().mapNotNull { Bukkit.getScoreboardManager().mainScoreboard.getTeam(it) }.distinct()
    }

    fun setJoinedTeams(teams: List<Team>) {
        joinedTeams.value(teams.map { it.name })
    }

    fun addJoinedTeam(team: Team) {
        val added = joinedTeams.value().also { it.add(team.name) }
        joinedTeams.value(added)
    }

    fun removeJoinedTeam(team: Team) {
        val removed = joinedTeams.value().also { it.remove(team.name) }
        joinedTeams.value(removed)
    }

    /**
     * 地面へのブロックの設置や、強制移動の切り替え
     */
    val isGoingOn = BooleanValue(false)
}