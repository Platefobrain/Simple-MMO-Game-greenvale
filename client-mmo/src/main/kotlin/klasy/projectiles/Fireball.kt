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
 * Klasa reprezentująca kulę ognia wystrzeloną przez Maga
 */
class Fireball(
    override var x: Float,
    override var y: Float,
    dirX: Float,
    dirY: Float,
    override val casterId: String,
    private val targetId: String? = null,
    private val targetX: Float,
    private val targetY: Float,
    private val speed: Float = 300f,
    private val radius: Float = 10f
) : Skile {

    override val id: String = UUID.randomUUID().toString()
    override val color: Color = Color.ORANGE
    override val size: Float = radius * 2
    override var isToRemove: Boolean = false

    private val direction = Vector2(dirX, dirY).nor()
    private val hitbox = Rectangle(x - radius, y - radius, size, size)

    override fun update(delta: Float): Boolean {
        if (isToRemove) return false

        val move = speed * delta
        x += direction.x * move
        y += direction.y * move
        hitbox.setPosition(x - radius, y - radius)

        val arrived = Vector2.dst(x, y, targetX, targetY) <= 6f
        if (arrived) markForRemoval()

        return !arrived
    }

    override fun checkCollision(player: Player) = /* identycznie jak w Arrow */
        (targetId == null || targetId == player.id) &&
                Vector2.dst(x, y, player.x, player.y) <= radius + 5f

    override fun checkCollision(enemy: EnemyClient) =
        (targetId == null || targetId == "enemy_${enemy.id}") &&
                Vector2.dst(x, y, enemy.x, enemy.y) <= radius + 5f

    override fun markForRemoval() { isToRemove = true }

    override fun render(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = color
        shapeRenderer.circle(x, y, radius)
    }
}