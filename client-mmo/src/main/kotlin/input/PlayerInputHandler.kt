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

package pl.decodesoft.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import pl.decodesoft.items.ItemDrop
import pl.decodesoft.msg.ChatSystem
import pl.decodesoft.player.Player
import pl.decodesoft.player.PlayerTargetManager
import pl.decodesoft.ui.GameUI
import kotlin.math.abs
import kotlin.reflect.KFunction0

class PlayerInputHandler(
    private val camera: OrthographicCamera,
    private val uiCamera: OrthographicCamera,
    private val targetManager: PlayerTargetManager,
    private val onMoveRequested: (Float, Float) -> Unit,
    private val onControlKeyPressed: KFunction0<Unit>,
    private val onAttackRequested: (Any, String) -> Unit,
    private val onItemClick: (String, Float, Float) -> Unit,
    private val onPathfindRequested: (Int, Int, Int, Int) -> Unit,
    private val menuToggle: () -> Unit,
    private val isMenuVisible: () -> Boolean,
    private val getDroppedItems: () -> Map<String, ItemDrop>,
    private val getLocalPlayer: () -> Player,
    private val gameUI: GameUI,
    private val chatSystem: ChatSystem
) {
    // ESC handling
    private var wasEscDown = false
    private var lastToggleTime = 0L
    private val toggleCooldown = 100L

    fun handleInput(): Boolean {
        // Chat
        if (chatSystem.handleInput()) {
            return true
        }

        // === OBSŁUGA ESC ===
        val escCurrentlyDown = Gdx.input.isKeyPressed(Input.Keys.ESCAPE)
        if (wasEscDown && !escCurrentlyDown) {
            if (TimeUtils.timeSinceMillis(lastToggleTime) > toggleCooldown) {
                menuToggle()
                lastToggleTime = TimeUtils.millis()
            }
        }
        wasEscDown = escCurrentlyDown

        // Jeśli menu widoczne, blokuj resztę inputu
        if (isMenuVisible()) {
            return true
        }

        // === KLAWISZ C ===
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            gameUI.toggleCharacterWindow()
            return true
        }

        // === KLAWISZ I ===
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            gameUI.toggleInventory()
            return true
        }

        // === KLAWISZ P - PATHFINDING ===
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            val localPlayer = getLocalPlayer()
            val startX = (localPlayer.x / 16f).toInt()
            val startY = (localPlayer.y / 16f).toInt()
            onPathfindRequested(startX, startY, 10, 10)
            return true
        }

        // === CONTROL KEYS ===
        if (handleControlKeys()) {
            return true
        }

        // === PRAWY PRZYCISK MYSZY - ITEMY LUB ATAK/RUCH ===
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            val mouseWorldPos = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

            // Sprawdź czy kliknięto na item
            val clickedItem = getDroppedItems().values.find { item ->
                val dx = abs(mouseWorldPos.x - item.x)
                val dy = abs(mouseWorldPos.y - item.y)
                dx <= 15f && dy <= 15f
            }

            if (clickedItem != null) {
                onItemClick(clickedItem.id, clickedItem.x, clickedItem.y)
                return true
            }

            // Jeśli nie kliknięto itemu, normalny atak/ruch
            return handleRightMouseButton()
        }

        // === LEWY PRZYCISK MYSZY ===
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            val mouseX = Gdx.input.x.toFloat()
            val mouseY = Gdx.input.y.toFloat()
            val adjustedY = uiCamera.viewportHeight - mouseY

            // UI Buttons
            if (gameUI.isClickOnCharacterButton(mouseX, adjustedY)) {
                gameUI.toggleCharacterWindow()
                return true
            }

            if (gameUI.isClickOnInventoryButton(mouseX, adjustedY)) {
                gameUI.toggleInventory()
                return true
            }

            if (gameUI.isClickOnChatButton(mouseX, adjustedY)) {
                if (chatSystem.getCurrentMode().name != "CHAT") {
                    chatSystem.switchMode()
                }
                return true
            }

            if (gameUI.isClickOnLogButton(mouseX, adjustedY)) {
                if (chatSystem.getCurrentMode().name != "LOG") {
                    chatSystem.switchMode()
                }
                return true
            }

            gameUI.handleCharacterWindowClick(mouseX, adjustedY, false)
            gameUI.handleInventoryClick(mouseX, adjustedY, false)

            return handleLeftMouseButton()
        }

        return false
    }

    private fun handleControlKeys(): Boolean {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            gameUI.toggleFPS()
            return true
        }

        for (key in arrayOf(Input.Keys.Q, Input.Keys.W, Input.Keys.E, Input.Keys.R,
            Input.Keys.SPACE, Input.Keys.NUM_1, Input.Keys.NUM_2)) {
            if (Gdx.input.isKeyJustPressed(key)) {
                onControlKeyPressed()
                return true
            }
        }
        return false
    }

    private fun handleLeftMouseButton(): Boolean {
        val entity = targetManager.findEntityUnderCursor()

        if (entity != null) {
            val (target, entityType) = entity
            targetManager.setTarget(target, entityType)
        } else {
            targetManager.clearTarget()
        }

        return true
    }

    private fun handleRightMouseButton(): Boolean {
        try {
            val entity = targetManager.findEntityUnderCursor()

            if (entity != null) {
                val (target, entityType) = entity
                targetManager.setTarget(target, entityType)
                onAttackRequested(target, entityType)
                return true
            } else {
                try {
                    val worldCoords = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
                    onMoveRequested(worldCoords.x, worldCoords.y)
                    return true
                } catch (e: Exception) {
                    Gdx.app.error("PlayerInputHandler", "Błąd przy konwersji współrzędnych: ${e.message}")
                    return false
                }
            }
        } catch (e: Exception) {
            Gdx.app.error("PlayerInputHandler", "Błąd podczas obsługi prawego przycisku myszy: ${e.message}")
            return false
        }
    }
}