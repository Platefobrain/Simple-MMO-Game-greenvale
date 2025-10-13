package pl.decodesoft.enemy.forest

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import pl.decodesoft.enemy.EnemySkinManager

class SheepSkin : EnemySkinManager.EnemySkin(90f) { // rozmiar

    private val spriteSheet: Texture = Texture(Gdx.files.internal("textures/enemies/Sheep.png"))

    private val idleLeftAnimation: Animation<TextureRegion>
    private val idleRightAnimation: Animation<TextureRegion>
    private val walkLeftAnimation: Animation<TextureRegion>
    private val walkRightAnimation: Animation<TextureRegion>
    private val deadTexture: TextureRegion

    init {
        val frameWidth = 32
        val frameHeight = 32
        val frameDuration = 0.25f

        // Wiersz 0: Idle left (2 klatki poziomo)
        val idleLeftFrames = Array(2) { i ->
            TextureRegion(spriteSheet, i * frameWidth, 0, frameWidth, frameHeight)
        }
        idleLeftAnimation = Animation(frameDuration, *idleLeftFrames)

        // Wiersz 1: Idle right (2 klatki poziomo)
        val idleRightFrames = Array(2) { i ->
            TextureRegion(spriteSheet, i * frameWidth, frameHeight, frameWidth, frameHeight)
        }
        idleRightAnimation = Animation(frameDuration, *idleRightFrames)

        // Wiersz 2: Walk left (2 klatki poziomo)
        val walkLeftFrames = Array(2) { i ->
            TextureRegion(spriteSheet, i * frameWidth, frameHeight * 2, frameWidth, frameHeight)
        }
        walkLeftAnimation = Animation(frameDuration, *walkLeftFrames)

        // Wiersz 3: Walk right (2 klatki poziomo)
        val walkRightFrames = Array(2) { i ->
            TextureRegion(spriteSheet, i * frameWidth, frameHeight * 3, frameWidth, frameHeight)
        }
        walkRightAnimation = Animation(frameDuration, *walkRightFrames)

        // Wiersz 4: Dead
        deadTexture = TextureRegion(spriteSheet, 0, frameHeight * 4, frameWidth, frameHeight)
    }

    override fun renderIdle(batch: SpriteBatch, x: Float, y: Float, targetX: Float, targetY: Float, stateTime: Float) {
        updateDirection(x, targetX)

        val animation = when (lastDirection) {
            Direction.LEFT -> idleLeftAnimation
            Direction.RIGHT -> idleRightAnimation
        }

        val frame = animation.getKeyFrame(stateTime, true)
        batch.draw(frame, x - size / 2, y - size / 2, size, size)
    }

    override fun renderMoving(batch: SpriteBatch, x: Float, y: Float, targetX: Float, targetY: Float, stateTime: Float) {
        updateDirection(x, targetX)

        val animation = when (lastDirection) {
            Direction.LEFT -> walkLeftAnimation
            Direction.RIGHT -> walkRightAnimation
        }

        val frame = animation.getKeyFrame(stateTime, true)
        batch.draw(frame, x - size / 2, y - size / 2, size, size)
    }

    override fun renderDead(batch: SpriteBatch, x: Float, y: Float, stateTime: Float) {
        batch.draw(deadTexture, x - size / 2, y - size / 2, size, size)
    }

    override fun dispose() {
        spriteSheet.dispose()
    }
}