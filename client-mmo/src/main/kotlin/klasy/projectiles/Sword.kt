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

package pl.decodesoft.klasy.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.player.Player
import pl.decodesoft.klasy.skile.Skile
import java.util.UUID

/**
 * Klasa reprezentująca atak mieczem Wojownika
 */
class Sword(
    override var x: Float,
    override var y: Float,
    dirX: Float,
    dirY: Float,
    override val casterId: String,
    private val targetId: String? = null,
    private val targetX: Float,
    private val targetY: Float,
    private val speed: Float = 550f,
    private val reach: Float = 24f
) : Skile {

    override val id: String = UUID.randomUUID().toString()
    override val color: Color = Color.LIGHT_GRAY
    override val size: Float = reach
    override var isToRemove: Boolean = false

    private val direction = Vector2(dirX, dirY).nor()
    private val hitbox = Rectangle(x - reach / 2, y - reach / 2, reach, reach)

    override fun update(delta: Float): Boolean {
        if (isToRemove) return false

        val move = speed * delta
        x += direction.x * move
        y += direction.y * move
        hitbox.setPosition(x - reach / 2, y - reach / 2)

        val arrived = Vector2.dst(x, y, targetX, targetY) <= 4f
        if (arrived) markForRemoval()

        return !arrived
    }

    override fun checkCollision(player: Player) = /* analogicznie */
        (targetId == null || targetId == player.id) &&
                Vector2.dst(x, y, player.x, player.y) <= reach

    override fun checkCollision(enemy: EnemyClient) =
        (targetId == null || targetId == "enemy_${enemy.id}") &&
                Vector2.dst(x, y, enemy.x, enemy.y) <= reach

    override fun markForRemoval() { isToRemove = true }

    override fun render(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = color
        shapeRenderer.rect(x - reach / 2, y - reach / 2, reach, reach)
    }
}