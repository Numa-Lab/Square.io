package net.numalab.tetra.test

import net.numalab.tetra.geo.*

fun main() {
    FillTest().test()
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

    private fun convertFromString(str: List<String>): PosSet {
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

    private fun convertFromPos(pos: PosSetNullable, width: Int, height: Int): List<String> {
        var builder = StringBuilder()
        val out = mutableListOf<String>()

        for (z in 0 until height) {
            for (x in 0 until width) {
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

    private fun print(pos: PosSet, name: String) {
        println("===== $name =====")
        val converted = convertFromPos(pos, test1[0].length, test1.size)
        converted.forEach {
            println(it)
        }
    }

    @JvmName("print1n")
    private fun print(pos: PosSetNullable, name: String) {
        println("===== $name =====")
        val converted = convertFromPos(pos, test1[0].length, test1.size)
        converted.forEach {
            println(it)
        }
    }

    @JvmName("print1")
    private fun print(str: List<String>, name: String) {
        println("===== $name =====")
        str.forEach {
            println(it)
        }
    }
}