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
        // ここにある色以外はサポートしない
        private val mapping = mapOf<NamedTextColor, DyeColor>(
            NamedTextColor.BLACK to DyeColor.BLACK,
            NamedTextColor.DARK_BLUE to DyeColor.BLUE,
            NamedTextColor.DARK_GREEN to DyeColor.GREEN,
            NamedTextColor.DARK_AQUA to DyeColor.LIGHT_BLUE,
            NamedTextColor.DARK_RED to DyeColor.RED,
            NamedTextColor.DARK_PURPLE to DyeColor.PURPLE,
            NamedTextColor.GOLD to DyeColor.YELLOW,
            NamedTextColor.GRAY to DyeColor.LIGHT_GRAY,
            NamedTextColor.DARK_GRAY to DyeColor.GRAY,
//            NamedTextColor.BLUE to DyeColor.BLUE,
            NamedTextColor.GREEN to DyeColor.LIME,
//            NamedTextColor.AQUA to DyeColor.CYAN,
//            NamedTextColor.RED to DyeColor.RED,
            NamedTextColor.LIGHT_PURPLE to DyeColor.MAGENTA,
//            NamedTextColor.YELLOW to DyeColor.YELLOW,
//            NamedTextColor.WHITE to DyeColor.WHITE,
        )

        /**
         * 一部の色がかけているので。
         */
        private fun getBy(textColor: TextColor): ColorHelper? {
            return mapping[textColor]?.let { ColorHelper(it, textColor) }
        }

        fun getBy(team: Team): ColorHelper? {
            return try {
                getBy(team.color())
            } catch (e: IllegalStateException) {
                null
            }
        }

        fun getBy(dye: DyeColor): ColorHelper? {
            val textColor = mapping.entries.firstOrNull { it.value == dye }?.key ?: return null
            return ColorHelper(dye, textColor)
        }

        fun random(): ColorHelper {
            val rk = mapping.keys.random()
            val rv = mapping[rk]!!

            return ColorHelper(rv, rk)
        }
    }

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