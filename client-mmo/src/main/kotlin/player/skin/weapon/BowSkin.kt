package pl.decodesoft.player.skin.weapon

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import pl.decodesoft.player.skin.Direction
import pl.decodesoft.player.skin.WeaponSkin
import kotlin.math.sin
import kotlin.math.cos

class BowSkin : WeaponSkin {

    private val bowTexture = Texture("textures/archer/weapon/bow_1.png")
    private val bowScale = 0.1f

    private val bowHandOffsets = mutableMapOf<Direction, Vector2>()

    init {
        bowHandOffsets[Direction.UP] = Vector2(-3f, 0f)
        bowHandOffsets[Direction.DOWN] = Vector2(-3f, 0f)
        bowHandOffsets[Direction.LEFT] = Vector2(0f, -3f)
        bowHandOffsets[Direction.RIGHT] = Vector2(0f, -3f)
    }

    override fun animateAttack(
        progress: Float,
        direction: Direction,
        leftArmBaseOffsets: Map<Direction, Vector2>,
        rightArmBaseOffsets: Map<Direction, Vector2>,
        leftHandBaseOffsets: Map<Direction, Vector2>,
        rightHandBaseOffsets: Map<Direction, Vector2>,
        leftArmOffsets: MutableMap<Direction, Vector2>,
        rightArmOffsets: MutableMap<Direction, Vector2>,
        leftHandOffsets: MutableMap<Direction, Vector2>,
        rightHandOffsets: MutableMap<Direction, Vector2>,
        leftArmRotations: MutableMap<Direction, Float>,
        rightArmRotations: MutableMap<Direction, Float>,
        leftHandRotations: MutableMap<Direction, Float>,
        rightHandRotations: MutableMap<Direction, Float>,
        armLength: Float
    ) {
        val armExtension = 90f

        fun calculateHandOffsetLeft(base: Vector2, angleDeg: Float): Vector2 {
            val radians = Math.toRadians(angleDeg.toDouble())
            val x = base.x - sin(radians).toFloat() * armLength
            val y = base.y - cos(radians).toFloat() * armLength + armLength
            return Vector2(x, y)
        }

        fun calculateHandOffsetRight(base: Vector2, angleDeg: Float): Vector2 {
            val radians = Math.toRadians(angleDeg.toDouble())
            val x = base.x + sin(radians).toFloat() * armLength
            val y = base.y - cos(radians).toFloat() * armLength + armLength
            return Vector2(x, y)
        }

        when (direction) {
            Direction.LEFT -> {
                val baseLeft = leftHandBaseOffsets[direction]!!
                val baseRight = rightHandBaseOffsets[direction]!!

                val rightArmAngle = armExtension * progress
                rightArmRotations[direction] = -rightArmAngle
                rightHandOffsets[direction] = calculateHandOffsetLeft(baseRight, rightArmAngle)
                rightHandRotations[direction] = -rightArmAngle

                leftArmRotations[direction] = 0f
                leftHandOffsets[direction] = baseLeft.cpy()
                leftHandRotations[direction] = 0f
            }

            Direction.RIGHT -> {
                val baseLeft = leftHandBaseOffsets[direction]!!
                val baseRight = rightHandBaseOffsets[direction]!!

                val rightArmAngle = armExtension * progress
                rightArmRotations[direction] = rightArmAngle
                rightHandOffsets[direction] = calculateHandOffsetRight(baseRight, rightArmAngle)
                rightHandRotations[direction] = rightArmAngle

                leftArmRotations[direction] = 0f
                leftHandOffsets[direction] = baseLeft.cpy()
                leftHandRotations[direction] = 0f
            }

            Direction.UP -> {
                val baseLeft = leftHandBaseOffsets[direction] ?: leftHandBaseOffsets[Direction.RIGHT]!!
                val baseRight = rightHandBaseOffsets[direction] ?: rightHandBaseOffsets[Direction.RIGHT]!!

                val armAngle = armExtension * progress

                val leftOffset = calculateHandOffsetRight(baseLeft, armAngle)
                leftHandOffsets[direction] = leftOffset
                leftHandRotations[direction] = armAngle
                leftArmRotations[direction] = armAngle

                val rightOffset = calculateHandOffsetRight(baseRight, armAngle)
                rightOffset.y -= progress * 4f
                rightHandOffsets[direction] = rightOffset
                rightHandRotations[direction] = armAngle
                rightArmRotations[direction] = armAngle
            }

            Direction.DOWN -> {
                // NAPINANIE ŁUKU - lewa ręka trzyma łuk, prawa ciągnie cięciwę
                val baseLeft = leftHandBaseOffsets[direction]!!
                val baseRight = rightHandBaseOffsets[direction]!!

                // Lewa ręka wyprostowana trzyma łuk
                leftArmRotations[direction] = -30f
                val leftRadians = Math.toRadians(-30.0)
                val leftX = baseLeft.x + sin(leftRadians).toFloat() * armLength * 0.8f
                val leftY = baseLeft.y - cos(leftRadians).toFloat() * armLength * 0.8f + armLength
                leftHandOffsets[direction] = Vector2(leftX, leftY)
                leftHandRotations[direction] = -30f

                // Prawa ręka ciągnie cięciwę do tyłu
                val pullBack = progress * 15f
                rightArmRotations[direction] = 30f
                val rightRadians = Math.toRadians(30.0)
                val rightX = baseRight.x - sin(rightRadians).toFloat() * armLength * 0.5f - pullBack
                val rightY = baseRight.y - cos(rightRadians).toFloat() * armLength * 0.5f + armLength
                rightHandOffsets[direction] = Vector2(rightX, rightY)
                rightHandRotations[direction] = 30f
            }
        }
    }

    override fun renderWithHand(
        batch: SpriteBatch,
        x: Float,
        y: Float,
        scale: Float,
        animationProgress: Float,
        attackType: String,
        direction: Direction,
        rightHandOffset: Vector2,
        rightHandRotation: Float
    ) {
        val (offsetX, offsetY, animRotation) = getAnimationTransform(animationProgress, scale, attackType, direction)
        val handOffset = bowHandOffsets[direction] ?: Vector2(0f, 0f)

        val (directionRotation, flipX) = when (direction) {
            Direction.UP -> 180f to false
            Direction.DOWN -> 180f to false
            Direction.LEFT -> 90f to true
            Direction.RIGHT -> -90f to false
        }

        val finalRotation = directionRotation + rightHandRotation + animRotation
        val finalScale = scale * bowScale

        val centerX = x + rightHandOffset.x * scale + handOffset.x * scale - bowTexture.width / 2 * finalScale + offsetX
        val centerY = y + rightHandOffset.y * scale + handOffset.y * scale - bowTexture.height / 2 * finalScale + offsetY

        batch.draw(
            bowTexture, centerX, centerY,
            bowTexture.width * finalScale / 2f, bowTexture.height * finalScale / 2f,
            bowTexture.width * finalScale, bowTexture.height * finalScale,
            1f, 1f, finalRotation,
            0, 0, bowTexture.width, bowTexture.height, flipX, false
        )
    }

    override fun render(
        batch: SpriteBatch, x: Float, y: Float, scale: Float,
        animationProgress: Float, attackType: String, direction: Direction
    ) {
        renderWithHand(batch, x, y, scale, animationProgress, attackType, direction, Vector2(0f, 0f), 0f)
    }

    override fun update(deltaTime: Float) {}
    override fun getBaseOffset(): Pair<Float, Float> = 0f to 0f

    override fun getAnimationTransform(
        progress: Float, scale: Float, attackType: String, direction: Direction
    ): Triple<Float, Float, Float> {
        val pullDirection = when (direction) {
            Direction.LEFT -> -1f
            Direction.RIGHT -> 1f
            else -> 1f
        }
        return Triple(progress * scale * 10f * pullDirection, 0f, 0f)
    }

    override fun dispose() {
        bowTexture.dispose()
    }
}