package net.numalab.tetra.geo

import org.bukkit.Location
import org.bukkit.block.BlockFace

class BlockLocation(val loc: Location) {
    init {
        if (loc.block.location != loc) throw IllegalArgumentException("Location is not block location")
    }

    fun getAdded(x: Int, z: Int): BlockLocation {
        return BlockLocation(this.loc.clone().add(x.toDouble(), 0.0, z.toDouble()))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is BlockLocation) {
            return loc.blockX == other.loc.blockX && loc.blockZ == other.loc.blockZ && loc.blockY == other.loc.blockY
        }
        return false
    }
}

/**
 * 線分
 */
class Stroke(val entry: List<BlockLocation>) {
    private val xSizeInfo = calcXSize()
    private val zSizeInfo = calcZSize()
    val xSize = xSizeInfo.first
    val zSize = zSizeInfo.first
    val xMin = xSizeInfo.second.first
    val xMax = xSizeInfo.second.second
    val zMin = zSizeInfo.second.first
    val zMax = zSizeInfo.second.second

    private fun calcXSize(): Pair<Int, Pair<Int?, Int?>> {
        val xMax = entry.maxOfOrNull { it.loc.block.x }
        val xMin = entry.minOfOrNull { it.loc.block.x }

        if (xMax == null || xMin == null) return Pair(0, Pair(null, null))

        return Pair(xMax - xMin + 1, Pair(xMin, xMax))
    }

    private fun calcZSize(): Pair<Int, Pair<Int?, Int?>> {
        val zMax = entry.maxOfOrNull { it.loc.block.z }
        val zMin = entry.minOfOrNull { it.loc.block.z }

        if (zMax == null || zMin == null) return Pair(0, Pair(null, null))

        return Pair(zMax - zMin + 1, Pair(zMin, zMax))
    }

    operator fun contains(loc: BlockLocation): Boolean {
        return entry.contains(loc)
    }

    operator fun get(index: Int): BlockLocation {
        return entry[index]
    }

    operator fun get(x: Int, z: Int): BlockLocation? {
        return entry.find { it.loc.block.x == x && it.loc.block.z == z }
    }

    fun size(): Int {
        return entry.size
    }

    fun lastIndex(): Int {
        return entry.lastIndex
    }
}

/**
 * ぬられている範囲
 */
class FilledArea(val outline: Stroke) {
    operator fun contains(loc: BlockLocation): Boolean {
        return outline.contains(loc)
    }
}

/**
 * 囲まれた部分を塗る
 * @param stroke 始点、終点が[FilledArea]に隣接していなければいけない
 */
fun FilledArea.fill(stroke: Stroke): List<BlockLocation>? {
    if (checkRelative(stroke[0]) && checkRelative(stroke[stroke.lastIndex()])) {
        val startRelative = stroke[0].getRelatives4()
        val outLineStart = this.outline.entry.find { startRelative.contains(it) }
        val endRelative = stroke[stroke.lastIndex()].getRelatives4()
        val outLineEnd = this.outline.entry.find { endRelative.contains(it) }

        if (outLineStart == null || outLineEnd == null) return null // 接していた気がするが接していない気がする
        val outLineStartIndex = this.outline.entry.indexOf(outLineStart)
        val outLineEndIndex = this.outline.entry.indexOf(outLineEnd)
        if (outLineStartIndex == -1 || outLineEndIndex == -1) return null // 接していた気がするが接していない気がする

        val outLineRelative = this.outline.entry.subList(outLineStartIndex, outLineEndIndex)

        val ray = stroke.rayTraceAll()
        val connected = Stroke(listOf(*ray.entry.toTypedArray(), *outLineRelative.toTypedArray()))
        val rayFilled = FilledArea(connected)
        return rayFilled.fillSelf()
    } else {
        // 論外
        return null
    }
}

/**
 * 上下左右から見てって最初に見つかるもののひとつ前を返す
 */
fun Stroke.rayTraceAll(): Stroke {
    val fromEast = this.rayTrace(BlockFace.EAST)
    val fromWest = this.rayTrace(BlockFace.WEST)
    val fromNorth = this.rayTrace(BlockFace.NORTH)
    val fromSouth = this.rayTrace(BlockFace.SOUTH)
    return Stroke(
        listOf(
            *fromEast.toTypedArray(),
            *fromWest.toTypedArray(),
            *fromNorth.toTypedArray(),
            *fromSouth.toTypedArray()
        )
    )
}

/**
 * 指定方向から見てって最初に見つかるもののひとつ前を返す
 */
fun Stroke.rayTrace(from: BlockFace): List<BlockLocation> {
    return when (from) {
        BlockFace.NORTH -> {
            if (this.zMax != null && this.zMin != null && this.xMax != null && this.xMin != null) {
                val startZ = this.zMax + 1 // 始点
                val xRange = this.xMin..this.xMax
                xRange.mapNotNull { x ->
                    for (z in startZ..this.zMin) {
                        if (this[x, z] != null) {
                            return@mapNotNull BlockLocation(
                                Location(
                                    this[0].loc.world,
                                    x.toDouble(), this[0].loc.blockY.toDouble(), (z + 1).toDouble()
                                )
                            )
                        }
                    }

                    return@mapNotNull null // ここには来ないはず
                }
            }

            return emptyList()
        }
        BlockFace.SOUTH -> {
            if (this.zMax != null && this.zMin != null && this.xMax != null && this.xMin != null) {
                val startZ = this.zMin - 1 // 始点
                val xRange = this.xMin..this.xMax
                xRange.mapNotNull { x ->
                    for (z in startZ..this.zMax) {
                        if (this[x, z] != null) {
                            return@mapNotNull BlockLocation(
                                Location(
                                    this[0].loc.world,
                                    x.toDouble(), this[0].loc.blockY.toDouble(), (z - 1).toDouble()
                                )
                            )
                        }
                    }

                    return@mapNotNull null // ここには来ないはず
                }
            }

            return emptyList()
        }
        BlockFace.EAST -> {
            if (this.zMax != null && this.zMin != null && this.xMax != null && this.xMin != null) {
                val startX = this.xMin - 1 // 始点
                val zRange = this.zMin..this.zMax
                zRange.mapNotNull { z ->
                    for (x in startX..this.xMax) {
                        if (this[x, z] != null) {
                            return@mapNotNull BlockLocation(
                                Location(
                                    this[0].loc.world,
                                    (x - 1).toDouble(), this[0].loc.blockY.toDouble(), z.toDouble()
                                )
                            )
                        }
                    }

                    return@mapNotNull null // ここには来ないはず
                }
            }

            return emptyList()
        }
        BlockFace.WEST -> {
            if (this.zMax != null && this.zMin != null && this.xMax != null && this.xMin != null) {
                val startX = this.xMax + 1 // 始点
                val zRange = this.zMin..this.zMax
                zRange.mapNotNull { z ->
                    for (x in startX..this.xMin) {
                        if (this[x, z] != null) {
                            return@mapNotNull BlockLocation(
                                Location(
                                    this[0].loc.world,
                                    (x + 1).toDouble(), this[0].loc.blockY.toDouble(), z.toDouble()
                                )
                            )
                        }
                    }

                    return@mapNotNull null // ここには来ないはず
                }
            }

            return emptyList()
        }
        else -> emptyList()
    }
}

/**
 * FilledAreaの自分自身を塗る
 * @param containOutline 境界線を含めるかどうか
 */
fun FilledArea.fillSelf(containOutline: Boolean = false): List<BlockLocation> {
    // 境界線すべてに対して到達済みかどうか(Bool)
    val flag = outline.entry.associateWith { false }.toMutableMap()
    val insides = mutableListOf<BlockLocation>()

    // 境界線を走査
    for (index in 0 until flag.size) {
        val (loc, bool) = flag.entries.elementAt(index)
        if (bool) continue // 到達済みならスキップ

        val relatives = loc.getRelatives4().filter { !flag.keys.contains(it) }
        val inside = relatives.filter { isInside(it) && !insides.contains(it) }
        insides.addAll(inside)
    }

    if (insides.isEmpty()) return emptyList()
    return insides.also { it.addAll(FilledArea(Stroke(insides)).fillSelf(containOutline)) }
}

/**
 * 例のあれで判定する
 */
fun FilledArea.isInside(loc: BlockLocation): Boolean {
    return if (this.outline.xMax != null) {
        var count = 0
        for (x in loc.loc.blockX..this.outline.xMax) {
            if (this.outline[x, loc.loc.blockZ] != null) {
                count++
            }
        }
        count % 2 != 0
    } else {
        false
    }
}

/**
 * @return [FilledArea]に隣接しているかどうか
 */
private fun FilledArea.checkRelative(point: BlockLocation): Boolean {
    return point.getRelatives8().any { it in this.outline }
}

fun BlockLocation.getRelatives8(): List<BlockLocation> {
    return listOf(
        this.getAdded(0, -1),
        this.getAdded(0, 1),
        this.getAdded(-1, 0),
        this.getAdded(-1, -1),
        this.getAdded(-1, 1),
        this.getAdded(1, 0),
        this.getAdded(1, -1),
        this.getAdded(1, 1)
    )
}

fun BlockLocation.getRelatives4(): List<BlockLocation> {
    return listOf(
        this.getAdded(0, -1),
        this.getAdded(0, 1),
        this.getAdded(-1, 0),
        this.getAdded(1, 0)
    )
}

/**
 * [BlockLocation]から[filter]を満たしているものを探して自動的に探索する
 */
fun BlockLocation.autoSelect(
    filter: (Location) -> Boolean,
    checked: List<BlockLocation> = mutableListOf()
): List<BlockLocation> {
    var out = mutableListOf(this)

    val re = this.getRelatives4().filter { filter(it.loc) && !checked.contains(it) }

    out.addAll(re)

    re.forEach {
        out.addAll(it.autoSelect(filter, out))
        out = forceDistinct(out)
    }

    return out
}

private fun forceDistinct(from: List<BlockLocation>): MutableList<BlockLocation> {
    val out = mutableListOf<BlockLocation>()
    from.forEach {
        if (!out.contains(it)) {
            out.add(it)
        }
    }
    return out
}

/**
 * [BlockLocation]から[filter]を満たしているものを探して自動的に探索して、そのoutlineを返す
 */
fun BlockLocation.autoSelectOutline(filter: (Location) -> Boolean): FilledArea {
    val selected = this.autoSelect(filter)
    val outLine = selected.filter { it.getRelatives4().any { r -> !filter(r.loc) } }
    return FilledArea(Stroke(outLine))
}