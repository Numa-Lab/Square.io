package net.numalab.tetra.geo

/**
 * Fill関係関数だけのファイル
 */

enum class FillAlgorithm {
    FillFromOutside,
    FillFromOutSideOptimized,
    Outline,
}

fun fill(one: PosSet, two: PosSet, algorithm: FillAlgorithm = FillAlgorithm.FillFromOutSideOptimized): PosSet {
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
    val xRange = (all.minX - 1)..(all.maxX + 1)
    val zRange = (all.minZ - 1)..(all.maxZ + 1)

    val outside = fillInRangeFromOutside(all, xRange, zRange)
    return flipInRange(outside, all.minX..all.maxX, all.minZ..all.maxZ)
}

// NotWorking
private fun fillOutSideOp(base: PosSet, toAdd: PosSet): PosSet {
    val all = base + toAdd
    val xRange = (toAdd.minX - 1)..(toAdd.maxX + 1)
    val zRange = (toAdd.minZ - 1)..(toAdd.maxZ + 1)

    val outside = fillInRangeFromOutside(all, xRange, zRange)
    return flipInRange(outside, toAdd.minX..toAdd.maxX, toAdd.minZ..toAdd.maxZ) + base
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