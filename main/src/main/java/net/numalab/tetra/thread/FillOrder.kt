package net.numalab.tetra.thread

import net.numalab.tetra.BlockManager
import net.numalab.tetra.ColorHelper
import net.numalab.tetra.geo.PosSet
import net.numalab.tetra.geo.fill
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

/**
 * スレッドセーフで塗りつぶし処理内容を指定するクラス
 */
data class FillOrder(
    val line: List<Pair<Int, Int>>,
    val territory: PosSet,
    val maxBlockChangePerTick: Int // 分割するサイズ
)

/**
 * 塗りつぶし処理結果を用いてWorldに反映する処理の内容を指定するクラス
 * @note worldはNotThreadSafeであることに注意
 */
data class WorldFillOrder(
    val world: World,
    val y: Int,
    val color: ColorHelper,
    val fillOrder: FillOrder,
    val drawer: Player,
    val mapSetCallBack: (PosSet) -> Unit
)

fun WorldFillOrder.resolve(plugin: JavaPlugin) {
    WorldFillRunnable(this, plugin).runTask(plugin)
}

/**
 * execute in main thread
 */
private class WorldFillRunnable(val order: WorldFillOrder, val plugin: JavaPlugin) : BukkitRunnable() {
    override fun run() {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            order.fillOrder.resolve({ callBack(it) }, plugin)
        })
    }

    private fun callBack(p: Pair<PosSet, List<PosSet>>?) {
        if (p == null) {
            return
        } else {
            p.second.forEachIndexed { index, t ->
                plugin.server.scheduler.runTaskLater(plugin, Runnable { changeBlock(t) }, index.toLong())
            }

            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                order.mapSetCallBack(p.first)
            }, p.second.lastIndex + 1L)
        }
    }

    private fun changeBlock(toChange: PosSet) {
        toChange.getNotZeros().forEach {
            val x = it.first
            val z = it.second
            val y = order.y
            BlockManager.setColoredWoolAt(Location(order.world, x.toDouble(), y.toDouble(), z.toDouble()), order.color)
        }
    }
}

private fun FillOrder.resolve(callBack: (Pair<PosSet, List<PosSet>>?) -> Unit, plugin: JavaPlugin) {
    val p = FillRunnable(this).run()
    Bukkit.getServer().scheduler.runTask(plugin, Runnable { callBack(p) })  // Return in main thread
}

/**
 * execute in another thread
 */
private class FillRunnable(val order: FillOrder) {
    fun run(): Pair<PosSet, List<PosSet>>? {
        val linePosSet = PosSet.of(order.line)
        val toFill = fill(linePosSet, order.territory).getNotZeros()
        return if (toFill.isEmpty()) {
            null
        } else {
            PosSet.of(toFill.map { l -> l.first to l.second }) to
                    (toFill.chunked(order.maxBlockChangePerTick)
                        .map { PosSet.of(it.map { l -> Pair(l.first, l.second) }) })
        }
    }
}