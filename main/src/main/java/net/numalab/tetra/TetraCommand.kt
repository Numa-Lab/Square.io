package net.numalab.tetra

import dev.kotx.flylib.command.Command
import dev.kotx.flylib.command.Permission
import net.kunmc.lab.configlib.ConfigCommandBuilder
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.scoreboard.Team

class TetraCommand(config: TetraConfig) : Command("tetra") {
    init {
        description("Tetra command")
        permission(Permission.OP)
        usage {
            selectionArgument("On/Off", "ON", "OFF")
            executes {
                when (typedArgs[0] as String) {
                    "ON" -> {
                        config.isGoingOn.value(true)
                        success("ONになりました")
                    }
                    "OFF" -> {
                        config.isGoingOn.value(false)
                        success("OFFになりました")
                    }
                    else -> {
                        fail("正しい引数ではありません")
                    }
                }
            }
        }

        children(TetraTeamCommand(config), ConfigCommandBuilder(config).build())
    }
}

class TetraTeamCommand(private val config: TetraConfig) : Command("team") {
    init {
        description("Tetra team command")
        permission(Permission.OP)
        usage {
            selectionArgument("selection", "registerTeam", "removeTeam", "clear")
            stringArgument(
                "teamName",
                { suggestAll(Bukkit.getScoreboardManager().mainScoreboard.teams.map { it.name }) })

            executes {
                val teamName = typedArgs[1] as String
                val team = Bukkit.getScoreboardManager().mainScoreboard.getTeam(teamName)
                if (team == null) {
                    fail("$teamName というチームは存在しません")
                } else {
                    when (typedArgs[0] as String) {
                        "registerTeam" -> {
                            if (checkTeamColor(team)) {
                                this@TetraTeamCommand.config.addJoinedTeam(team)
                                success("${displayTeam(team)} というチームを登録しました")
                            } else {
                                fail("${team.name} というチームの色は使用できません。")
                            }
                        }
                        "removeTeam" -> {
                            this@TetraTeamCommand.config.addJoinedTeam(team)
                            success("${displayTeam(team)} というチームを削除しました")
                        }
                        "clear" -> {
                            this@TetraTeamCommand.config.addJoinedTeam(team)
                            success("チームを全て削除しました")
                        }
                    }
                }
            }
        }
    }

    private fun displayTeam(team: Team): String {
        val c = ColorHelper.getBy(team)
        return if (c != null) displayTeam(team.name, c)
        else {
            "${team.name}[TextColor:${team.color().name()},DyeColor:UNDEFINED]"
        }
    }

    private fun displayTeam(teamName: String, teamColor: ColorHelper): String {
        return "${teamName}[TextColor:${teamColor.textColor.name()},DyeColor:${teamColor.dye.name}]"
    }

    private fun TextColor.name(): String {
        return if (this is NamedTextColor) {
            this.toString()
        } else {
            "Hex:${this.asHexString()}"
        }
    }

    /**
     * 当該チームの文字色が自動的に変換されるかどうか
     */
    private fun checkTeamColor(team: Team): Boolean {
        return ColorHelper.getBy(team) != null
    }
}