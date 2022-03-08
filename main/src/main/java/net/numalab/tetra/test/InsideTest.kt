package net.numalab.tetra.test

import net.numalab.tetra.geo.Pos
import net.numalab.tetra.geo.PosSet
import net.numalab.tetra.geo.PosSetNullable
import net.numalab.tetra.geo.isInside

fun main() {
    InsideTest().test()
}

class InsideTest {
    val test1 = listOf(
        "0011100",
        "0110010",
        "0011100"
    )

    fun test() {
        val r1 = convertFromString(test1)
        print(r1, "test1")

        val p = Pos(6, 0)

        println("r1.isInside = ${r1.isInside(p, false)}")
    }

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