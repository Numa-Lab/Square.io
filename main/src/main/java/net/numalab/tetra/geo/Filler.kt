package net.numalab.tetra.geo

import java.lang.Integer.min
import kotlin.math.max

/**
 * [arr] [index-z][index-x]
 */
class PosSet(
    private val arr: Array<ByteArray>,
    val startX: Int,
    val startZ: Int,
    cMinX: Int = minX(arr, startX),
    cMinZ: Int = minZ(arr, startZ),
    cMaxX: Int = maxX(arr, startX),
    cMaxZ: Int = maxZ(arr, startZ)
) {
    companion object {
        fun of(poses: List<Pair<Int, Int>>): PosSet {
            val minX = poses.minOf { it.first }
            val minZ = poses.minOf { it.second }
            val maxX = poses.maxOf { it.first }
            val maxZ = poses.maxOf { it.second }

            val set = PosSet(minX, minZ, maxX, maxZ, Unit)

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

    constructor(
        startX: Int,
        startZ: Int,
        endX: Int,
        endZ: Int,
        disableUpdate: Unit
    ) : this(Array(endZ - startZ + 1) { ByteArray(endX - startX + 1) }, startX, startZ, startX, startZ, endX, endZ)


    var minX: Int = cMinX
        private set
    var minZ: Int = cMinZ
        private set
    var maxX: Int = cMaxX
        private set
    var maxZ: Int = cMaxZ
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
        return if (isInIndexRange(x, z)) {
            arr[z - startZ][x - startX]
        } else {
            0
        }
    }

    /**
     * @param x x座標
     * @param z z座標
     * @Note [disableUpdate]をtrueにすると、[updateMinMax]を呼び出さない(超推奨)
     */
    operator fun set(x: Int, z: Int, disableUpdate: Boolean, value: Byte) {
        if (isInIndexRange(x, z)) {
            arr[z - startZ][x - startX] = value

            if (!disableUpdate) {
                updateMinMax()
            }
        }
    }

    private fun isInIndexRange(x: Int, z: Int): Boolean {
        return 0 <= z - startZ && z - startZ < arr.size && 0 <= x - startX && x - startX < arr[z - startZ].size
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


        val newArr = PosSet(minX, minZ, maxX, maxZ, Unit)

        val zero = 0.toByte()

        for (z in minZ..maxZ) {
            for (x in minX..maxX) {
                newArr[x, z, true] = if (this[x, z] != zero || other[x, z] != zero) 1.toByte() else zero
            }
        }

        newArr.updateMinMax()
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

        val newArr = PosSet(minX, minZ, maxX, maxZ, Unit)

        // Copy this to newArr
        for (z in minZ..maxZ) {
            for (x in minX..maxX) {
                newArr[x, z, true] =
                    if (this[x, z] != 0.toByte() && toMinus[x, z] == 0.toByte()) 1.toByte() else 0.toByte()
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
            val toAdd = PosSet(x, z, x, z, Unit)
            toAdd[x, z, true] = value
            this + toAdd
        }
    }

    /**
     * @return Triple of (x, z, value) whose value is not 0
     * @note this function is not efficient,so use it only when you need to get exactly value
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

    fun clone(): PosSet {
        val newArr = PosSet(startX, startZ, maxX, maxZ, Unit)
        for (z in newArr.minZ..newArr.maxZ) {
            for (x in newArr.minX..newArr.maxX) {
                newArr[x, z, true] = this[x, z]
            }
        }

        newArr.updateMinMax()
        return newArr
    }
}

fun fillInRangeFromOutside(all: PosSet, xRange: IntRange, zRange: IntRange): PosSet {
    val result = PosSet(xRange.first, zRange.first, xRange.last, zRange.last, Unit)
    for (z in zRange) {
        for (x in xRange) {
            if (all[x, z] != 0.toByte()) {
                break
            } else {
                result[x, z, true] = 1.toByte()
            }
        }

        for (x in xRange.reversed()) {
            if (all[x, z] != 0.toByte()) {
                break
            } else {
                result[x, z, true] = 1.toByte()
            }
        }
    }

    for (x in xRange) {
        for (z in zRange) {
            if (all[x, z] != 0.toByte()) {
                break
            } else {
                result[x, z, true] = 1.toByte()
            }
        }

        for (z in zRange.reversed()) {
            if (all[x, z] != 0.toByte()) {
                break
            } else {
                result[x, z, true] = 1.toByte()
            }
        }
    }
    result.updateMinMax()
    return result
}

fun flipInRange(outside: PosSet, xRange: IntRange, zRange: IntRange): PosSet {
    val out = PosSet(xRange.first, zRange.first, xRange.last, zRange.last, Unit)
    for (x in xRange) {
        for (z in zRange) {
            if (outside[x, z] == 0.toByte()) {
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