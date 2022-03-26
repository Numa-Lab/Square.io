package net.numalab.tetra.test

import net.numalab.tetra.geo.PosSet
import net.numalab.tetra.geo.fill
import net.numalab.tetra.geo.fillInRangeFromOutside
import net.numalab.tetra.geo.flipInRange
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
//    FillTest().test()
    FillTest().bench()
}

private class FillTest {
    val currentPath = "main/src/main/java/net/numalab/tetra/test/"

    val test1 = File("${currentPath}test1.png")

    val test2 = File("${currentPath}test2.png")

    val expected = File("${currentPath}expected.png")

    fun test() {
        println("Pos1 load")
        val pos1 = convertFromImage(test1)
        printIntoImage(pos1, "pos1")
        println("Pos2 load")
        val pos2 = convertFromImage(test2)
        printIntoImage(pos2, "pos2")

        println("Pos3 load")
        val pos3 = pos1 + pos2
        printIntoImage(pos3, "pos1 + pos2")

        val xRange = (pos3.minX - 1)..(pos3.maxX + 1)
        val zRange = (pos3.minZ - 1)..(pos3.maxZ + 1)

        println("OutSide load")
        val outside = fillInRangeFromOutside(pos3, xRange, zRange)
        printIntoImage(outside, "outside")
        println("Inside load")
        val inside = flipInRange(outside, pos3.minX..pos3.maxX, pos3.minZ..pos3.maxZ)
        printIntoImage(inside, "inside")

        println("Fill")
        val fill = fill(pos1, pos2)
        printIntoImage(fill, "fill")

        println("Expected load")
        val expected = convertFromImage(expected)

        println("Check")
        var isSame = true
        for (x in fill.startX..fill.maxX) {
            for (z in fill.startZ..fill.maxZ) {
                if (fill[x, z] != expected[x, z]) {
                    isSame = false
                    println("$x, $z: ${fill[x, z]} != ${expected[x, z]}")
                    break
                }
            }
        }
        println("Result: $isSame")

        println("END!")
    }

    fun bench() {
        val times = 100

        val pos1 = convertFromImage(test1)
        printIntoImage(pos1, "pos1")
        val pos2 = convertFromImage(test2)
        printIntoImage(pos2, "pos2")
        val start = System.nanoTime()
        for (i in 0..times) {
            fill(pos1, pos2)
        }
        val end = System.nanoTime()
        println("time: ${(end - start) / 1000000.0}ms")
        println("time per: ${(end - start) / (times * 1000000.0)}ms")
    }

    fun convertFromImage(path: File): PosSet {
        val image = ImageIO.read(path)
        val width = image.width
        val height = image.height
        val pixels = image.raster.getPixels(0, 0, width, height, null as IntArray?)
        val posSet = PosSet(0, 0, width, height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val index = x + y * width
                if (pixels[index] == 0) {
                    posSet[x, y, true] = 0.toByte()
                } else {
                    posSet[x, y, true] = 1.toByte()
                }
            }
        }

        posSet.updateMinMax()
        return posSet
    }

    fun printIntoImage(posSet: PosSet, name: String) {
        printIntoImage(posSet, File("${currentPath}$name.png"))
    }

    fun printIntoImage(posSet: PosSet, toWrite: File) {
        println("writing image to $toWrite")

        val image =
            BufferedImage(posSet.maxX - posSet.minX + 1, posSet.maxZ - posSet.minZ + 1, BufferedImage.TYPE_INT_RGB)
        val width = image.width
        val height = image.height

        val g = image.graphics

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (posSet[x, y] == 0.toByte()) {
                    g.color = Color.WHITE
                } else {
                    g.color = Color.BLACK
                }
                g.drawLine(x, y, x, y)
            }
        }
        ImageIO.write(image, "png", toWrite)
    }
}