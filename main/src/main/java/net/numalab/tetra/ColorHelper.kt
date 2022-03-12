package net.numalab.tetra

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.DyeColor
import org.bukkit.scoreboard.Team

/**
 * 色関係のヘルパークラス
 */
class ColorHelper(val dye: DyeColor, val textColor: TextColor) {
    companion object {
        // TODO 手動で全種類マッピングする
        private val mapping = DyeColor.values()
            .associateBy { NamedTextColor.nearestTo(TextColor.color(it.color.red, it.color.green, it.color.blue)) }
            .also {
                if (it.keys.size != it.keys.distinct().size) {
                    // 同じ色がある
                    throw IllegalStateException("Failed to Auto Generate Color Mapping")
                }
            }

        /**
         * 一部の色がかけているので。
         */
        private fun getBy(textColor: TextColor): ColorHelper? {
            return mapping[textColor]?.let { ColorHelper(it, textColor) }
        }

        fun getBy(team: Team) = getBy(team.color())

        fun random(): ColorHelper {
            val rk = mapping.keys.random()
            val rv = mapping[rk]!!

            return ColorHelper(rv, rk)
        }
    }

    private constructor(textColor: TextColor) : this(
        mapping[textColor] ?: throw IllegalArgumentException("Unknown Color"),
        textColor
    )

    private constructor(team: Team) : this(team.color())

    fun equalByDyeColor(other: ColorHelper) = dye == other.dye

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorHelper) return false
        return equalByDyeColor(other)
    }

    override fun hashCode(): Int {
        var result = dye.hashCode()
        result = 31 * result + textColor.hashCode()
        return result
    }
}