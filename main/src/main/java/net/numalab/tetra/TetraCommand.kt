package net.numalab.tetra

import dev.kotx.flylib.command.Command
import dev.kotx.flylib.command.Permission
import net.kunmc.lab.configlib.ConfigCommandBuilder
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandException
import org.bukkit.scoreboard.Team

class TetraCommand(val config: TetraConfig) : Command("tetra") {
    var blockManager: BlockManager? = null
    var autoSetter: AutoSetter? = null

    init {
        description("Tetra command")
        permission(Permission.OP)
        usage {
            selectionArgument("On/Off", "ON", "OFF")
            executes {
                when (typedArgs[0] as String) {
                    "ON" -> {
                        val failed = checkAllTeamColor()
                        if (failed.isNotEmpty()) {
                            fail("以下のチームの色は使用できません\n${failed.joinToString("\n") { it.name }}")
                        } else {
                            if (this@TetraCommand.config.getJoinedTeams()
                                    .filter { it.entries.mapNotNull { e -> Bukkit.getPlayer(e) }.isNotEmpty() }
                                    .size < 2
                            ) {
                                fail("[WARN]少なくともチームメンバーがいる2つ以上のチームを追加してください")
                            }
                            try {
                                Bukkit.dispatchCommand(sender, "tetra config modify isGoingOn set true")
                                this@TetraCommand.config.isGoingOn.value(true)
                            } catch (e: CommandException) {
                                fail("コンフィグファイルの変更に失敗しました")
                            }

                            autoSetter?.onStart()
                            blockManager?.reset()
                            success("ONになりました")
                        }
                    }
                    "OFF" -> {
                        try {
                            Bukkit.dispatchCommand(sender, "tetra config modify isGoingOn set false")
                        } catch (e: CommandException) {
                            fail("コンフィグファイルの変更に失敗しました")
                        }
                        this@TetraCommand.config.isGoingOn.value(false)
                        blockManager?.reset()
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

    private fun checkAllTeamColor(): List<Team> = config.getJoinedTeams().filter {
        ColorHelper.getBy(it) == null
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
                            this@TetraTeamCommand.config.removeJoinedTeam(team)
                            success("${displayTeam(team)} というチームを削除しました")
                        }
                        "clear" -> {
                            this@TetraTeamCommand.config.clearJoinedTeam()
                            success("チームを全て削除しました")
                        }
                    }
                }
            }
        }

        children()
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