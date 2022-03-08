package net.numalab.tetra.test

import net.numalab.tetra.geo.Pos
import net.numalab.tetra.geo.angle

fun main() {
    AngleTest().test()
}

class AngleTest {
    fun test() {
        val from = Pos(0, 0)
        val to1 = Pos(1, 0)
        val to2 = Pos(0, 1)

        println("${angle(from, to1, to2)}")
    }
}