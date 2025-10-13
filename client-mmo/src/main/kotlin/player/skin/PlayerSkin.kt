package pl.decodesoft.player.skin

import com.badlogic.gdx.graphics.g2d.SpriteBatch

enum class Direction {
    LEFT, RIGHT, UP, DOWN
}

abstract class PlayerSkin(
    protected val size: Float
) {

    protected var animationTime: Float = 0f

    open fun update(deltaTime: Float) {
        animationTime += deltaTime
    }

    abstract fun renderIdle(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction)
    abstract fun renderMoving(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction)
    abstract fun renderDead(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction)

    // Nowa metoda dla animacji broni
    open fun setWeaponAnimation(animationProgress: Float, attackType: String) {
        // Domyślna pusta implementacja
    }

    abstract fun dispose()
}