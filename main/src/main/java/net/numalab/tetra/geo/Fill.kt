package net.numalab.tetra.geo

import java.lang.Integer.max
import java.lang.Integer.min

/**
 * Fill関係関数だけのファイル
 */

enum class FillAlgorithm {
    FillFromOutside,
    FillFromOutSideOptimized,
    Outline,
}

fun fill(one: PosSet, two: PosSet, algorithm: FillAlgorithm = FillAlgorithm.FillFromOutside): PosSet {
    return when (algorithm) {
        FillAlgorithm.FillFromOutside -> {
            fillOutSide(one, two)
        }

        FillAlgorithm.Outline -> {
            return fillOutline(one, two)
        }

        /**
         * Except the size of one is smaller than two,
         * and one contacts with two.
         */
        FillAlgorithm.FillFromOutSideOptimized -> {
            return fillOutSideOp(one, two)
        }
    }
}

private fun fillOutSide(one: PosSet, two: PosSet): PosSet {
    val all = one + two
    val xRange = (all.minX - 1)..(all.maxX)
    val zRange = (all.minZ - 1)..(all.maxZ)

    val outside = fillInRangeFromOutside(all, xRange, zRange)
    return flipInRange(outside, all.minX..all.maxX, all.minZ..all.maxZ)
}

private fun fillOutSideOp(one: PosSet, two: PosSet): PosSet {
    val oneSize = (one.maxX - one.minX + 1) * (one.maxZ - one.minZ + 1)
    val twoSize = (two.maxX - two.minX + 1) * (two.maxZ - two.minZ + 1)

    if (twoSize < oneSize) {
        return fillOutSideOp(two, one)
    }

    val xRange = (one.minX - 1)..(one.maxX + 1)
    val zRange = (one.minZ - 1)..(one.maxZ + 1)

    val oTwo = PosSet(xRange.first, zRange.first, xRange.last, zRange.last, Unit)

    for (x in max(one.minX - 1, two.minX)..min(one.maxX + 1, two.maxX)) {
        for (z in max(one.minZ - 1, two.minZ)..min(one.maxZ + 1, two.maxZ)) {
            if (two[x, z] != 0.toByte()) {
                oTwo[x, z, true] = 1.toByte()
            }
        }
    }

    oTwo.updateMinMax()

    val outside = fillInRangeFromOutside(one + oTwo, xRange, zRange)

    return flipInRange(outside, one.minX..one.maxX, one.minZ..one.maxZ) + two
}

// WIP
private fun fillOutline(one: PosSet, two: PosSet): PosSet {
    val all = one + two

    val topZ = all.minZ
    val topX = firstX(all, topZ)

    val bottomZ = all.maxZ
    val bottomX = firstX(all, bottomZ)

    val leftX = all.minX
    val leftZ = firstZ(all, leftX)

    val rightX = all.maxX
    val rightZ = firstZ(all, rightX)

    return PosSet(leftX, topZ, rightX, bottomZ, Unit)
}

private fun firstX(all: PosSet, z: Int): Int {
    var firstX = Integer.MAX_VALUE

    for (x in all.minX..all.maxX) {
        if (all[x, z] != 0.toByte()) {
            firstX = x
            break
        }
    }

    if (firstX == Integer.MAX_VALUE) {
        throw IllegalArgumentException("PosSet is expected to have updated min/max")
    }

    return firstX
}

private fun firstZ(all: PosSet, x: Int): Int {
    var firstZ = Integer.MAX_VALUE

    for (z in all.minZ..all.maxZ) {
        if (all[x, z] != 0.toByte()) {
            firstZ = z
            break
        }
    }

    if (firstZ == Integer.MAX_VALUE) {
        throw IllegalArgumentException("PosSet is expected to have updated min/max")
    }

    return firstZ
}