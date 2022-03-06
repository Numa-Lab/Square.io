package net.numalab.tetra.test

import net.numalab.tetra.geo.*

fun main() {
    FillTest().test()
}

class FillTest {
    val test1 = listOf(
        "0000111011000",
        "0000101010000",
        "0000101010000",
        "0000000000000",
        "0000000000000"
    )

    val test2 = listOf(
        "0000000000000",
        "0000000000000",
        "0000000000000",
        "0000100010000",
        "0000011100000"
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


        val rayTraced = rayTraceAll(pos3)
        print(rayTraced, "rayTraced")

        val rayTracePX = rayTraceFromPX(pos3)
        print(rayTracePX, "rayTracePX")

        val rayTraceSafe = rayTraced.cast()

        if (rayTraceSafe != null) {
            print(rayTraceSafe, "rayTraceSafe")

            val inside = rayTraceSafe.getInside()
            print(inside, "inside")
        }


//        val toFill = fill(pos1, pos2)
//        print(toFill, "toFill")
    }

    private fun print(pos: PosSet, name: String) {
        println("===== $name =====")
        val converted = convertFromPos(pos, 13, 5)
        converted.forEach {
            println(it)
        }
    }

    @JvmName("print1n")
    private fun print(pos: PosSetNullable, name: String) {
        println("===== $name =====")
        val converted = convertFromPos(pos, 13, 5)
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