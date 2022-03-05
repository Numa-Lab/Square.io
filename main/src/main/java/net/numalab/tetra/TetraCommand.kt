package net.numalab.tetra

import dev.kotx.flylib.command.Command
import dev.kotx.flylib.command.Permission
import net.kunmc.lab.configlib.ConfigCommandBuilder
import org.bukkit.Bukkit

class TetraCommand(config: TetraConfig) : Command("tetra") {
    init {
        description("Tetra command")
        permission(Permission.OP)
        usage {
        }

        children(TetraTeamCommand(config), ConfigCommandBuilder(config).build())
    }
}

class TetraTeamCommand(config: TetraConfig) : Command("team") {
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
                            config.addJoinedTeam(team)
                            success("$teamName というチームを登録しました")
                        }
                        "removeTeam" -> {
                            config.removeJoinedTeam(team)
                            success("$teamName というチームを削除しました")
                        }
                        "clear" -> {
                            config.setJoinedTeams(emptyList())
                            success("チームを全て削除しました")
                        }
                    }
                }
            }
        }
    }
}