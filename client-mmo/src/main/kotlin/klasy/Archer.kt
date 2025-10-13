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

package pl.decodesoft.klasy

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import pl.decodesoft.klasy.skile.SkileManager
import pl.decodesoft.network.MessageManager
import pl.decodesoft.player.Player

// Klasa reprezentująca Łucznika
class Archer(
    player: Player,
    networkScope: CoroutineScope,
    session: () -> DefaultWebSocketSession?,
    skileManager: SkileManager,
    messageManager: MessageManager
) : CharacterClass(player, networkScope, session, skileManager, messageManager) {

    // Nadpisane właściwości z klasy bazowej
    override val attackCooldown = 2.0f
    override var attackTimer = 0f
    override val attackRange = 300f
    override val attackName = "Strzał"
    override val attackColor: Color = Color.ORANGE

    // strzały - TYLKO wysyła request do serwera
    override fun performAttack(targetX: Float, targetY: Float, targetId: String) {
        // Oblicz kierunek strzały
        val dirX = targetX - player.x
        val dirY = targetY - player.y
        val distance: Float = Vector2.dst(player.x, player.y, targetX, targetY)

        // Normalizacja wektora kierunku
        val normalizedDirX = dirX / distance
        val normalizedDirY = dirY / distance

        // USUNIĘTO: Tworzenie strzały lokalnie
        // Strzała zostanie utworzona TYLKO gdy serwer zatwierdzi atak
        // i wyśle broadcast RANGED_ATTACK

        // Wyślij informację o strzale do serwera
        sendAttackMessage(
            "RANGED_ATTACK",
            targetX,
            targetY,
            normalizedDirX,
            normalizedDirY,
            targetId
        )
    }
}