package net.numalab.tetra

import org.bukkit.*

/**
 * チームごとに羊毛、色付きガラスの準備をして地面に設置するクラス
 */
class BlockManager(val config: TetraConfig, plugin: Tetra) {
    companion object {
        private fun setColoredWoolAt(location: Location, color: DyeColor) {
            location.block.type = when (color) {
                DyeColor.WHITE -> Material.WHITE_WOOL
                DyeColor.ORANGE -> Material.ORANGE_WOOL
                DyeColor.MAGENTA -> Material.MAGENTA_WOOL
                DyeColor.LIGHT_BLUE -> Material.LIGHT_BLUE_WOOL
                DyeColor.YELLOW -> Material.YELLOW_WOOL
                DyeColor.LIME -> Material.LIME_WOOL
                DyeColor.PINK -> Material.PINK_WOOL
                DyeColor.GRAY -> Material.GRAY_WOOL
                DyeColor.LIGHT_GRAY -> Material.LIGHT_GRAY_WOOL
                DyeColor.CYAN -> Material.CYAN_WOOL
                DyeColor.PURPLE -> Material.PURPLE_WOOL
                DyeColor.BLUE -> Material.BLUE_WOOL
                DyeColor.BROWN -> Material.BROWN_WOOL
                DyeColor.GREEN -> Material.GREEN_WOOL
                DyeColor.RED -> Material.RED_WOOL
                DyeColor.BLACK -> Material.BLACK_WOOL
            }
        }

        private fun setColoredGlassAt(location: Location, dyeColor: DyeColor) {
            location.block.type = when (dyeColor) {
                DyeColor.WHITE -> Material.WHITE_STAINED_GLASS
                DyeColor.ORANGE -> Material.ORANGE_STAINED_GLASS
                DyeColor.MAGENTA -> Material.MAGENTA_STAINED_GLASS
                DyeColor.LIGHT_BLUE -> Material.LIGHT_BLUE_STAINED_GLASS
                DyeColor.YELLOW -> Material.YELLOW_STAINED_GLASS
                DyeColor.LIME -> Material.LIME_STAINED_GLASS
                DyeColor.PINK -> Material.PINK_STAINED_GLASS
                DyeColor.GRAY -> Material.GRAY_STAINED_GLASS
                DyeColor.LIGHT_GRAY -> Material.LIGHT_GRAY_STAINED_GLASS
                DyeColor.CYAN -> Material.CYAN_STAINED_GLASS
                DyeColor.PURPLE -> Material.PURPLE_STAINED_GLASS
                DyeColor.BLUE -> Material.BLUE_STAINED_GLASS
                DyeColor.BROWN -> Material.BROWN_STAINED_GLASS
                DyeColor.GREEN -> Material.GREEN_STAINED_GLASS
                DyeColor.RED -> Material.RED_STAINED_GLASS
                DyeColor.BLACK -> Material.BLACK_STAINED_GLASS
            }
        }

        fun setColoredWoolAt(location: Location, color: ColorHelper) {
            setColoredWoolAt(location, color.dye)
        }

        fun setColoredGlassAt(location: Location, color: ColorHelper) {
            setColoredGlassAt(location, color.dye)
        }
    }

    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { task() }, 1, 1)
    }

    private fun task() {
        if (config.isGoingOn.value()) {
            // 地面に設置する
            val teams = config.getJoinedTeams()
            for (team in teams) {
                val teamColor = ColorHelper.getBy(team) ?: continue

                team.entries
                    .mapNotNull { e -> Bukkit.getOnlinePlayers().find { it.name == e } }
                    .filter { it.gameMode == GameMode.SURVIVAL }
                    .forEach {
                        val bottomLocation = it.location.block.getRelative(0, -1, 0).location
                        setColoredWoolAt(bottomLocation, teamColor)  // TODO 陣地内か陣地外かでブロックを変える
                    }
            }
        }
    }
}