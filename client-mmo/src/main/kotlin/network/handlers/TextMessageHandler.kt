package pl.decodesoft.network.handlers

import com.badlogic.gdx.Gdx
import pl.decodesoft.MMOGame
import pl.decodesoft.network.BaseMessageHandler

// Handler obsługujący wiadomości tekstowe bez określonego formatu
class TextMessageHandler(game: MMOGame) : BaseMessageHandler(game) {
    override val supportedMessageTypes = emptySet<String>()

    // Nadpisujemy metodę canHandle, aby sprawdzać czy wiadomość jest zwykłym tekstem
    override fun canHandle(messageType: String): Boolean {
        return !messageType.contains("|") &&
                !messageType.startsWith("ITEM_") &&
                !messageType.startsWith("PLAYER_") &&
                !messageType.startsWith("ENEMY_") &&
                !messageType.startsWith("CHAT") &&
                !messageType.startsWith("STATS_")
    }

    override fun handleMessage(parts: List<String>) {
        // W przypadku prostych wiadomości tekstowych, wyświetlamy je jako wiadomość systemową w czacie
        val message = parts.joinToString(" ")

        Gdx.app.postRunnable {
            game.receiveNetworkChatMessage("SERVER", "Serwer", message)
        }

        // Dodatkowo logujemy wiadomość w konsoli
        Gdx.app.log("ServerMessage", message)
    }
}