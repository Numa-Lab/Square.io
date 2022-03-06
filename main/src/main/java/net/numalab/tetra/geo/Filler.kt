package net.numalab.tetra.geo

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

    for (z in minZ()..maxZ()) {
        var isInside = false
        var isLastContained = false

        for (x in minX()..maxX()) {
            if (this.contains(Pos(x, z))) {
                if (!isLastContained) {
                    isInside = !isInside
                }
                isLastContained = true
                if (containSelf) {
                    out.add(Pos(x, z))
                }
            } else {
                isLastContained = false
                if (isInside) {
                    out.add(Pos(x, z))
                }
            }
        }
    }

    return out
}

/**
 * @return 想定外の配置であった場合はnullを含む
 */
fun rayTraceAll(poses: PosSet): PosSetNullable {
    val fromPX = rayTraceFromPX(poses)
    val fromNX = rayTraceFromNX(poses)
    val fromPZ = rayTraceFromPZ(poses)
    val fromNZ = rayTraceFromNZ(poses)
    return fromPX + fromNX + fromPZ + fromNZ
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

private fun rayTraceFromNX(poses: PosSet): PosSetNullable {
    return poses.map {
        for (x in poses.minX()..poses.maxX()) {
            if (poses.contains(Pos(x, it.z))) {
                return@map Pos(x - 1, it.z)
            }
        }
        return@map null
    }
}

private fun rayTraceFromPZ(poses: PosSet): PosSetNullable {
    return poses.map {
        for (z in poses.maxZ() downTo poses.minZ()) {
            if (poses.contains(Pos(it.x, z))) {
                return@map Pos(it.x, z + 1)
            }
        }
        return@map null
    }
}

private fun rayTraceFromNZ(poses: PosSet): PosSetNullable {
    println("rayTraceFromNZ")
    return poses.map {
        for (z in poses.minZ()..poses.maxZ()) {
            if (poses.contains(Pos(it.x, z))) {
                return@map Pos(it.x, z - 1)
            }
        }
        return@map null
    }
}