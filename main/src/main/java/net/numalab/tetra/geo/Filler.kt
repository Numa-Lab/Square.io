package net.numalab.tetra.geo

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

fun PosSetNullable.maxX() = this.filterNotNull().maxOfOrNull { it.x }
fun PosSetNullable.maxZ() = this.filterNotNull().maxOfOrNull { it.z }
fun PosSetNullable.minX() = this.filterNotNull().minOfOrNull { it.x }
fun PosSetNullable.minZ() = this.filterNotNull().minOfOrNull { it.z }

fun fill(one: PosSet, two: PosSet): PosSet {
    val all = one + two
    val xRange = (all.minX() - 1)..(all.maxX() + 1)
    val zRange = (all.minZ() - 1)..(all.maxZ() + 1)

    val outside = fillInRange(all, xRange, zRange, Pos(all.maxX() + 1, all.maxZ() + 1))
    val inside = flipInRange(outside, all.minX()..all.maxX(), all.minZ()..all.maxZ())
    return inside
}

fun Pos.get4Relative(): PosSet {
    return listOf(
        Pos(this.x, this.z - 1),
        Pos(this.x - 1, this.z),
        Pos(this.x + 1, this.z),
        Pos(this.x, this.z + 1),
    )
}

fun Pos.get8Relative(): PosSet {
    return listOf(
        Pos(this.x, this.z - 1),
        Pos(this.x - 1, this.z - 1),
        Pos(this.x - 1, this.z),
        Pos(this.x - 1, this.z + 1),
        Pos(this.x, this.z + 1),
        Pos(this.x + 1, this.z + 1),
        Pos(this.x + 1, this.z),
        Pos(this.x + 1, this.z - 1)
    )
}

fun fillInRange(all: PosSet, xRange: IntRange, zRange: IntRange, startFrom: Pos): PosSet {
    val result = mutableListOf<Pos>()
    for (z in zRange) {
        for (x in xRange) {
            val p = Pos(x, z)
            if (p !in all) {
                result.add(p)
            } else {
                break
            }
        }

        for (x in xRange.reversed()) {
            val p = Pos(x, z)
            if (p !in all) {
                result.add(p)
            } else {
                break
            }
        }
    }

    for (x in xRange) {
        for (z in zRange) {
            val p = Pos(x, z)
            if (p !in all) {
                result.add(p)
            } else {
                break
            }
        }

        for (z in zRange.reversed()) {
            val p = Pos(x, z)
            if (p !in all) {
                result.add(p)
            } else {
                break
            }
        }
    }

    return result
}

fun flipInRange(outside: PosSet, xRange: IntRange, zRange: IntRange): PosSet {
    val out = mutableListOf<Pos>()
    for (x in xRange) {
        for (z in zRange) {
            val p = Pos(x, z)
            if (!outside.contains(p)) {
                out.add(p)
            }
        }
    }
    return out
}

fun print(pos: PosSet, name: String) {
    println("===== $name =====")
    val converted = convertFromPos(pos, pos.minX(), pos.minZ(), pos.maxX(), pos.maxZ())
    converted.forEach {
        println(it)
    }
}

@JvmName("print1n")
fun print(pos: PosSetNullable, name: String) {
    println("===== $name =====")
    val converted = convertFromPos(pos, pos.minX()!!, pos.minZ()!!, pos.maxX()!!, pos.maxZ()!!)
    converted.forEach {
        println(it)
    }
}

fun convertFromString(str: List<String>): PosSet {
    return str.mapIndexed { z, string ->
        string.mapIndexed { x, c ->
            if (c == '1') {
                Pos(x, z)
            } else {
                null
            }
        }
    }.flatten().filterNotNull()
}

fun convertFromPos(pos: PosSetNullable, startX: Int, startZ: Int, endX: Int, endZ: Int): List<String> {
    var builder = StringBuilder()
    val out = mutableListOf<String>()
    out.add("StartPos: $startX, $startZ")

    for (z in startZ..endZ) {
        for (x in startX..endX) {
            if (pos.contains(Pos(x, z))) {
                builder.append("1")
            } else {
                builder.append("0")
            }
        }

        out.add(builder.toString())
        builder = StringBuilder()
    }

    return out
}