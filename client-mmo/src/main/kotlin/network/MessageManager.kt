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

package pl.decodesoft.network

import com.badlogic.gdx.graphics.Color
import pl.decodesoft.MMOGame
import pl.decodesoft.network.handlers.*
import kotlin.math.abs
import kotlin.math.sign

// Zmodyfikowany MessageManager z systemem kolejkowania i obsługą itemów
class MessageManager(game: MMOGame) {
    private val handlers = mutableListOf<MessageHandler>()

    // Klasa dla pojedynczego komunikatu z animacją
    data class DisplayMessage(
        val text: String,
        var timer: Float,
        val duration: Float,
        var yOffset: Float = 0f, // Offset w górę od podstawowej pozycji
        val color: Color = Color.WHITE, // Kolor komunikatu
        var currentY: Float = -30f, // Aktualna pozycja Y (startuje poniżej)
        var targetY: Float = 0f, // Docelowa pozycja Y
        var isMoving: Boolean = true, // Czy komunikat się jeszcze porusza
        val moveSpeed: Float = 150f // Prędkość ruchu w pikselach na sekundę
    )

    // Lista aktywnych komunikatów
    private val activeMessages = mutableListOf<DisplayMessage>()
    private val maxMessages = 5 // Maksymalna liczba komunikatów na ekranie
    private val messageSpacing = 40f // Odstęp między komunikatami

    init {
        // Rejestrowanie wszystkich handlerów
        handlers.add(ItemMessageHandler(game))
        handlers.add(PlayerMessageHandler(game))
        handlers.add(EnemyMessageHandler(game))
        handlers.add(NPCMessageHandler(game))
        handlers.add(ChatMessageHandler(game))
        handlers.add(CombatMessageHandler(game))
        handlers.add(PathfindingMessageHandler(game))
        handlers.add(TextMessageHandler(game))
    }

    // Wyświetl komunikat na środku ekranu z animacją
    fun showMessage(message: String, duration: Float = 2f, color: Color = Color.WHITE) {
        // Przesuń wszystkie istniejące komunikaty w górę
        activeMessages.forEach { msg ->
            msg.targetY += messageSpacing
            msg.isMoving = true // Uruchom ponownie animację dla starszych komunikatów
        }

        // Dodaj nowy komunikat na podstawowej pozycji z animacją
        val newMessage = DisplayMessage(
            text = message,
            timer = duration,
            duration = duration,
            yOffset = 0f,
            color = color,
            currentY = -30f, // Startuje poniżej środka
            targetY = 0f,    // Docelowa pozycja na środku
            isMoving = true
        )
        activeMessages.add(0, newMessage) // Dodaj na początku listy

        // Usuń najstarsze komunikaty jeśli przekroczono limit
        if (activeMessages.size > maxMessages) {
            activeMessages.removeAt(activeMessages.size - 1)
        }
    }

    // Aktualizuj timer komunikatów i animacje
    fun update(delta: Float) {
        val toRemove = mutableListOf<DisplayMessage>()

        activeMessages.forEach { message ->
            // Animacja ruchu w górę
            if (message.isMoving) {
                val distance = message.targetY - message.currentY
                if (abs(distance) > 1f) { // Jeszcze się porusza
                    message.currentY += message.moveSpeed * delta * sign(distance)
                } else {
                    // Zatrzymaj się na docelowej pozycji
                    message.currentY = message.targetY
                    message.isMoving = false
                }
            }

            // Odliczanie czasu
            message.timer -= delta
            if (message.timer <= 0) {
                toRemove.add(message)
            }
        }

        // Usuń wygasłe komunikaty
        activeMessages.removeAll(toRemove)
    }

    // Gettery dla GameUI do renderowania
    fun getActiveMessages(): List<DisplayMessage> = activeMessages.toList()

    // Główna metoda przetwarzania wiadomości
    fun processMessage(message: String) {

        if (message.contains("|")) {
            val parts = message.split("|")
            if (parts.isEmpty()) return

            val messageType = parts[0]

            val handlerFound = handlers.firstOrNull { it.canHandle(messageType) }

            handlerFound?.handleMessage(parts) ?: run {
                println("Nieobsługiwana wiadomość: $message")
            }

        } else {
            val textHandlers = handlers.filter { it.canHandle(message) }

            if (textHandlers.isNotEmpty()) {
                textHandlers.first().handleMessage(listOf(message))
            } else {
                println("Nieobsługiwana wiadomość tekstowa: $message") // Log nieobsługiwanej wiadomości tekstowej
            }
        }
    }
}