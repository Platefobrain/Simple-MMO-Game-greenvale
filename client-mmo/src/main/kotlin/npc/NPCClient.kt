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

package pl.decodesoft.npc

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import pl.decodesoft.player.Faction

class NPCClient(
    val id: String,
    val name: String,
    val type: String,
    val x: Float,
    val y: Float,
    var currentHealth: Int = 100,
    val maxHealth: Int = 100,
    val level: Int = 1,
    var isSelected: Boolean = false,
    val faction: Faction = Faction.NONE
) {

    private val npcColor: Color
        get() = when (type) {
            "INNKEEPER" -> Color.BROWN
            "MERCHANT" -> Color.GOLD
            "GUARD" -> Color.BLUE
            "BLACKSMITH" -> Color.GRAY
            "ALCHEMIST" -> Color.PURPLE
            else -> Color.WHITE
        }

    val displayType: String
        get() = when (type) {
            "INNKEEPER" -> "Innkeeper"
            "MERCHANT" -> "Kupiec"
            "GUARD" -> "Strażnik"
            "BLACKSMITH" -> "Kowal"
            "ALCHEMIST" -> "Alchemik"
            else -> type
        }

    fun render(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = npcColor
        shapeRenderer.circle(x, y, 18f)
        shapeRenderer.color = Color.WHITE
    }
}