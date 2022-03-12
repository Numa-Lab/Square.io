package net.numalab.tetra.test

import net.numalab.tetra.geo.*

fun main() {
    FillTest().test()
//    FillTest().bench()
}

class FillTest {
    val test1 = listOf(
        "0001110000",
        "0011111000",
        "0010001000",
        "0000000000",
        "0000000000"
    )

    val test2 = listOf(
        "0000000000",
        "0000000000",
        "0001010000",
        "0000100000",
        "0000000000"
    )

    val expected = listOf(
        "0001110000",
        "0011111000",
        "0011111000",
        "0000100000",
        "0000000000"
    )

    fun test() {
        val pos1 = convertFromString(test1)
        print(pos1, "pos1")
        val pos2 = convertFromString(test2)
        print(pos2, "pos2")

        val pos3 = pos1 + pos2
        print(pos3, "pos1 + pos2")

        val xRange = (pos3.minX() - 1)..(pos3.maxX() + 1)
        val zRange = (pos3.minZ() - 1)..(pos3.maxZ() + 1)

        val outside = fillInRange(pos3, xRange, zRange, Pos(pos3.maxX() + 1, pos3.maxZ() + 1))
        print(outside, "outside")
        val inside = flipInRange(outside, pos3.minX()..pos3.maxX(), pos3.minZ()..pos3.maxZ())
        print(inside, "inside")

        val fill = fill(pos1, pos2)
        print(fill, "fill")
    }

    fun bench() {
        val times = 10000

        val pos1 = convertFromString(test1)
        print(pos1, "pos1")
        val pos2 = convertFromString(test2)
        print(pos2, "pos2")
        val start = System.nanoTime()
        for (i in 0..times) {
            fill(pos1, pos2)
        }
        val end = System.nanoTime()
        println("time: ${(end - start) / 1000000.0}ms")
        println("time per: ${(end - start) / (times * 1000000.0)}ms")
    }
}