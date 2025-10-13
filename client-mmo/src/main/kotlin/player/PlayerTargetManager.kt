/*
 * This file is part of [GreenVale]
 *
 * [GreenVale] is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * [GreenVale] is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with [GreenVale].  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.decodesoft.player

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.items.ItemDrop
import pl.decodesoft.npc.NPCClient
import kotlin.math.abs

// Klasa zarządzająca celami gracza
class PlayerTargetManager(
    private val camera: OrthographicCamera,
    private val localPlayer: Player,
    private val players: Map<String, Player>,
    private val enemies: Map<String, EnemyClient>,
    private val npcs: Map<String, NPCClient>,
    private val droppedItems: Map<String, ItemDrop>,
    private val onTargetChanged: (Any?, String?) -> Unit
) {
    // Targetowanie
    companion object {
        private const val ENTITY_HITBOX_HALF_WIDTH = 20f    // (szerokość 30px)
        private const val ENTITY_HITBOX_HEIGHT = 70f        // wysokosc
        private const val ITEM_HITBOX_HALF_SIZE = 15f       // itemy
    }

    // Właściwości z prywatnym setterem
    private var selectedEntityId: String? = null
    private var selectedEntityType: String? = null
    private var lastPlayerTarget: Player? = null
    private var lastEnemyTarget: EnemyClient? = null
    private var lastNPCTarget: NPCClient? = null

    fun findEntityUnderCursor(): Pair<Any, String>? {
        val mouseX = Gdx.input.x
        val mouseY = Gdx.input.y

        // Konwersja współrzędnych ekranu na współrzędne świata gry
        val worldCoords = camera.unproject(Vector3(mouseX.toFloat(), mouseY.toFloat(), 0f))

        // Ograniczenie zasięgu przeszukiwania
        val maxSearchRadius = 600f
        val searchRadiusSquared = maxSearchRadius * maxSearchRadius

        // Najpierw sprawdź, czy kursor jest nad którymś z graczy
        val nearbyPlayers = players.values.filter { player ->
            player.id != localPlayer.id &&
                    Vector2.dst2(localPlayer.x, localPlayer.y, player.x, player.y) <= searchRadiusSquared
        }

        // GRACZE - PROSTOKĄTNY HITBOX (od dołu do góry)
        nearbyPlayers.find { player ->
            val dx = abs(worldCoords.x - player.x)
            val dyFromFeet = worldCoords.y - (player.y - 15f)

            dx <= ENTITY_HITBOX_HALF_WIDTH && dyFromFeet >= 0f && dyFromFeet <= ENTITY_HITBOX_HEIGHT
        }?.let {
            return Pair(it, "player")
        }

        val nearbyEnemies = enemies.values.filter { enemy ->
            val distSquared = Vector2.dst2(localPlayer.x, localPlayer.y, enemy.x, enemy.y)
            distSquared <= searchRadiusSquared
        }

        // PRZECIWNICY - PROSTOKĄTNY HITBOX (od dołu do góry)
        nearbyEnemies.find { enemy ->
            val dx = abs(worldCoords.x - enemy.x)
            val dyFromFeet = worldCoords.y - (enemy.y - 1f)

            dx <= ENTITY_HITBOX_HALF_WIDTH && dyFromFeet >= 0f && dyFromFeet <= ENTITY_HITBOX_HEIGHT
        }?.let {
            return Pair(it, "enemy")
        }

        val nearbyNPCs = npcs.values.filter { npc ->
            val distSquared = Vector2.dst2(localPlayer.x, localPlayer.y, npc.x, npc.y)
            distSquared <= searchRadiusSquared
        }

        // NPC - PROSTOKĄTNY HITBOX (od dołu do góry)
        nearbyNPCs.find { npc ->
            val dx = abs(worldCoords.x - npc.x)
            val dyFromFeet = worldCoords.y - (npc.y - 15f)

            dx <= ENTITY_HITBOX_HALF_WIDTH && dyFromFeet >= 0f && dyFromFeet <= ENTITY_HITBOX_HEIGHT
        }?.let {
            return Pair(it, "npc")
        }

        // Sprawdź itemy
        val nearbyItems = droppedItems.values.filter { item ->
            val distSquared = Vector2.dst2(localPlayer.x, localPlayer.y, item.x, item.y)
            distSquared <= searchRadiusSquared
        }

        // ITEMY - KWADRATOWY HITBOX (wyśrodkowany)
        nearbyItems.find { item ->
            val dx = abs(worldCoords.x - item.x)
            val dy = abs(worldCoords.y - item.y)
            dx <= ITEM_HITBOX_HALF_SIZE && dy <= ITEM_HITBOX_HALF_SIZE
        }?.let {
            return Pair(it, "item")
        }

        return null
    }

    fun setTarget(entity: Any?, entityType: String?) {
        // Odznacz poprzedni cel
        clearSelectionMarkers()

        // Ustaw nowy cel
        if (entity != null && entityType != null) {
            when (entityType) {
                "player" -> {
                    val player = entity as Player
                    player.isSelected = true
                    selectedEntityId = player.id
                    selectedEntityType = "player"
                    lastPlayerTarget = player
                    lastEnemyTarget = null
                    lastNPCTarget = null
                }
                "enemy" -> {
                    val enemy = entity as EnemyClient
                    enemy.isSelected = true
                    selectedEntityId = enemy.id
                    selectedEntityType = "enemy"
                    lastPlayerTarget = null
                    lastEnemyTarget = enemy
                    lastNPCTarget = null
                }
                "npc" -> {
                    val npc = entity as NPCClient
                    npc.isSelected = true
                    selectedEntityId = npc.id
                    selectedEntityType = "npc"
                    lastPlayerTarget = null
                    lastEnemyTarget = null
                    lastNPCTarget = npc
                }
                "item" -> {
                    val item = entity as ItemDrop
                    item.isSelected = true
                    selectedEntityId = item.id
                    selectedEntityType = "item"
                }
            }
        } else {
            selectedEntityId = null
            selectedEntityType = null
        }

        onTargetChanged(entity, entityType)
    }

    fun clearTarget() {
        clearSelectionMarkers()
        selectedEntityId = null
        selectedEntityType = null
        onTargetChanged(null, null)
    }

    private fun clearSelectionMarkers() {
        selectedEntityId?.let { prevId ->
            when (selectedEntityType) {
                "player" -> players[prevId]?.isSelected = false
                "enemy" -> enemies[prevId]?.isSelected = false
                "npc" -> npcs[prevId]?.isSelected = false
                "item" -> droppedItems[prevId]?.isSelected = false
            }
        }
    }
}