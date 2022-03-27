package net.numalab.tetra.thread

import net.numalab.tetra.ColorHelper
import net.numalab.tetra.WorldFiller
import net.numalab.tetra.geo.FillAlgorithm
import net.numalab.tetra.geo.PosSet
import net.numalab.tetra.geo.fill
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * スレッドセーフで塗りつぶし処理内容を指定するクラス
 */
data class FillOrder(
    val line: List<Pair<Int, Int>>,
    val territory: PosSet,
    val fillAlgorithm: FillAlgorithm
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
    // Called when the calculating task is finished
    val filler: WorldFiller,
    val callBack: (PosSet) -> Unit
)

fun WorldFillOrder.resolve(plugin: JavaPlugin) {
    WorldFillRunnable(this, plugin).run(callBack)
}

/**
 * execute in main thread
 */
private class WorldFillRunnable(val order: WorldFillOrder, val plugin: JavaPlugin) {
    fun run(callBack: (PosSet) -> Unit) {
        order.fillOrder.resolve({ callBack(it, callBack) }, plugin)
    }

    private fun callBack(p: PosSet?, callBack: (PosSet) -> Unit) {
        if (p == null) {
            return
        } else {
            callBack(p)
            changeBlock(p)
        }
    }

    private fun changeBlock(toChange: PosSet) {
        val y = order.y.toDouble()
        val toAdd = toChange.getNotZeros().map { Location(order.world, it.first.toDouble(), y, it.second.toDouble()) }
        order.filler.addQueue(toAdd, order.color)
    }
}

private fun FillOrder.resolve(callBack: (PosSet?) -> Unit, plugin: JavaPlugin) {
    val p = FillRunnable(this).run()
    callBack(p)
}

/**
 * execute in another thread
 */
private class FillRunnable(val order: FillOrder) {
    fun run(): PosSet? {
        val linePosSet = PosSet.of(order.line)
        val toFill = fill(linePosSet, order.territory, order.fillAlgorithm).getNotZeros()
        return if (toFill.isEmpty()) {
            null
        } else {
            PosSet.of(toFill.map { l -> l.first to l.second })
        }
    }
}