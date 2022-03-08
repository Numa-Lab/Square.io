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
    return rayTraced.forceCast().getInside()
}

/**
 * 内側を取得
 * @param containSelf trueの場合、自身も含める
 */
//TODO おかしい
fun PosSet.getInside(containSelf: Boolean = false): PosSet {
    val out = mutableListOf<Pos>()
    for (x in minX()..maxX()) {
        for (z in minZ()..maxZ()) {
            if (this.isInside(Pos(x, z), containSelf)) {
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
    println(abs(angle))
    return abs(angle) > 2.5 * Math.PI   // 誤差かなと思って2.5にしてます(理論上は2)
}

/**
 * @return [from]から[to1]と[from]から[to2]への角度(ラジアン)
 */
fun angle(from: Pos, to1: Pos, to2: Pos): Double {
//    val r1 = atan2((to1.x - from.x).toDouble(), (to1.z - from.z).toDouble())
//    val r2 = atan2((to2.x - from.x).toDouble(), (to2.z - from.z).toDouble())
//
//    println(
//        "[Angle]:(${to1.x - from.x},${to1.z - from.z}),(${to2.x - from.x},${to2.z - from.z}):${
//            angleDiff(
//                r1,
//                r2
//            )
//        }:${r1},${r2}"
//    )
//
//    return angleDiff(r1, r2)

    val cos = ((to1.x - from.x) * (to2.x - from.x) + (to1.z - from.z) * (to2.z - from.z)) / (
            sqrt(((to1.x - from.x) * (to1.x - from.x) + (to1.z - from.z) * (to1.z - from.z)).toDouble()) *
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