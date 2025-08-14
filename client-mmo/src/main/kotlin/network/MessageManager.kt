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

// Zmodyfikowany MessageManager z systemem kolejkowania i obsługą itemów
class MessageManager(game: MMOGame) {
    private val handlers = mutableListOf<MessageHandler>()

    // Klasa dla pojedynczego komunikatu
    data class DisplayMessage(
        val text: String,
        var timer: Float,
        val duration: Float,
        var yOffset: Float = 0f, // Offset w górę od podstawowej pozycji
        val color: Color = Color.WHITE // DODANE: Kolor komunikatu
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
        handlers.add(ChatMessageHandler(game))
        handlers.add(CombatMessageHandler(game))
        handlers.add(PathfindingMessageHandler(game))
        handlers.add(TextMessageHandler(game))
    }

    // Wyświetl komunikat na środku ekranu
    fun showMessage(message: String, duration: Float = 2f, color: Color = Color.WHITE) {
        // Przesuń wszystkie istniejące komunikaty w górę
        activeMessages.forEach { msg ->
            msg.yOffset += messageSpacing
        }

        // Dodaj nowy komunikat na podstawowej pozycji z kolorem
        val newMessage = DisplayMessage(message, duration, duration, 0f, color)
        activeMessages.add(0, newMessage) // Dodaj na początku listy

        // Usuń najstarsze komunikaty jeśli przekroczono limit
        if (activeMessages.size > maxMessages) {
            activeMessages.removeAt(activeMessages.size - 1)
        }
    }

    // Aktualizuj timer komunikatów
    fun update(delta: Float) {
        val toRemove = mutableListOf<DisplayMessage>()

        activeMessages.forEach { message ->
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
        // DODAJ DEBUG:
        if (message.startsWith("ITEM_")) {
            println("🔥 DEBUG: MessageManager otrzymał wiadomość ITEM: $message")
        }

        if (message.contains("|")) {
            val parts = message.split("|")
            if (parts.isEmpty()) return

            val messageType = parts[0]

            // DODAJ DEBUG:
            if (messageType.startsWith("ITEM_")) {
                println("🔥 DEBUG: MessageManager szuka handlera dla: $messageType")
            }

            val handlerFound = handlers.firstOrNull { it.canHandle(messageType) }

            // DODAJ DEBUG:
            if (messageType.startsWith("ITEM_")) {
                if (handlerFound != null) {
                    println("🔥 DEBUG: Znaleziono handler: ${handlerFound.javaClass.simpleName}")
                } else {
                    println("🔥 DEBUG: NIE ZNALEZIONO handlera dla: $messageType")
                }
            }

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