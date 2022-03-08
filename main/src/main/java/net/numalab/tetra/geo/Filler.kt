package net.numalab.tetra.geo

import kotlin.math.*

class Pos(val x: Int, val z: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Pos) {
            return x == other.x && z == other.z
        }
        return false
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + z
        return result
    }
}

typealias PosSet = List<Pos>
typealias PosSetNullable = List<Pos?>

operator fun PosSet.plus(other: PosSet): PosSet {
    return this.toMutableList().apply { addAll(other) }.apply { distinct() }
}

@JvmName("plusn")
operator fun PosSetNullable.plus(other: PosSetNullable): PosSetNullable {
    return this.toMutableList().apply { addAll(other) }.apply { distinct() }
}

fun PosSetNullable.forceCast(): PosSet {
    if (this.any { it == null }) throw IllegalArgumentException("ForceCast To PosSet was unsuccessful since PosSetNullable contains null")
    return this.filterNotNull().toMutableList()
}

fun PosSetNullable.cast(): PosSet? {
    if (this.any { it == null }) {
        return null
    } else {
        return this.filterNotNull().toMutableList()
    }
}

fun PosSetNullable.safeCast(): PosSet {
    val l = this.filterNotNull()
    if (l.isEmpty()) {
        throw IllegalArgumentException("SafeCast To PosSet was unsuccessful since PosSetNullable contains all null")
    } else {
        return l
    }
}

fun PosSet.maxX() = this.maxOf { it.x }
fun PosSet.maxZ() = this.maxOf { it.z }
fun PosSet.minX() = this.minOf { it.x }
fun PosSet.minZ() = this.minOf { it.z }

fun fill(one: PosSet, two: PosSet): PosSet? {
    val rayTraced = rayTraceAll(one + two)
    if (rayTraced.any { it == null }) {
        return null
    }
    return rayTraced.forceCast().getInside(false)
}

/**
 * @param one which has force sort order and marked as sorted
 * @WIP
 */
@Deprecated("Use fill(one, two) instead")
fun fillWithForceData(one: PosSet, two: PosSet): PosSet? {
    val rayTraced = rayTraceAllWithData(one + two)
    val map = rayTraced.associate { it.second to it.first }
    return rayTraced.map { it.first }.forceCast().getInsideWithData(one.mapNotNull { map[it]!! }, false)
}

/**
 * 内側を取得
 * @param containSelf trueの場合、自身も含める
 */
//TODO おかしい
fun PosSet.getInside(containSelf: Boolean = false): PosSet? {
    val sorted = this.sort() ?: return null
    val out = mutableListOf<Pos>()
    for (x in sorted.minX()..sorted.maxX()) {
        for (z in sorted.minZ()..sorted.maxZ()) {
            if (sorted.isInside(Pos(x, z), containSelf)) {
                out.add(Pos(x, z))
            }
        }
    }
    return out
}

fun PosSet.getInsideWithData(forceData: PosSet, containSelf: Boolean = false): PosSet? {
    val sorted = this.sortWithData(forceData) ?: return null
    val out = mutableListOf<Pos>()
    for (x in sorted.minX()..sorted.maxX()) {
        for (z in sorted.minZ()..sorted.maxZ()) {
            if (sorted.isInside(Pos(x, z), containSelf)) {
                out.add(Pos(x, z))
            }
        }
    }
    return out
}

/**
 * return if the pos is inside of this PosSet or not by using winding algorithm
 */
fun PosSet.isInside(pos: Pos, containSelf: Boolean): Boolean {
    if (this.contains(pos)) {
        return containSelf
    }

    var angle = 0.0
    this.forEachIndexed { index, p ->
        val a = angle(pos, p, this[(index + 1) % this.size])
        if (a.isNaN()) {
            println("a is NaN")
        } else {
            angle += a
        }
    }
//    println(abs(angle))
    return abs(angle) >= 1.985 * Math.PI   // 誤差かなと思って1.9にしてます(理論上は2)
}

/**
 * @return [from]から[to1]と[from]から[to2]への角度(ラジアン)
 */
fun angle(from: Pos, to1: Pos, to2: Pos): Double {
    val cos = ((to1.x - from.x) * (to2.x - from.x) + (to1.z - from.z) * (to2.z - from.z)) / (
            sqrt(
                ((to1.x - from.x) * (to1.x - from.x) + (to1.z - from.z) * (to1.z - from.z)).toDouble()
            ) *
                    sqrt(
                        ((to2.x - from.x) * (to2.x - from.x) + (to2.z - from.z) * (to2.z - from.z)).toDouble()
                    )
            )

    return acos(
        min(max(cos, -1.0), 1.0)    // 誤差対策
    )
}

private fun angleDiff(one: Double, two: Double): Double {
    return ((((one - two) % Math.PI) + 1.5 * Math.PI) % Math.PI) - 0.5 * Math.PI
}

fun PosSet.distinct(): List<Pos> {
    return this.distinctBy { it.x to it.z }
}

/**
 * @return 想定外の配置であった場合はnullを含む
 */
fun rayTraceAll(poses: PosSet): PosSetNullable {
    val fromPX = rayTraceFromPX(poses)
    val fromNX = rayTraceFromNX(poses)
    val fromPZ = rayTraceFromPZ(poses)
    val fromNZ = rayTraceFromNZ(poses)
    return (fromPX + fromNX + fromPZ + fromNZ).safeCast().distinct()
}

/**
 * @return Pair#first is the pos, Pair#second is the hit pos
 */
fun rayTraceAllWithData(poses: PosSet): List<Pair<Pos, Pos>> {
    val fromPX = rayTraceFromPXWithData(poses)
    val fromNX = rayTraceFromNXWithData(poses)
    val fromPZ = rayTraceFromPZWithData(poses)
    val fromNZ = rayTraceFromNZWithData(poses)
    return (fromPX + fromNX + fromPZ + fromNZ).filterNotNull()
        .distinctBy { it.first.x to it.first.z to it.second.x to it.second.z }
}

fun rayTraceFromPX(poses: PosSet): PosSetNullable {
    return poses.map {
        for (x in poses.maxX() downTo poses.minX()) {
            if (poses.contains(Pos(x, it.z))) {
                return@map Pos(x + 1, it.z)
            }
        }
        return@map null
    }
}

fun rayTraceFromNX(poses: PosSet): PosSetNullable {
    return poses.map {
        for (x in poses.minX()..poses.maxX()) {
            if (poses.contains(Pos(x, it.z))) {
                return@map Pos(x - 1, it.z)
            }
        }
        return@map null
    }
}

fun rayTraceFromPZ(poses: PosSet): PosSetNullable {
    return poses.map {
        for (z in poses.maxZ() downTo poses.minZ()) {
            if (poses.contains(Pos(it.x, z))) {
                return@map Pos(it.x, z + 1)
            }
        }
        return@map null
    }
}

fun rayTraceFromNZ(poses: PosSet): PosSetNullable {
    return poses.map {
        for (z in poses.minZ()..poses.maxZ()) {
            if (poses.contains(Pos(it.x, z))) {
                return@map Pos(it.x, z - 1)
            }
        }
        return@map null
    }
}

fun rayTraceFromPXWithData(poses: PosSet): List<Pair<Pos, Pos>?> {
    return poses.map {
        for (x in poses.maxX() downTo poses.minX()) {
            if (poses.contains(Pos(x, it.z))) {
                return@map Pair(Pos(x + 1, it.z), Pos(x, it.z))
            }
        }
        return@map null
    }
}

fun rayTraceFromNXWithData(poses: PosSet): List<Pair<Pos, Pos>?> {
    return poses.map {
        for (x in poses.minX()..poses.maxX()) {
            if (poses.contains(Pos(x, it.z))) {
                return@map Pair(Pos(x - 1, it.z), Pos(x, it.z))
            }
        }
        return@map null
    }
}

fun rayTraceFromPZWithData(poses: PosSet): List<Pair<Pos, Pos>?> {
    return poses.map {
        for (z in poses.maxZ() downTo poses.minZ()) {
            if (poses.contains(Pos(it.x, z))) {
                return@map Pair(Pos(it.x, z + 1), Pos(it.x, z))
            }
        }
        return@map null
    }
}

fun rayTraceFromNZWithData(poses: PosSet): List<Pair<Pos, Pos>?> {
    return poses.map {
        for (z in poses.minZ()..poses.maxZ()) {
            if (poses.contains(Pos(it.x, z))) {
                return@map Pair(Pos(it.x, z - 1), Pos(it.x, z))
            }
        }
        return@map null
    }
}

/**
 * Sort and make a list of PosSet that express convex hull
 */
fun PosSet.sortConvexHull(): PosSet? {
    val minZs = this.filter { it.z == this.minZ() }
    val min = minZs.minByOrNull { it.x } ?: return null
    var A = min
    val L = mutableListOf<Pos>()
    do {
        L.add(A)
        var B = this[0]
        for (i in 1 until this.size) {
            val C = this[i]
            if (B == A) {
                B = C
            } else {
                val v = (B.x - A.x) * (C.z - A.z) - (C.x - A.x) * (B.z - A.z)
                if (v > 0 || (v == 0 && abs((C.x - A.x) * (C.x - A.x) + (C.z - A.z) * (C.z - A.z)) > abs((B.x - A.x) * (B.x - A.x) + (B.z - A.z) * (B.z - A.z)))) {
                    B = C
                }
            }
        }
        A = B
    } while (A != L[0])

    return L
}

/**
 * ソートする
 * 1.x座標が最も小さく、z座標が最小の物を取ってくる
 * 2.すべての点に対して距離が一番小さい物を取ってくる
 * 3.(2で長さが一致する場合は外積を取り、右側にあるものを選ぶ)
 * 4. 2,3の結果を保存する
 * 5. 4の結果が1の結果と一致するまで繰り返す
 */
fun PosSet.sort(): PosSet? {
    val minZs = this.filter { it.z == this.minZ() }
    val min = minZs.minByOrNull { it.x } ?: return null
    var current = min
    val result = mutableListOf<Pos>()
    do {
        result.add(current)
        val filtered = this.filter { !result.contains(it) }
        if (filtered.isEmpty()) break
        val minDis =
            filtered.minOf { (it.x - current.x) * (it.x - current.x) + (it.z - current.z) * (it.z - current.z) }
        val nearest =
            filtered.filter { (it.x - current.x) * (it.x - current.x) + (it.z - current.z) * (it.z - current.z) == minDis }  // TODO 最適化
        if (nearest.size == 1) {
            current = nearest[0]
        } else {
            current = rightest(nearest, current)
        }
    } while (current != result[0])

    return result
}

private fun rightest(list: PosSet, from: Pos): Pos {
    var rightest = list[0]
    for (index in 1..list.lastIndex) {
        val current = list[index]
        if ((rightest.x - from.x) * (current.z - from.z) - (current.x - from.x) * (rightest.z - from.z) > 0) {
            rightest = current
        }
    }
    return rightest
}

/**
 * [forceData]を強制的にソート済みとみなして扱う
 * [forceData]はこのセットに含まれているべき
 */
fun PosSet.sortWithData(forceData: PosSet): PosSet? {
    val removed = this.filter { !forceData.contains(it) }
    val first = forceData[0]
    val last = forceData[forceData.lastIndex]

    val marged = removed + first + last
    val sorted = marged.sort() ?: return null

    val firstIndex = sorted.indexOf(first)
    val lastIndex = sorted.indexOf(last)
    if (firstIndex > lastIndex) {
        return sorted.toMutableList().also {
            it.remove(last)
            it.addAll(firstIndex + 1, forceData)
        }
    } else if (firstIndex < lastIndex) {
        return sorted.toMutableList().also {
            it.remove(first)
            it.addAll(lastIndex - 1, forceData.asReversed())
        }
    } else {
        // Should not be here
        return sorted
    }
}