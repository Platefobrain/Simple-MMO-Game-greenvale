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
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import pl.decodesoft.enemy.EnemyClient
import pl.decodesoft.klasy.skile.Skile
import pl.decodesoft.player.Player
import java.util.*
import kotlin.math.atan2

//Strzała wystrzelona przez Łucznika.
class Arrow(
    override var x: Float,
    override var y: Float,
    dirX: Float,
    dirY: Float,
    override val casterId: String,
    private val targetId: String? = null,
    private val targetX: Float,
    private val targetY: Float,
    private val speed: Float = 400f
) : Skile {

    override val id: String = UUID.randomUUID().toString()
    override val color: Color = Color.YELLOW
    override val size: Float = 8f
    override var isToRemove: Boolean = false

    private val direction: Vector2 = Vector2(dirX, dirY).nor()
    private val hitbox = Rectangle(x - size / 2, y - size / 2, size, size)

    private val arrowTexture = ProjectileTextures.arrow
    private val arrowScale = 0.1f

    private val angle: Float
        get() = Math.toDegrees(atan2(direction.y.toDouble(), direction.x.toDouble())).toFloat()

    override fun update(delta: Float): Boolean {
        if (isToRemove) return false

        val move = speed * delta
        x += direction.x * move
        y += direction.y * move
        hitbox.setPosition(x - size / 2, y - size / 2)

        // doleciała?
        val arrived = Vector2.dst(x, y, targetX, targetY) <= 4f
        if (arrived) markForRemoval()

        return !arrived
    }

    override fun checkCollision(player: Player): Boolean {
        if (targetId != null && targetId != player.id) return false
        return Vector2.dst(x, y, player.x, player.y) <= 15f
    }

    override fun checkCollision(enemy: EnemyClient): Boolean {
        if (targetId != null && targetId != "enemy_${enemy.id}") return false
        return Vector2.dst(x, y, enemy.x, enemy.y) <= 15f
    }

    override fun markForRemoval() {
        isToRemove = true
    }

    override fun render(shapeRenderer: ShapeRenderer) {
        // Ta metoda nie jest już używana
    }

    fun render(batch: SpriteBatch) {
        val finalScale = arrowScale

        val centerX = x - arrowTexture.width / 2 * finalScale
        val centerY = y - arrowTexture.height / 2 * finalScale

        batch.draw(
            arrowTexture,
            centerX,
            centerY,
            arrowTexture.width * finalScale / 2f,
            arrowTexture.height * finalScale / 2f,
            arrowTexture.width * finalScale,
            arrowTexture.height * finalScale,
            1f,
            1f,
            angle,
            0,
            0,
            arrowTexture.width,
            arrowTexture.height,
            false,
            false
        )
    }
}