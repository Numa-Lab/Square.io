package net.numalab.tetra.test

import net.numalab.tetra.geo.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val times = 10000

fun main() {
    FillAlgorithm.values().map {
        it to FillTest().bench(it)
    }.sortedBy { it.second }.forEach {
        println("${it.first.name} : ${it.second / 1000000.0 / times.toDouble()} ms / time")
    }
}

private class FillTest {
    val currentPath = "main/src/main/java/net/numalab/tetra/test/"

    val test1 = File("${currentPath}test1.png")

    val test2 = File("${currentPath}test2.png")

    val expected = File("${currentPath}expected.png")

    fun test(algorithm: FillAlgorithm, differColor: Color = Color.MAGENTA): Boolean {
        println("Pos1 load")
        val pos1 = convertFromImage(test1)
        printIntoImage(pos1, "${algorithm.name}/pos1")
        println("Pos2 load")
        val pos2 = convertFromImage(test2)
        printIntoImage(pos2, "${algorithm.name}/pos2")

        println("Pos3 load")
        val pos3 = pos1 + pos2
        printIntoImage(pos3, "${algorithm.name}/pos1 + pos2")

        val xRange = (pos3.minX - 1)..(pos3.maxX + 1)
        val zRange = (pos3.minZ - 1)..(pos3.maxZ + 1)

        println("OutSide load")
        val outside = fillInRangeFromOutside(pos3, xRange, zRange)
        printIntoImage(outside, "${algorithm.name}/outside")
        println("Inside load")
        val inside = flipInRange(outside, pos3.minX..pos3.maxX, pos3.minZ..pos3.maxZ)
        printIntoImage(inside, "${algorithm.name}/inside")

        println("Fill")
        val fill = fill(pos1, pos2, algorithm)
        printIntoImage(fill, "${algorithm.name}/fill")

        println("Expected load")
        val expected = convertFromImage(expected)

        println("Check")
        val differFile = File("${currentPath}${algorithm.name}/differ.png")
        if (!differFile.parentFile.exists()) {
            differFile.parentFile.mkdirs()
        }
        val image = BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB)
        val g = image.graphics
        var isSame = true
        for (x in 0 until 1000) {
            for (z in 0 until 1000) {
                if (fill[x, z] != expected[x, z]) {
                    isSame = false
                    g.color = differColor
                } else {
                    g.color = Color.WHITE
                }
                g.drawLine(x, z, x, z)
            }
        }
        ImageIO.write(image, "png", differFile)

        println("Result: $isSame")
        println("END!")

        return isSame
    }

    fun bench(algorithm: FillAlgorithm): Long {
        val check = test(algorithm)
        if (!check) {
            println("Check failed: $algorithm")
            return Long.MAX_VALUE
        }

        println("Starting Benchmark for $algorithm")

        val pos1 = convertFromImage(test1)
        printIntoImage(pos1, "pos1")
        val pos2 = convertFromImage(test2)
        printIntoImage(pos2, "pos2")
        val start = System.nanoTime()
        for (i in 0..times) {
            fill(pos1, pos2, algorithm)
        }
        val end = System.nanoTime()
        println("time: ${(end - start) / 1000000.0}ms")
        println("time per: ${(end - start) / (times * 1000000.0)}ms")

        return end - start
    }

    fun convertFromImage(path: File): PosSet {
        val image = ImageIO.read(path)
        val width = image.width
        val height = image.height
        val posSet = PosSet(0, 0, width, height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixels = image.getRGB(x, y)
                if (pixels == -1) {
                    posSet[x, y, true] = 0.toByte()
                } else {
                    posSet[x, y, true] = 1.toByte()
                }
            }
        }

        posSet.updateMinMax()
        return posSet
    }

    fun printIntoImage(posSet: PosSet, name: String, toX: Int = 1000, toZ: Int = 1000) {
        printIntoImage(posSet, File("${currentPath}$name.png"), toX, toZ)
    }

    fun printIntoImage(posSet: PosSet, toWrite: File, toX: Int = posSet.maxX, toZ: Int = posSet.maxZ) {
        println("writing image to $toWrite")
        if (!toWrite.parentFile.exists()) {
            toWrite.parentFile.mkdirs()
        }

        val image = BufferedImage(toX, toZ, BufferedImage.TYPE_INT_RGB)

        val g = image.graphics

        for (x in 0 until toX) {
            for (z in 0 until toZ) {
                if (posSet[x, z] == 0.toByte()) {
                    g.color = Color.WHITE
                } else {
                    g.color = Color.BLACK
                }
                g.drawLine(x, z, x, z)
            }
        }
        ImageIO.write(image, "png", toWrite)
    }
}