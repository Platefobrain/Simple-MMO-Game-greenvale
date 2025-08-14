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

package pl.decodesoft.network.handlers

import com.badlogic.gdx.Gdx
import pl.decodesoft.MMOGame
import pl.decodesoft.network.BaseMessageHandler

// Handler obsługujący wiadomości czatu
class ChatMessageHandler(game: MMOGame) : BaseMessageHandler(game) {

    // Deklarujemy oba typy
    override val supportedMessageTypes = setOf("CHAT", "LOG")

    override fun handleMessage(parts: List<String>) {
        when (parts[0]) {
            "CHAT" -> handleChatMessage(parts)
            "LOG"  -> handleLogMessage(parts)
        }
    }

    /**  CHAT|senderId|senderName|Treść  */
    private fun handleChatMessage(parts: List<String>) {
        if (parts.size < 4) return
        val senderId   = parts[1]
        val senderName = parts[2]
        val content    = parts.subList(3, parts.size).joinToString("|")

        Gdx.app.postRunnable {
            game.receiveNetworkChatMessage(senderId, senderName, content)
        }
    }

    /**  LOG|Treść  */
    private fun handleLogMessage(parts: List<String>) {
        if (parts.size < 2) return
        val content = parts.subList(1, parts.size).joinToString("|")

        Gdx.app.postRunnable {
            game.receiveNetworkCombatLog(content)
        }
    }
}