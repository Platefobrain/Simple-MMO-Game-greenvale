package pl.decodesoft.player.skin.weapon

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import pl.decodesoft.player.skin.Direction
import pl.decodesoft.player.skin.WeaponSkin
import kotlin.math.sin
import kotlin.math.cos

class StaffSkin : WeaponSkin {

    private val staffTexture = Texture("textures/mage/weapon/staff_1.png")
    private val staffScale = 0.1f

    private val staffHandOffsets = mutableMapOf<Direction, Vector2>()

    init {
        staffHandOffsets[Direction.UP] = Vector2(20f, 0f)
        staffHandOffsets[Direction.DOWN] = Vector2(-2f, 0f)
        staffHandOffsets[Direction.LEFT] = Vector2(0f, -3f)
        staffHandOffsets[Direction.RIGHT] = Vector2(0f, -3f)
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
                // UNOSZENIE LASKI DO GÓRY - lustrzane do DOWN
                val baseLeft = rightHandBaseOffsets[direction]!!  // UWAGA: zamiana stron!
                val baseRight = leftHandBaseOffsets[direction]!!

                // Kąt rotacji ramienia
                val armAngle = 60f * progress
                leftArmRotations[direction] = armAngle * 1.5f

                // Dłoń obliczona na podstawie rotacji ramienia (ruch w prawo i do góry)
                val armRadians = Math.toRadians((armAngle * 0.8f).toDouble())
                val leftX = baseRight.x - sin(armRadians).toFloat() * armLength * 1.0f
                val leftY = baseRight.y - cos(armRadians).toFloat() * armLength * 1.0f + armLength + progress * 8.5f

                leftHandOffsets[direction] = Vector2(leftX, leftY)
                leftHandRotations[direction] = -0.5f

                // Lewa ręka nieruchoma
                rightArmRotations[direction] = 0f
                rightHandOffsets[direction] = baseLeft.cpy()
                rightHandRotations[direction] = 0f
            }

            Direction.DOWN -> {
                // DŹWIGANIE LASKI - dłoń podąża za ramieniem
                val baseLeft = leftHandBaseOffsets[direction]!!
                val baseRight = rightHandBaseOffsets[direction]!!

                // Kąt rotacji ramienia
                val armAngle = -60f * progress
                rightArmRotations[direction] = armAngle * 1.5f

                // Dłoń obliczona na podstawie rotacji ramienia
                val armRadians = Math.toRadians((armAngle * 0.8f).toDouble())
                val rightX = baseRight.x + sin(armRadians).toFloat() * armLength * 1.0f
                val rightY = baseRight.y - cos(armRadians).toFloat() * armLength * 1.0f + armLength + progress * 8.5f

                rightHandOffsets[direction] = Vector2(rightX, rightY)
                rightHandRotations[direction] = 0.5f  // BROŃ BEZ ROTACJI

                // Lewa ręka pozostaje nieruchoma
                leftArmRotations[direction] = 0f
                leftHandOffsets[direction] = baseLeft.cpy()
                leftHandRotations[direction] = 0f
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
        val handOffset = staffHandOffsets[direction] ?: Vector2(0f, 0f)

        val (directionRotation, flipX) = when (direction) {
            Direction.UP -> -100f to true
            Direction.DOWN -> 100f to false
            Direction.LEFT -> 0f to true
            Direction.RIGHT -> 0f to false
        }

        val finalRotation = directionRotation + rightHandRotation + animRotation
        val finalScale = scale * staffScale

        val centerX = x + rightHandOffset.x * scale + handOffset.x * scale - staffTexture.width / 2 * finalScale + offsetX
        val centerY = y + rightHandOffset.y * scale + handOffset.y * scale - staffTexture.height / 2 * finalScale + offsetY

        batch.draw(
            staffTexture, centerX, centerY,
            staffTexture.width * finalScale / 2f, staffTexture.height * finalScale / 2f,
            staffTexture.width * finalScale, staffTexture.height * finalScale,
            1f, 1f, finalRotation,
            0, 0, staffTexture.width, staffTexture.height, flipX, false
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

        return when (direction) {
            Direction.DOWN -> {
                // Broń unosi się i lekko odchyla w lewo
                val offsetX = -progress * scale * 2f    // ruch w lewo
                val offsetY = progress * scale * 5f     // ruch lekko w górę
                val rotation = -progress * 8f          // lekki obrót w lewo
                Triple(offsetX, offsetY, rotation)
            }

            Direction.UP -> {
                val offsetX = progress * scale * 4f
                val offsetY = progress * scale * 9f
                val rotation = progress * 8f
                Triple(offsetX, offsetY, rotation)
            }

            Direction.LEFT -> {
                val offsetX = -progress * scale * 5f
                val offsetY = 0f
                val rotation = -progress * 6f
                Triple(offsetX, offsetY, rotation)
            }

            Direction.RIGHT -> {
                val offsetX = progress * scale * 5f
                val offsetY = 0f
                val rotation = progress * 6f
                Triple(offsetX, offsetY, rotation)
            }
        }
    }

    override fun dispose() {
        staffTexture.dispose()
    }
}