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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.ktor.websocket.*
import kotlin.math.sqrt

class MovementController(
    private val networkScope: CoroutineScope,
    private val getSession: () -> DefaultWebSocketSession?
) {
    private var lastClickPosition = Pair(0f, 0f)
    private var lastMoveTime = 0L
    private val minimumDistance = 5f // piksele - dostosuj według potrzeb
    private val moveThrottle = 100L // milisekundy - dostosuj według potrzeb

    // Obsługuje ruch po kliknięciu na pustą przestrzeń mapy
    fun handleMovementClick(mouseX: Float, mouseY: Float, playerId: String): Boolean {
        val now = System.currentTimeMillis()

        // Sprawdź throttle czasowy
        if (now - lastMoveTime < moveThrottle) {
            return false
        }

        // Sprawdź dystans od ostatniego kliku
        val distance = calculateDistance(mouseX, mouseY, lastClickPosition.first, lastClickPosition.second)

        if (distance < minimumDistance) {
            return false
        }

        // Aktualizuj ostatnią pozycję i czas
        lastClickPosition = Pair(mouseX, mouseY)
        lastMoveTime = now

        // Wyślij ruch do serwera
        sendMoveCommand(mouseX, mouseY, playerId)

        return true
    }

    // Wysyła ruch do określonego celu (przeciwnik, NPC, obiekt) - bez throttlingu
    private fun sendMoveToTarget(x: Float, y: Float, playerId: String) {
        sendMoveCommand(x, y, playerId)
    }

    // Obsługuje kliknięcie na cel (używane przez PlayerController)
    fun handleTargetClick(x: Float, y: Float, playerId: String) {
        sendMoveToTarget(x, y, playerId)
    }

    // Wspólna metoda wysyłająca komendę ruchu do serwera
    private fun sendMoveCommand(targetX: Float, targetY: Float, playerId: String) {
        networkScope.launch {
            try {
                val session = getSession()
                if (session != null) {
                    val moveMessage = "MOVE_TO|$targetX|$targetY|0|$playerId"
                    session.send(Frame.Text(moveMessage))
                } else {
                    Gdx.app.error("MovementController", "Brak aktywnej sesji WebSocket")
                }
            } catch (e: Exception) {
                Gdx.app.error("MovementController", "Błąd wysyłania ruchu: ${e.message}")
            }
        }
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))
    }

    // Reset pozycji (przy respawnie, teleportacji)
    fun resetLastPosition(x: Float, y: Float) {
        lastClickPosition = Pair(x, y)
        lastMoveTime = 0L
    }
}