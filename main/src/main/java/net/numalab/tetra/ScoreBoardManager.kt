package net.numalab.tetra

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.scoreboard.DisplaySlot.SIDEBAR
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Team

class ScoreBoardManager {
    private val scoreBoard = Bukkit.getScoreboardManager().mainScoreboard
    private fun getScoreObj(): Objective {
        val o = scoreBoard.getObjective("Te_score")
        if (o != null) {
            return o
        }
        return scoreBoard.registerNewObjective("Te_score", "dummy", Component.text("塗りつぶし数"))
    }

    private fun getResultObj(): Objective {
        val o = scoreBoard.getObjective("Te_result")
        if (o != null) {
            return o
        }
        return scoreBoard.registerNewObjective("Te_result", "dummy", Component.text("結果"))
    }

    private var scoreObj = getScoreObj()

    fun updateScoreBoard(team: Team, s: Int) {
        scoreObj.displaySlot = SIDEBAR
        val displayName = team.name
        val score = scoreObj.getScore(displayName)
        score.score = s
    }

    private var resultObj = getResultObj()

    /**
     * 0:goingOn 1:notGoingOn
     */
    fun updateGameState(config: TetraConfig) {
        val score = resultObj.getScore("state")
        score.score = if (config.isGoingOn.value()) 0 else 1
    }

    /**
     * すべての表示を消去する
     */
    fun reset() {
        println("reset")
        scoreObj.displaySlot = null
        scoreObj.unregister()
        scoreObj = getScoreObj()
    }
}