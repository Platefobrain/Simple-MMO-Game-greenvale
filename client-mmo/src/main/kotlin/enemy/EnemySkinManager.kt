package pl.decodesoft.enemy

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.math.abs

class EnemySkinManager {

    // Pozycja skina
    companion object {
        private const val RENDER_OFFSET_Y = 10f
    }

    private val enemySkins = mutableMapOf<String, EnemySkin>()
    private var stateTime = 0f

    init {
        enemySkins["Sheep"] = pl.decodesoft.enemy.forest.SheepSkin()
        enemySkins["Wolf"] = pl.decodesoft.enemy.forest.WolfSkin()
        //enemySkins["Bear"] = pl.decodesoft.enemy.forest.BearSkin()
        enemySkins["Spider"] = pl.decodesoft.enemy.forest.SpiderSkin()
    }

    fun update(delta: Float) {
        stateTime += delta
    }

    fun render(batch: SpriteBatch, enemy: EnemyClient) {
        val skin = enemySkins[enemy.type] ?: return

        val adjustedY = enemy.y + RENDER_OFFSET_Y

        if (!enemy.isAlive) {
            skin.renderDead(batch, enemy.x, adjustedY, stateTime)
            return
        }

        if (enemy.isMoving) {
            skin.renderMoving(batch, enemy.x, adjustedY, enemy.targetX, enemy.targetY, stateTime)
        } else {
            skin.renderIdle(batch, enemy.x, adjustedY, enemy.targetX, enemy.targetY, stateTime)
        }
    }

    fun dispose() {
        enemySkins.values.forEach { it.dispose() }
    }

    abstract class EnemySkin(protected val size: Float) {

        protected enum class Direction {
            LEFT, RIGHT
        }

        protected var lastDirection = Direction.RIGHT

        abstract fun renderIdle(batch: SpriteBatch, x: Float, y: Float, targetX: Float, targetY: Float, stateTime: Float)
        abstract fun renderMoving(batch: SpriteBatch, x: Float, y: Float, targetX: Float, targetY: Float, stateTime: Float)
        abstract fun renderDead(batch: SpriteBatch, x: Float, y: Float, stateTime: Float)
        abstract fun dispose()

        protected fun updateDirection(x: Float, targetX: Float) {
            val deltaX = targetX - x
            if (abs(deltaX) > 0.1f) {
                lastDirection = if (deltaX < 0) Direction.LEFT else Direction.RIGHT
            }
        }
    }
}