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
            NamedTextColor.RED to DyeColor.RED,
            NamedTextColor.GREEN to DyeColor.GREEN,
            NamedTextColor.BLUE to DyeColor.BLUE,
            NamedTextColor.YELLOW to DyeColor.YELLOW,
            NamedTextColor.WHITE to DyeColor.WHITE,
            NamedTextColor.GRAY to DyeColor.GRAY,
//            NamedTextColor.LIGHT_GRAY to DyeColor.LIGHT_GRAY,
            NamedTextColor.BLACK to DyeColor.BLACK,
//            NamedTextColor.PINK to DyeColor.PINK,
//            NamedTextColor.ORANGE to DyeColor.ORANGE,
//            NamedTextColor.MAGENTA to DyeColor.MAGENTA,
//            NamedTextColor.LIME to DyeColor.LIME,
//            NamedTextColor.CYAN to DyeColor.CYAN,
//            NamedTextColor.PURPLE to DyeColor.PURPLE,
//            NamedTextColor.BROWN to DyeColor.BROWN,
//            NamedTextColor.LIGHT_BLUE to DyeColor.LIGHT_BLUE,
//            NamedTextColor.GOLD to DyeColor.GOLD,
//            NamedTextColor.LIGHT_GRAY to DyeColor.LIGHT_GRAY,
//            NamedTextColor.DARK_GRAY to DyeColor.DARK_GRAY,
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