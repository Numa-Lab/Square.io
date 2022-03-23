package net.numalab.tetra.geo

import java.lang.Integer.min
import kotlin.math.max

/**
 * [arr] [index-z][index-x]
 */
class PosSet(private val arr: Array<ByteArray>, val startX: Int, val startZ: Int) {
    companion object {
        fun of(poses: List<Pair<Int, Int>>): PosSet {
            val minX = poses.minOf { it.first }
            val minZ = poses.minOf { it.second }
            val maxX = poses.maxOf { it.first }
            val maxZ = poses.maxOf { it.second }

            val set = PosSet(minX, minZ, maxX, maxZ)

            for (pair in poses) {
                set[pair.first, pair.second, true] = 1.toByte()
            }
            set.updateMinMax()
            return set
        }

        private fun minX(arr: Array<ByteArray>, startX: Int): Int {
            var minX = Int.MAX_VALUE
            var flag = false
            for (z in arr.indices) {
                for (x in arr[z].indices) {
                    if (arr[z][x] == 1.toByte()) {
                        if (minX > x) {
                            minX = x
                        }
                        flag = true
                        break
                    }
                }
            }

            if (!flag) {
                return startX
            }

            return minX + startX
        }

        private fun minZ(arr: Array<ByteArray>, startZ: Int): Int {
            for (z in arr.indices) {
                for (x in arr[z].indices) {
                    if (arr[z][x] != 0.toByte()) {
                        return z + startZ
                    }
                }
            }
            return startZ
        }

        private fun maxX(arr: Array<ByteArray>, startX: Int): Int {
            var maxX = 0
            for (z in arr.indices) {
                for (x in arr[z].indices.reversed()) {
                    if (arr[z][x] != 0.toByte()) {
                        if (maxX < x) {
                            maxX = x
                        }
                        break
                    }
                }
            }

            return maxX + startX
        }

        private fun maxZ(arr: Array<ByteArray>, startZ: Int): Int {
            for (z in arr.indices.reversed()) {
                for (x in arr[z].indices) {
                    if (arr[z][x] != 0.toByte()) {
                        return z + startZ
                    }
                }
            }
            return startZ
        }
    }

    constructor(
        startX: Int,
        startZ: Int,
        endX: Int,
        endZ: Int
    ) : this(Array(endZ - startZ + 1) { ByteArray(endX - startX + 1) }, startX, startZ)


    var minX: Int = minX(arr, startX)
        private set
    var minZ: Int = minZ(arr, startZ)
        private set
    var maxX: Int = maxX(arr, startX)
        private set
    var maxZ: Int = maxZ(arr, startZ)
        private set

    fun updateMinMax() {
        minX = minX(arr, startX)
        minZ = minZ(arr, startZ)
        maxX = maxX(arr, startX)
        maxZ = maxZ(arr, startZ)
    }


    /**
     * @return array entry if out of range, 0 otherwise
     * @param x x座標
     * @param z z座標
     */
    operator fun get(x: Int, z: Int): Byte {
        return getByIndex(x - startX, z - startZ)
    }

    fun getByIndex(x: Int, z: Int): Byte {
        if (z in arr.indices && x in arr[z].indices) {
            return arr[z][x]
        }
        return 0
    }

    /**
     * @param x x座標
     * @param z z座標
     * @Note [disableUpdate]をtrueにすると、[updateMinMax]を呼び出さない(超推奨)
     */
    operator fun set(x: Int, z: Int, disableUpdate: Boolean = false, value: Byte) {
        setByIndex(x - startX, z - startZ, value)
    }

    fun setAll(triples: List<Triple<Int, Int, Byte>>) {
        for (triple in triples) {
            setByIndex(triple.first - startX, triple.second - startZ, triple.third, true)
        }

        updateMinMax()
    }

    fun setByIndex(x: Int, z: Int, value: Byte, disableUpdate: Boolean = false) {
        if (z in arr.indices && x in arr[z].indices) {
            arr[z][x] = value

            if (!disableUpdate) {
                updateMinMax()
            }
        }
    }

    /**
     * @param x x座標
     * @param z z座標
     */
    operator fun contains(pos: Pair<Int, Int>): Boolean {
        return this[pos.first, pos.second] != 0.toByte()
    }

    operator fun plus(other: PosSet): PosSet {
        val maxX = max(this.maxX, other.maxX)
        val maxZ = max(this.maxZ, other.maxZ)
        val minX = min(this.minX, other.minX)
        val minZ = min(this.minZ, other.minZ)


        val newArr = PosSet(minX, minZ, maxX, maxZ)

        // Copy this to newArr
        newArr.setAll(this.getNotZeros())

        // Copy other to newArr
        newArr.setAll(other.getNotZeros())

        return newArr
    }

    operator fun minus(toMinus: PosSet): PosSet {
        if (this.minX > toMinus.maxX || this.minZ > toMinus.maxZ || this.maxX < toMinus.minX || this.maxZ < toMinus.minZ) {
            return this
        }

        val maxX = this.maxX
        val maxZ = this.maxZ
        val minX = this.minX
        val minZ = this.minZ

        val newArr = PosSet(minX, minZ, maxX, maxZ)

        // Copy this to newArr
        this.getNotZeros().forEach {
            if (!toMinus.contains(it.first to it.second)) {
                newArr[it.first, it.second, true] = it.third
            }
        }

        newArr.updateMinMax()

        return newArr
    }


    fun add(x: Int, z: Int, value: Byte, disableUpdate: Boolean = false): PosSet {
        return if ((z - startZ) in arr.indices && x - startX in arr[(z - startZ)].indices) {
            arr[z - startZ][x - startX] = value
            if (!disableUpdate) {
                updateMinMax()
            }
            this
        } else {
            // Need To Expand
            val toAdd = PosSet(x, z, x, z)
            toAdd[x, z] = value
            this + toAdd
        }
    }

    /**
     * @return Triple of (x, z, value) whose value is not 0
     */
    fun getNotZeros(): List<Triple<Int, Int, Byte>> {
        val list = mutableListOf<Triple<Int, Int, Byte>>()
        for (z in arr.indices) {
            for (x in arr[z].indices) {
                if (arr[z][x] != 0.toByte()) {
                    list.add(Triple(x + startX, z + startZ, arr[z][x]))
                }
            }
        }
        return list
    }
}

fun fill(one: PosSet, two: PosSet): PosSet {
    val all = one + two
    val xRange = (all.minX - 1)..(all.maxX + 1)
    val zRange = (all.minZ - 1)..(all.maxZ + 1)

    val outside = fillInRangeFromOutside(all, xRange, zRange)
    val inside = flipInRange(outside, all.minX..all.maxX, all.minZ..all.maxZ)
    return inside
}

fun get4Relative(x: Int, z: Int): List<Pair<Int, Int>> {
    return listOf(
        Pair(x, z - 1),
        Pair(x - 1, z),
        Pair(x + 1, z),
        Pair(x, z + 1),
    )
}

fun get8Relative(x: Int, z: Int): List<Pair<Int, Int>> {
    return listOf(
        Pair(x, z - 1),
        Pair(x - 1, z - 1),
        Pair(x - 1, z),
        Pair(x - 1, z + 1),
        Pair(x, z + 1),
        Pair(x + 1, z + 1),
        Pair(x + 1, z),
        Pair(x + 1, z - 1)
    )
}

fun fillInRangeFromOutside(all: PosSet, xRange: IntRange, zRange: IntRange): PosSet {
    val result = PosSet(xRange.first, zRange.first, xRange.last, zRange.last)
    for (z in zRange) {
        for (x in xRange) {
            val p = Pair(x, z)
            if (p !in all) {
                result[x, z, true] = 1.toByte()
            } else {
                break
            }
        }

        for (x in xRange.reversed()) {
            val p = Pair(x, z)
            if (p !in all) {
                result[x, z, true] = 1.toByte()
            } else {
                break
            }
        }
    }

    for (x in xRange) {
        for (z in zRange) {
            val p = Pair(x, z)
            if (p !in all) {
                result[x, z, true] = 1.toByte()
            } else {
                break
            }
        }

        for (z in zRange.reversed()) {
            val p = Pair(x, z)
            if (p !in all) {
                result[x, z, true] = 1.toByte()
            } else {
                break
            }
        }
    }
    result.updateMinMax()
    return result
}

fun flipInRange(outside: PosSet, xRange: IntRange, zRange: IntRange): PosSet {
    val out = PosSet(xRange.first, zRange.first, xRange.last, zRange.last)
    for (x in xRange) {
        for (z in zRange) {
            val p = Pair(x, z)
            if (p !in outside) {
                out[x, z, true] = 1.toByte()
            } else {
                out[x, z, true] = 0.toByte()
            }
        }
    }

    out.updateMinMax()
    return out
}

fun print(pos: PosSet, name: String) {
    println("===== $name =====")
    val converted = convertFromPos(pos)
    converted.forEach {
        println(it)
    }
}


fun convertFromString(str: List<String>): PosSet {
    val p = str.mapIndexed { z, string ->
        string.mapIndexed { x, c ->
            if (c == '1') {
                Pair(x, z)
            } else {
                null
            }
        }
    }.flatten().filterNotNull()

    return PosSet.of(p)
}

fun convertFromPos(pos: PosSet): List<String> {
    var builder = StringBuilder()
    val out = mutableListOf<String>()
    out.add("StartPos: ${pos.startX}, ${pos.startZ}")

    for (z in pos.startZ..pos.maxZ) {
        for (x in pos.startX..pos.maxX) {
            if (Pair(x, z) in pos) {
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