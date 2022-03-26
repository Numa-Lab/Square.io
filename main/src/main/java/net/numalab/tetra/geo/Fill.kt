package net.numalab.tetra.geo

/**
 * Fill関係関数だけのファイル
 */

fun fill(one: PosSet, two: PosSet): PosSet {
    val all = one + two
    val xRange = (all.minX - 1)..(all.maxX + 1)
    val zRange = (all.minZ - 1)..(all.maxZ + 1)

    val outside = fillInRangeFromOutside(all, xRange, zRange)
    val inside = flipInRange(outside, all.minX..all.maxX, all.minZ..all.maxZ)
    return inside
}