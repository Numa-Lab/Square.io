package net.numalab.tetra

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.scoreboard.DisplaySlot.SIDEBAR
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Team

class ScoreBoardManager {
    private val scoreBoard = Bukkit.getScoreboardManager().mainScoreboard
    private fun getObj(): Objective {
        val o = scoreBoard.getObjective("Te_score")
        if (o != null) {
            return o
        }
        return scoreBoard.registerNewObjective("Te_score", "dummy", Component.text("塗りつぶし数"))
    }

    private var obj = getObj()

    fun updateScoreBoard(team: Team, s: Int) {
        obj.displaySlot = SIDEBAR
        val displayName = team.name
        val score = obj.getScore(displayName)
        score.score = s
    }

    /**
     * すべての表示を消去する
     */
    fun reset() {
        println("reset")
        obj.displaySlot = null
        obj.unregister()
        obj = getObj()
    }
}