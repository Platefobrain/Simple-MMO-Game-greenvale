package pl.decodesoft.map

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.math.floor

class GameMap(private val width: Int, val height: Int, private val tileSize: Int) {
    private val chunkPixelSize = width * tileSize // np. 120 * 16 = 1920px
    private val loadedChunks = mutableMapOf<String, ChunkData>()
    private val chunkObjects = mutableMapOf<String, MutableList<MapObject>>()

    // ==================== DANE STRUKTURALNE ====================

    private class ChunkData(
        val name: String,
        val tiles: Array<IntArray>,
        val textures: Map<Int, TileDefinition>
    )

    private class MapObject(
        val texture: Texture,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    private data class ObjectDefinition(
        val texturePath: String,
        val width: Float,
        val height: Float
    )

    private data class TileDefinition(
        val texture: Texture,
        val width: Float = 16f,
        val height: Float = 16f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f
    )

    // ==================== DEFINICJE OBIEKTÓW ====================

    private fun getObjectDefinition(objectId: Int): ObjectDefinition? {
        return when (objectId) {
            // greenshire
            1000 -> ObjectDefinition("assets/maps/tiles/greenshire/object/tree_1.png", 215f, 264f)
            1004 -> ObjectDefinition("assets/maps/tiles/greenshire/tombstone_1.png", 44f, 52f)
            1005 -> ObjectDefinition("assets/maps/tiles/greenshire/tombstone_2.png", 46f, 53f)
            1006 -> ObjectDefinition("assets/maps/tiles/greenshire/tombstone_3.png", 42f, 38f)
            1010 -> ObjectDefinition("assets/maps/tiles/greenshire/object/pedestal_1.png", 28f, 37f)
            else -> null
        }
    }

    // ==================== TEKSTURY greenshire ====================

    private fun loadGreenshireTextures(): Map<Int, TileDefinition> {
        return mapOf(
            0 to TileDefinition(Texture("assets/maps/tiles/greenshire/grass.png")),
            // tekstury scian left
            1 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_left_1.png"), 16f, 48f),
            2 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_left_2.png"), 16f, 48f),
            3 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_left_3.png"), 16f, 48f),
            4 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_right_1.png"), 16f, 48f),
            5 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_right_2.png"), 16f, 48f),
            6 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_right_3.png"), 16f, 48f),

            // tekstury scian top
            7 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_left_top.png"), 16f, 8f),
            8 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_right_top.png"), 16f, 8f),
            9 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_top_1.png"), 48f, 8f),
            10 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_top_2.png"), 48f, 8f),
            11 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_top_3.png"), 48f, 8f),

            // tekstury scian left
            12 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_left_bottom_1.png"), 16f, 64f),
            13 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_right_bottom_1.png"), 16f, 64f),
            14 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_bottom_1.png"), 64f, 64f),
            15 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_bottom_2.png"), 64f, 64f),
            16 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_bottom_3.png"), 128f, 64f),
            17 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_bottom_window.png"), 32f, 64f),
            18 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_left_bottom_2.png"), 16f, 64f),
            19 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_right_bottom_2.png"), 16f, 64f),
            20 to TileDefinition(Texture("assets/maps/tiles/greenshire/wall/wall_bottom_shadow.png"), 96f, 64f),


            72 to TileDefinition(Texture("assets/maps/tiles/greenshire/flower_1.png")),
            73 to TileDefinition(Texture("assets/maps/tiles/greenshire/flower_2.png")),
            74 to TileDefinition(Texture("assets/maps/tiles/greenshire/flower_3.png")),
            75 to TileDefinition(Texture("assets/maps/tiles/greenshire/flower_4.png")),
            76 to TileDefinition(Texture("assets/maps/tiles/greenshire/wed_1.png")),
            77 to TileDefinition(Texture("assets/maps/tiles/greenshire/wed_2.png")),
            78 to TileDefinition(Texture("assets/maps/tiles/greenshire/wed_3.png")),
            79 to TileDefinition(Texture("assets/maps/tiles/greenshire/wed_4.png")),
            80 to TileDefinition(Texture("assets/maps/tiles/greenshire/wed_5.png")),
        )
    }

    // ==================== INNE BIOMY ====================

    private fun loadForestTextures(): Map<Int, TileDefinition> {
        return mapOf(
            0 to TileDefinition(Texture("assets/maps/tiles/forest/grass.png"), 16f, 16f)
        )
    }

    private fun loadDesertTextures(): Map<Int, TileDefinition> {
        return mapOf(
            0 to TileDefinition(Texture("assets/maps/tiles/desert/sand.png"), 16f, 16f)
        )
    }

    private fun loadMountainsTextures(): Map<Int, TileDefinition> {
        return mapOf(
            0 to TileDefinition(Texture("assets/maps/tiles/mountains/stone.png"), 16f, 16f)
        )
    }

    private fun loadSwampTextures(): Map<Int, TileDefinition> {
        return mapOf(
            0 to TileDefinition(Texture("assets/maps/tiles/swamp/mud.png"), 16f, 16f)
        )
    }

    // ==================== ŁADOWANIE CHUNKÓW ====================

    fun loadFromCsv(csv: String, chunkName: String) {
        val tiles = Array(height) { IntArray(width) }
        val objects = mutableListOf<MapObject>()
        val lines = csv.trim().lines()

        for ((y, line) in lines.withIndex()) {
            val cols = line.split(",")
            for ((x, cell) in cols.withIndex()) {
                if (x < width && y < height) {
                    val value = cell.trim().toIntOrNull() ?: 0
                    val objectDef = getObjectDefinition(value)

                    if (objectDef != null) {
                        val correctedY = height - 1 - y
                        objects.add(
                            MapObject(
                                texture = Texture(objectDef.texturePath),
                                x = x * tileSize.toFloat() - objectDef.width / 2 + tileSize / 2,
                                y = correctedY * tileSize.toFloat(),
                                width = objectDef.width,
                                height = objectDef.height
                            )
                        )
                        tiles[y][x] = 0
                    } else {
                        tiles[y][x] = value
                    }
                }
            }
        }

        val textures = when (chunkName) {
            "greenshire" -> loadGreenshireTextures()
            "forest" -> loadForestTextures()
            "desert" -> loadDesertTextures()
            "mountains" -> loadMountainsTextures()
            "swamp" -> loadSwampTextures()
            else -> loadGreenshireTextures()
        }

        loadedChunks[chunkName] = ChunkData(chunkName, tiles, textures)
        chunkObjects[chunkName] = objects
        println("✓ Załadowano chunk klienta: $chunkName (obiektów: ${objects.size})")
    }

    // ==================== MAPOWANIE POZYCJI ====================

    private fun getChunkName(chunkX: Int, chunkY: Int): String? {
        return when {
            chunkX == 0 && chunkY == 0 -> "greenshire"
            chunkX == -1 && chunkY == 0 -> "forest"
            chunkX == 1 && chunkY == 0 -> "desert"
            chunkX == 0 && chunkY == 1 -> "mountains"
            chunkX == 0 && chunkY == -1 -> "swamp"
            else -> null
        }
    }

    // ==================== RYSOWANIE ====================

    fun draw(batch: SpriteBatch, playerX: Float, playerY: Float) {
        val playerChunkX = floor(playerX / chunkPixelSize).toInt()
        val playerChunkY = floor(playerY / chunkPixelSize).toInt()

        for (dy in -1..1) {
            for (dx in -1..1) {
                val chunkX = playerChunkX + dx
                val chunkY = playerChunkY + dy
                val chunkName = getChunkName(chunkX, chunkY) ?: continue
                val chunk = loadedChunks[chunkName] ?: continue
                val offsetX = chunkX * chunkPixelSize.toFloat()
                val offsetY = chunkY * chunkPixelSize.toFloat()
                drawChunk(batch, chunk, offsetX, offsetY)
            }
        }
    }

    fun drawObjects(batch: SpriteBatch, playerX: Float, playerY: Float) {
        val playerChunkX = floor(playerX / chunkPixelSize).toInt()
        val playerChunkY = floor(playerY / chunkPixelSize).toInt()

        for (dy in -1..1) {
            for (dx in -1..1) {
                val chunkX = playerChunkX + dx
                val chunkY = playerChunkY + dy
                val chunkName = getChunkName(chunkX, chunkY) ?: continue
                val offsetX = chunkX * chunkPixelSize.toFloat()
                val offsetY = chunkY * chunkPixelSize.toFloat()
                drawChunkObjects(batch, chunkName, offsetX, offsetY)
            }
        }
    }

    private fun drawChunk(batch: SpriteBatch, chunk: ChunkData, offsetX: Float, offsetY: Float) {
        val baseTile = chunk.textures[0]
        baseTile?.let {
            for (y in 0 until height) {
                val correctedY = height - 1 - y
                for (x in 0 until width) {
                    batch.draw(
                        it.texture,
                        x * tileSize.toFloat() + offsetX,
                        correctedY * tileSize.toFloat() + offsetY,
                        it.width,
                        it.height
                    )
                }
            }
        }

        data class TileInfo(val x: Int, val y: Int, val tileDef: TileDefinition)
        val tilesToDraw = mutableListOf<TileInfo>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val textureId = chunk.tiles[y][x]
                if (textureId != 0) {
                    chunk.textures[textureId]?.let { tileDef ->
                        tilesToDraw.add(TileInfo(x, y, tileDef))
                    }
                }
            }
        }

        tilesToDraw.sortBy { it.tileDef.height }
        tilesToDraw.forEach { tile ->
            val correctedY = height - 1 - tile.y
            batch.draw(
                tile.tileDef.texture,
                tile.x * tileSize.toFloat() + offsetX + tile.tileDef.offsetX,
                correctedY * tileSize.toFloat() + offsetY + tile.tileDef.offsetY,
                tile.tileDef.width,
                tile.tileDef.height
            )
        }
    }

    private fun drawChunkObjects(batch: SpriteBatch, chunkName: String, offsetX: Float, offsetY: Float) {
        val objects = chunkObjects[chunkName] ?: return
        objects.forEach { obj ->
            batch.draw(
                obj.texture,
                obj.x + offsetX,
                obj.y + offsetY,
                obj.width,
                obj.height
            )
        }
    }

    // ==================== ZWALNIANIE PAMIĘCI ====================

    fun dispose() {
        // Zwolnienie tekstur wszystkich chunków
        loadedChunks.values.forEach { chunk ->
            chunk.textures.values.forEach { tileDef ->
                tileDef.texture.dispose()
            }
        }
        loadedChunks.clear()

        // Zwolnienie tekstur wszystkich obiektów
        chunkObjects.values.forEach { objects ->
            objects.forEach { obj ->
                obj.texture.dispose()
            }
        }
        chunkObjects.clear()
    }
}