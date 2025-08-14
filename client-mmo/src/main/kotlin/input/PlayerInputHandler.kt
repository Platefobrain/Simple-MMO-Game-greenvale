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
import pl.decodesoft.msg.ChatSystem
import pl.decodesoft.player.PlayerTargetManager
import pl.decodesoft.ui.GameUI
import kotlin.reflect.KFunction0

// Klasa odpowiedzialna za obsługę wejścia gracza
class PlayerInputHandler(
    private val camera: OrthographicCamera,
    private val targetManager: PlayerTargetManager,
    private val onMoveRequested: (Float, Float) -> Unit,
    private val onControlKeyPressed: KFunction0<Unit>,
    private val onAttackRequested: (Any, String) -> Unit,
    private val gameUI: GameUI, // Wymagane GameUI
    private val chatSystem: ChatSystem // Dodaj ChatSystem
) {

    fun handleInput(): Boolean {
        // Klawisz C otwiera/zamyka okno postaci (Character Window)
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            gameUI.toggleCharacterWindow()
            return true
        }

        // Klawisz V otwiera/zamyka ekwipunek (Inventory)
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            gameUI.toggleInventory()
            return true
        }

        // Obsługa klawiszy kontrolnych (umiejętności)
        if (handleControlKeys()) {
            return true
        }

        // Obsługa LEWEGO przycisku myszy
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            val mouseX = Gdx.input.x.toFloat()
            val mouseY = Gdx.input.y.toFloat()
            val adjustedY = camera.viewportHeight - mouseY // Odwróć Y dla UI

            // Sprawdź kliknięcie w przycisk Character (C)
            if (gameUI.isClickOnCharacterButton(mouseX, adjustedY)) {
                gameUI.toggleCharacterWindow()
                return true
            }

            // Sprawdź kliknięcie w przycisk Inventory (I)
            if (gameUI.isClickOnInventoryButton(mouseX, adjustedY)) {
                gameUI.toggleInventory()
                return true
            }

            // Sprawdź kliknięcie w przycisk Chat
            if (gameUI.isClickOnChatButton(mouseX, adjustedY)) {
                // Jeśli nie jesteśmy w trybie CHAT, przełącz
                if (chatSystem.getCurrentMode().name != "CHAT") {
                    chatSystem.switchMode()
                }
                return true
            }

            // Sprawdź kliknięcie w przycisk Log
            if (gameUI.isClickOnLogButton(mouseX, adjustedY)) {
                // Jeśli nie jesteśmy w trybie LOG, przełącz
                if (chatSystem.getCurrentMode().name != "LOG") {
                    chatSystem.switchMode()
                }
                return true
            }

            // Obsługa kliknięć w okno postaci (zakładki Equipment/Stats)
            gameUI.handleCharacterWindowClick(mouseX, adjustedY, false)

            // Obsługa kliknięć w panel ekwipunku (sloty inventory)
            gameUI.handleInventoryClick(mouseX, adjustedY, false)

            return handleLeftMouseButton()
        }

        // Obsługa PRAWEGO przycisku myszy
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            return handleRightMouseButton()
        }

        return false
    }

    private fun handleControlKeys(): Boolean {
        // Obsługa klawiszy Q, W, E, R itd. (bez C i V - są teraz dla UI)
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
                // Żądanie ataku z automatycznym podchodzeniem
                onAttackRequested(target, entityType)
                return true
            } else {
                // Ruch do wskazanego miejsca
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