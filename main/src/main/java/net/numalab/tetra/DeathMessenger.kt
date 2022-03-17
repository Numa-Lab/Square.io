package net.numalab.tetra

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team

class DeathMessenger(plugin: JavaPlugin, private val config: TetraConfig) : Listener {
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    private val deadQueue = mutableListOf<Pair<Player, Component>>()

    @EventHandler
    private fun onDeath(e: PlayerDeathEvent) {
        val found = deadQueue.find { it.first.uniqueId == e.entity.uniqueId }
        if (found != null) {
            e.deathMessage(found.second)
        }
    }

    fun addDeadQueue(dead: Player, killer: Player?, isLastDeath: Boolean) {
        deadQueue.add(Pair(dead, getDeathMessage(dead, killer, isLastDeath)))
    }

    private fun getDeathMessage(dead: Player, killer: Player?, isLastDeath: Boolean): Component {
        if (killer != null) {
            if (isLastDeath) {
                return dead.displayName()
                    .append(Component.text("は"))
                    .append(killer.displayName())
                    .append(Component.text("によって殺された"))
                    .append(Component.space())
                    .append(Component.text("最終キル!").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
            } else {
                return dead.displayName()
                    .append(Component.text("は"))
                    .append(killer.displayName())
                    .append(Component.text("によって殺された"))
            }
        } else {
            if (isLastDeath) {
                return dead.displayName()
                    .append(Component.text("は"))
                    .append(Component.text("死んだ"))
                    .append(Component.space())
                    .append(Component.text("最終キル!").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
            } else {
                return dead.displayName()
                    .append(Component.text("は"))
                    .append(Component.text("死んだ"))
            }
        }
    }

    /**
     * @return 最後のチーム、まだ最後じゃない場合はnull
     */
    fun onTeamDeath(team: Team, score: Int): Team? {
        Bukkit.broadcast(
            Component.text("${team.name}は${score}ブロック塗りつぶして滅んだ")
                .append(Component.space())
                .append(Component.text("チームキル!").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
        )
        return checkGameEnd()
    }

    private fun checkGameEnd(): Team? {
        val notEndTeam = config.getJoinedTeams()
            .filterNot {
                it.entries.mapNotNull { e -> Bukkit.getPlayer(e) }
                    .all { p -> p.gameMode != GameMode.SURVIVAL || p.isDead }
                        || it.entries.isEmpty()
            }
        if (notEndTeam.size == 1) {
            return notEndTeam.first()
        } else if (notEndTeam.isEmpty()) {
            // なぜか勝者がいない
            // TODO 処理
        }
        return null
    }

    fun broadCastResult(remainedTeam: Pair<Team, Int>, teams: List<Pair<Team, Int>>) {
        Bukkit.broadcast(Component.text("=====ランキング====="))
        Bukkit.broadcast(Component.empty())
        (teams + remainedTeam).sortedBy { it.second }.forEachIndexed { index, pair ->
            Bukkit.broadcast(
                Component.text("[${index + 1}位]")
                    .append(Component.space())
                    .append(pair.first.displayName())
                    .append(Component.text("は${pair.second}ブロック塗りつぶして滅んだ"))
            )
        }
        Bukkit.broadcast(Component.empty())
        Bukkit.broadcast(Component.text("生き残りのチーム"))
        Bukkit.broadcast(Component.empty())
        Bukkit.broadcast(
            (remainedTeam.first.displayName().append(Component.text("が生き残りました"))).color(NamedTextColor.GOLD)
        )
    }
}