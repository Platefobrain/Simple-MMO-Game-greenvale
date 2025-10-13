package pl.decodesoft.player.skin

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import pl.decodesoft.player.Race
import pl.decodesoft.player.skin.weapon.BowSkin
import pl.decodesoft.player.skin.weapon.StaffSkin
import pl.decodesoft.player.skin.weapon.SwordSkin
import kotlin.math.sin
import kotlin.math.cos

class RaceSkin(
    private val race: Race,
    characterClass: Int  // 0=Archer, 1=Mage, 2=Warrior
) : PlayerSkin(64f) {

    // Tekstury dla różnych części ciała i kierunków
    private val headTextures = mutableMapOf<Direction, Texture>()
    private val bodyTextures = mutableMapOf<Direction, Texture>()
    private val eyesOpenTextures = mutableMapOf<Direction, Texture>()
    private val eyesClosedTextures = mutableMapOf<Direction, Texture>()
    private val leftArmTextures = mutableMapOf<Direction, Texture>()
    private val rightArmTextures = mutableMapOf<Direction, Texture>()
    private val leftHandTextures = mutableMapOf<Direction, Texture>()
    private val rightHandTextures = mutableMapOf<Direction, Texture>()
    private val leftLegTextures = mutableMapOf<Direction, Texture>()
    private val rightLegTextures = mutableMapOf<Direction, Texture>()

    // Offsety
    private val headOffsets = mutableMapOf<Direction, Vector2>()
    private val headDynamicOffsets = mutableMapOf<Direction, Vector2>()
    private val bodyOffsets = mutableMapOf<Direction, Vector2>()
    private val eyesOffsets = mutableMapOf<Direction, Vector2>()
    private val eyesDynamicOffsets = mutableMapOf<Direction, Vector2>()
    private val leftArmOffsets = mutableMapOf<Direction, Vector2>()
    private val rightArmOffsets = mutableMapOf<Direction, Vector2>()
    private val leftHandOffsets = mutableMapOf<Direction, Vector2>()
    private val rightHandOffsets = mutableMapOf<Direction, Vector2>()
    private val leftLegOffsets = mutableMapOf<Direction, Vector2>()
    private val rightLegOffsets = mutableMapOf<Direction, Vector2>()

    // Rotacje
    private val leftArmRotations = mutableMapOf<Direction, Float>()
    private val rightArmRotations = mutableMapOf<Direction, Float>()
    private val leftHandRotations = mutableMapOf<Direction, Float>()
    private val rightHandRotations = mutableMapOf<Direction, Float>()
    private val leftLegRotations = mutableMapOf<Direction, Float>()
    private val rightLegRotations = mutableMapOf<Direction, Float>()

    // Bazowe offsety
    private val leftArmBaseOffsets = mutableMapOf<Direction, Vector2>()
    private val rightArmBaseOffsets = mutableMapOf<Direction, Vector2>()
    private val leftHandBaseOffsets = mutableMapOf<Direction, Vector2>()
    private val rightHandBaseOffsets = mutableMapOf<Direction, Vector2>()
    private val leftLegBaseOffsets = mutableMapOf<Direction, Vector2>()
    private val rightLegBaseOffsets = mutableMapOf<Direction, Vector2>()

    // Bronie - wybór według klasy postaci
    private val weaponSkin: WeaponSkin = when (characterClass) {
        0 -> BowSkin()
        1 -> StaffSkin()
        2 -> SwordSkin()
        else -> SwordSkin()
    }

    // Parametry animacji
    private var weaponAnimationProgress = 0f
    private var weaponAttackType = "MELEE_ATTACK"
    private var blinkTimer = 0f
    private var blinkInterval = 3f
    private var blinkDuration = 0.15f
    private var isBlinking = false

    // Skalowanie
    private var headScale = 0.2f
    private var bodyScale = 0.2f
    private var armScale = 0.2f
    private var handScale = 0.2f
    private var eyesScale = 0.2f
    private var legScale = 0.2f

    private var armSwingSpeed = 9f
    private var armLength = 8f
    private var legSwingSpeed = 9f
    private var headBobSpeed = 9f
    private var headBobAmplitude = 0.5f

    init {
        loadTexturesForRace()
        setupOffsets()
    }

    private fun loadTexturesForRace() {
        val basePath = "textures/races/${race.name.lowercase()}"

        Direction.entries.forEach { dir ->
            val dirName = dir.name.lowercase()

            try {
                headTextures[dir] = Texture("$basePath/head_${dirName}.png")
                bodyTextures[dir] = Texture("$basePath/body_${dirName}.png")

                if (dir != Direction.UP) {
                    eyesOpenTextures[dir] = Texture("$basePath/eye_${dirName}.png")
                    eyesClosedTextures[dir] = Texture("$basePath/eye_${dirName}_blink.png")
                }

                leftArmTextures[dir] = Texture("$basePath/left_arm_${dirName}.png")
                rightArmTextures[dir] = Texture("$basePath/right_arm_${dirName}.png")
                leftHandTextures[dir] = Texture("$basePath/left_hand_${dirName}.png")
                rightHandTextures[dir] = Texture("$basePath/right_hand_${dirName}.png")
                leftLegTextures[dir] = Texture("$basePath/left_leg_${dirName}.png")
                rightLegTextures[dir] = Texture("$basePath/right_leg_${dirName}.png")

            } catch (e: Exception) {
                println("WARNING: Nie można załadować tekstur dla rasy ${race.name}, kierunek $dir: ${e.message}")
            }
        }
    }

    private fun setupOffsets() {
        Direction.entries.forEach { dir ->
            headOffsets[dir] = Vector2(0f, 25f)
            headDynamicOffsets[dir] = Vector2(0f, 25f)
            bodyOffsets[dir] = Vector2(0f, 0f)
        }

        eyesOffsets[Direction.DOWN] = Vector2(0f, 15f)
        eyesOffsets[Direction.LEFT] = Vector2(-10f, 17f)
        eyesOffsets[Direction.RIGHT] = Vector2(10f, 17f)
        eyesDynamicOffsets.putAll(eyesOffsets)

        leftArmBaseOffsets[Direction.UP] = Vector2(8f, 0f)
        leftArmBaseOffsets[Direction.DOWN] = Vector2(8f, 0f)
        leftArmBaseOffsets[Direction.LEFT] = Vector2(3f, 0f)
        leftArmBaseOffsets[Direction.RIGHT] = Vector2(-3f, 0f)

        rightArmBaseOffsets[Direction.UP] = Vector2(-8f, 0f)
        rightArmBaseOffsets[Direction.DOWN] = Vector2(-8f, 0f)
        rightArmBaseOffsets[Direction.LEFT] = Vector2(3f, 0f)
        rightArmBaseOffsets[Direction.RIGHT] = Vector2(-3f, 0f)

        leftHandBaseOffsets[Direction.UP] = Vector2(10f, -6f)
        leftHandBaseOffsets[Direction.DOWN] = Vector2(10f, -6f)
        leftHandBaseOffsets[Direction.LEFT] = Vector2(3f, -7f)
        leftHandBaseOffsets[Direction.RIGHT] = Vector2(-3f, -7f)

        rightHandBaseOffsets[Direction.UP] = Vector2(-10f, -6f)
        rightHandBaseOffsets[Direction.DOWN] = Vector2(-10f, -6f)
        rightHandBaseOffsets[Direction.LEFT] = Vector2(3f, -7f)
        rightHandBaseOffsets[Direction.RIGHT] = Vector2(-3f, -7f)

        leftLegBaseOffsets[Direction.UP] = Vector2(-4f, -10f)
        leftLegBaseOffsets[Direction.DOWN] = Vector2(4f, -10f)
        leftLegBaseOffsets[Direction.LEFT] = Vector2(0f, -10f)
        leftLegBaseOffsets[Direction.RIGHT] = Vector2(0f, -10f)

        rightLegBaseOffsets[Direction.UP] = Vector2(4f, -10f)
        rightLegBaseOffsets[Direction.DOWN] = Vector2(-4f, -10f)
        rightLegBaseOffsets[Direction.LEFT] = Vector2(0f, -10f)
        rightLegBaseOffsets[Direction.RIGHT] = Vector2(0f, -10f)

        leftArmOffsets.putAll(leftArmBaseOffsets)
        rightArmOffsets.putAll(rightArmBaseOffsets)
        leftHandOffsets.putAll(leftHandBaseOffsets)
        rightHandOffsets.putAll(rightHandBaseOffsets)
        leftLegOffsets.putAll(leftLegBaseOffsets)
        rightLegOffsets.putAll(rightLegBaseOffsets)

        Direction.entries.forEach { dir ->
            leftArmRotations[dir] = 0f
            rightArmRotations[dir] = 0f
            leftHandRotations[dir] = 0f
            rightHandRotations[dir] = 0f
            leftLegRotations[dir] = 0f
            rightLegRotations[dir] = 0f
        }
    }

    override fun setWeaponAnimation(animationProgress: Float, attackType: String) {
        weaponAnimationProgress = animationProgress
        weaponAttackType = attackType
    }

    override fun renderIdle(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        updateBlink()
        headDynamicOffsets[direction] = headOffsets[direction]!!.cpy()
        eyesOffsets[direction]?.let { baseOffset ->
            eyesDynamicOffsets[direction] = baseOffset.cpy()
        }

        if (weaponAnimationProgress > 0f) {
            animateWeaponAttack(direction)
        } else {
            resetArmsToIdle(direction)
        }

        renderBodyParts(batch, x, y, scale, direction)
    }

    override fun renderMoving(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        updateBlink()

        if (weaponAnimationProgress > 0f) {
            animateWeaponAttack(direction)
        } else {
            animateWalking(direction)
        }

        renderBodyParts(batch, x, y, scale, direction)
    }

    override fun renderDead(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        isBlinking = true
        renderBodyParts(batch, x, y, scale, direction)
    }

    private fun resetArmsToIdle(direction: Direction) {
        leftArmOffsets[direction] = leftArmBaseOffsets[direction]!!.cpy()
        rightArmOffsets[direction] = rightArmBaseOffsets[direction]!!.cpy()
        leftHandOffsets[direction] = leftHandBaseOffsets[direction]!!.cpy()
        rightHandOffsets[direction] = rightHandBaseOffsets[direction]!!.cpy()

        leftArmRotations[direction] = 0f
        rightArmRotations[direction] = 0f
        leftHandRotations[direction] = 0f
        rightHandRotations[direction] = 0f
    }

    private fun animateWeaponAttack(direction: Direction) {
        val progress = weaponAnimationProgress.coerceIn(0f, 1f)

        // Deleguj animację do odpowiedniej broni
        weaponSkin.animateAttack(
            progress, direction,
            leftArmBaseOffsets, rightArmBaseOffsets,
            leftHandBaseOffsets, rightHandBaseOffsets,
            leftArmOffsets, rightArmOffsets,
            leftHandOffsets, rightHandOffsets,
            leftArmRotations, rightArmRotations,
            leftHandRotations, rightHandRotations,
            armLength
        )
    }

    private fun updateBlink() {
        blinkTimer += 0.016f

        if (!isBlinking && blinkTimer >= blinkInterval) {
            isBlinking = true
            blinkTimer = 0f
        } else if (isBlinking && blinkTimer >= blinkDuration) {
            isBlinking = false
            blinkTimer = 0f
        }
    }

    private fun animateWalking(direction: Direction) {
        val headBob = sin(animationTime.toDouble() * headBobSpeed).toFloat() * headBobAmplitude

        val baseHeadOffset = headOffsets[direction]!!
        headDynamicOffsets[direction] = Vector2(baseHeadOffset.x, baseHeadOffset.y + headBob)

        val baseEyesOffset = eyesOffsets[direction]
        if (baseEyesOffset != null) {
            eyesDynamicOffsets[direction] = Vector2(baseEyesOffset.x, baseEyesOffset.y + headBob)
        }

        val armSwingAmplitude = if (direction == Direction.UP || direction == Direction.DOWN) 8f else 35f
        val armRotation = sin(animationTime.toDouble() * armSwingSpeed).toFloat() * armSwingAmplitude

        val legSwingAmplitude = if (direction == Direction.UP || direction == Direction.DOWN) 20f else 30f
        val legRotation = sin(animationTime.toDouble() * legSwingSpeed).toFloat() * legSwingAmplitude

        when (direction) {
            Direction.LEFT, Direction.RIGHT -> {
                val baseLeftHand = leftHandBaseOffsets[direction]!!
                val baseRightHand = rightHandBaseOffsets[direction]!!

                leftArmRotations[direction] = armRotation
                val leftArmRadians = Math.toRadians(armRotation.toDouble())
                val leftHandX = baseLeftHand.x + sin(leftArmRadians).toFloat() * armLength
                val leftHandY = baseLeftHand.y - cos(leftArmRadians).toFloat() * armLength + armLength
                leftHandOffsets[direction] = Vector2(leftHandX, leftHandY)
                leftHandRotations[direction] = armRotation

                rightArmRotations[direction] = -armRotation
                val rightArmRadians = Math.toRadians(-armRotation.toDouble())
                val rightHandX = baseRightHand.x + sin(rightArmRadians).toFloat() * armLength
                val rightHandY = baseRightHand.y - cos(rightArmRadians).toFloat() * armLength + armLength
                rightHandOffsets[direction] = Vector2(rightHandX, rightHandY)
                rightHandRotations[direction] = -armRotation

                leftLegRotations[direction] = legRotation
                rightLegRotations[direction] = -legRotation
            }

            Direction.UP, Direction.DOWN -> {
                val baseLeftHand = leftHandBaseOffsets[direction] ?: leftHandBaseOffsets[Direction.RIGHT]!!
                val baseRightHand = rightHandBaseOffsets[direction] ?: rightHandBaseOffsets[Direction.RIGHT]!!

                leftArmRotations[direction] = armRotation
                rightArmRotations[direction] = armRotation

                val armRadians = Math.toRadians(armRotation.toDouble())

                val leftHandX = baseLeftHand.x + sin(armRadians).toFloat() * armLength
                val leftHandY = baseLeftHand.y - cos(armRadians).toFloat() * armLength + armLength
                leftHandOffsets[direction] = Vector2(leftHandX, leftHandY)
                leftHandRotations[direction] = armRotation

                val rightHandX = baseRightHand.x + sin(armRadians).toFloat() * armLength
                val rightHandY = baseRightHand.y - cos(armRadians).toFloat() * armLength + armLength
                rightHandOffsets[direction] = Vector2(rightHandX, rightHandY)
                rightHandRotations[direction] = armRotation

                val baseLeftLeg = leftLegBaseOffsets[direction]!!
                val baseRightLeg = rightLegBaseOffsets[direction]!!

                val leftLegLift = if (legRotation > 0) legRotation else 0f
                val rightLegLift = if (legRotation < 0) -legRotation else 0f

                leftLegOffsets[direction] = Vector2(baseLeftLeg.x, baseLeftLeg.y + leftLegLift * 0.3f)
                rightLegOffsets[direction] = Vector2(baseRightLeg.x, baseRightLeg.y + rightLegLift * 0.3f)

                leftLegRotations[direction] = 0f
                rightLegRotations[direction] = 0f
            }
        }
    }

    private fun renderBodyParts(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        val renderOrder = when (direction) {
            Direction.UP -> listOf(
                "left_leg", "right_leg", "weapon", "left_arm", "left_hand",
                "right_arm", "right_hand", "body", "head", "eyes"
            )
            Direction.DOWN -> listOf(
                "left_leg", "right_leg", "left_arm", "left_hand", "right_arm",
                "body", "head", "weapon", "right_hand", "eyes"
            )
            Direction.LEFT -> listOf(
                "weapon", "right_leg", "left_leg", "right_arm", "right_hand",
                "body", "head", "eyes", "left_hand", "left_arm"
            )
            Direction.RIGHT -> listOf(
                "left_leg", "right_leg", "left_arm", "left_hand",
                "body", "head", "eyes", "weapon", "right_hand", "right_arm"
            )
        }

        for (partName in renderOrder) {
            when (partName) {
                "head" -> renderHead(batch, x, y, scale, direction)
                "eyes" -> renderEyes(batch, x, y, scale, direction)
                "body" -> renderBody(batch, x, y, scale, direction)
                "left_arm" -> renderLeftArm(batch, x, y, scale, direction)
                "right_arm" -> renderRightArm(batch, x, y, scale, direction)
                "left_hand" -> renderLeftHand(batch, x, y, scale, direction)
                "right_hand" -> renderRightHand(batch, x, y, scale, direction)
                "left_leg" -> renderLeftLeg(batch, x, y, scale, direction)
                "right_leg" -> renderRightLeg(batch, x, y, scale, direction)
                "weapon" -> {
                    val rightHandOffset = rightHandOffsets[direction] ?: Vector2(0f, 0f)
                    val rightHandRot = rightHandRotations[direction] ?: 0f

                    weaponSkin.renderWithHand(
                        batch, x, y, scale,
                        weaponAnimationProgress, weaponAttackType, direction,
                        rightHandOffset, rightHandRot
                    )
                }
            }
        }
    }

    private fun renderHead(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        val headTexture = headTextures[direction] ?: return
        val headOffset = headDynamicOffsets[direction] ?: headOffsets[direction]!!

        val partWidth = headTexture.width.toFloat()
        val partHeight = headTexture.height.toFloat()

        val finalX = x + headOffset.x * scale - (partWidth * headScale * scale) / 2
        val finalY = y + headOffset.y * scale - (partHeight * headScale * scale) / 2

        batch.draw(
            headTexture, finalX, finalY,
            partWidth * headScale * scale / 2, partHeight * headScale * scale / 2,
            partWidth * headScale * scale, partHeight * headScale * scale,
            1f, 1f, 0f, 0, 0, headTexture.width, headTexture.height, false, false
        )
    }

    private fun renderBody(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        val bodyTexture = bodyTextures[direction] ?: return
        val bodyOffset = bodyOffsets[direction]!!

        val partWidth = bodyTexture.width.toFloat()
        val partHeight = bodyTexture.height.toFloat()

        val finalX = x + bodyOffset.x * scale - (partWidth * bodyScale * scale) / 2
        val finalY = y + bodyOffset.y * scale - (partHeight * bodyScale * scale) / 2

        batch.draw(
            bodyTexture, finalX, finalY,
            partWidth * bodyScale * scale / 2, partHeight * bodyScale * scale / 2,
            partWidth * bodyScale * scale, partHeight * bodyScale * scale,
            1f, 1f, 0f, 0, 0, bodyTexture.width, bodyTexture.height, false, false
        )
    }

    private fun renderLeftArm(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        val armTexture = leftArmTextures[direction] ?: return
        val armOffset = leftArmOffsets[direction]!!
        val armRotation = leftArmRotations[direction]!!

        val partWidth = armTexture.width.toFloat()
        val partHeight = armTexture.height.toFloat()

        val finalX = x + armOffset.x * scale - (partWidth * armScale * scale) / 2
        val finalY = y + armOffset.y * scale - (partHeight * armScale * scale) / 2

        batch.draw(
            armTexture, finalX, finalY,
            partWidth * armScale * scale / 2, partHeight * armScale * scale - 5f,
            partWidth * armScale * scale, partHeight * armScale * scale,
            1f, 1f, armRotation, 0, 0, armTexture.width, armTexture.height, false, false
        )
    }

    private fun renderRightArm(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        val armTexture = rightArmTextures[direction] ?: return
        val armOffset = rightArmOffsets[direction]!!
        val armRotation = rightArmRotations[direction]!!

        val partWidth = armTexture.width.toFloat()
        val partHeight = armTexture.height.toFloat()

        val finalX = x + armOffset.x * scale - (partWidth * armScale * scale) / 2
        val finalY = y + armOffset.y * scale - (partHeight * armScale * scale) / 2

        batch.draw(
            armTexture, finalX, finalY,
            partWidth * armScale * scale / 2, partHeight * armScale * scale - 5f,
            partWidth * armScale * scale, partHeight * armScale * scale,
            1f, 1f, armRotation, 0, 0, armTexture.width, armTexture.height, false, false
        )
    }

    private fun renderLeftHand(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        val handTexture = leftHandTextures[direction] ?: return
        val handOffset = leftHandOffsets[direction]!!
        val handRotation = leftHandRotations[direction]!!

        val partWidth = handTexture.width.toFloat()
        val partHeight = handTexture.height.toFloat()

        val finalX = x + handOffset.x * scale - (partWidth * handScale * scale) / 2
        val finalY = y + handOffset.y * scale - (partHeight * handScale * scale) / 2

        batch.draw(
            handTexture, finalX, finalY,
            partWidth * handScale * scale / 2, partHeight * handScale * scale / 2,
            partWidth * handScale * scale, partHeight * handScale * scale,
            1f, 1f, handRotation, 0, 0, handTexture.width, handTexture.height, false, false
        )
    }

    private fun renderRightHand(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        val handTexture = rightHandTextures[direction] ?: return
        val handOffset = rightHandOffsets[direction]!!
        val handRotation = rightHandRotations[direction]!!

        val partWidth = handTexture.width.toFloat()
        val partHeight = handTexture.height.toFloat()

        val finalX = x + handOffset.x * scale - (partWidth * handScale * scale) / 2
        val finalY = y + handOffset.y * scale - (partHeight * handScale * scale) / 2

        batch.draw(
            handTexture, finalX, finalY,
            partWidth * handScale * scale / 2, partHeight * handScale * scale / 2,
            partWidth * handScale * scale, partHeight * handScale * scale,
            1f, 1f, handRotation, 0, 0, handTexture.width, handTexture.height, false, false
        )
    }

    private fun renderLeftLeg(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        val legTexture = leftLegTextures[direction] ?: return
        val legOffset = leftLegOffsets[direction]!!
        val legRotation = leftLegRotations[direction]!!

        val partWidth = legTexture.width.toFloat()
        val partHeight = legTexture.height.toFloat()

        val finalX = x + legOffset.x * scale - (partWidth * legScale * scale) / 2
        val finalY = y + legOffset.y * scale - (partHeight * legScale * scale) / 2

        batch.draw(
            legTexture, finalX, finalY,
            partWidth * legScale * scale / 2, partHeight * legScale * scale,
            partWidth * legScale * scale, partHeight * legScale * scale,
            1f, 1f, legRotation, 0, 0, legTexture.width, legTexture.height, false, false
        )
    }

    private fun renderRightLeg(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        val legTexture = rightLegTextures[direction] ?: return
        val legOffset = rightLegOffsets[direction]!!
        val legRotation = rightLegRotations[direction]!!

        val partWidth = legTexture.width.toFloat()
        val partHeight = legTexture.height.toFloat()

        val finalX = x + legOffset.x * scale - (partWidth * legScale * scale) / 2
        val finalY = y + legOffset.y * scale - (partHeight * legScale * scale) / 2

        batch.draw(
            legTexture, finalX, finalY,
            partWidth * legScale * scale / 2, partHeight * legScale * scale,
            partWidth * legScale * scale, partHeight * legScale * scale,
            1f, 1f, legRotation, 0, 0, legTexture.width, legTexture.height, false, false
        )
    }

    private fun renderEyes(batch: SpriteBatch, x: Float, y: Float, scale: Float, direction: Direction) {
        if (direction == Direction.UP) return

        val eyeTexture = if (isBlinking) {
            eyesClosedTextures[direction]
        } else {
            eyesOpenTextures[direction]
        } ?: return

        val currentEyesOffset = eyesDynamicOffsets[direction] ?: eyesOffsets[direction] ?: return

        val eyeWidth = eyeTexture.width.toFloat()
        val eyeHeight = eyeTexture.height.toFloat()

        val finalX = x + currentEyesOffset.x * scale - (eyeWidth * eyesScale * scale) / 2
        val finalY = y + currentEyesOffset.y * scale - (eyeHeight * eyesScale * scale) / 2

        batch.draw(
            eyeTexture, finalX, finalY,
            eyeWidth * eyesScale * scale / 2, eyeHeight * eyesScale * scale / 2,
            eyeWidth * eyesScale * scale, eyeHeight * eyesScale * scale,
            1f, 1f, 0f, 0, 0, eyeTexture.width, eyeTexture.height, false, false
        )
    }

    override fun dispose() {
        headTextures.values.forEach { it.dispose() }
        bodyTextures.values.forEach { it.dispose() }
        leftArmTextures.values.forEach { it.dispose() }
        rightArmTextures.values.forEach { it.dispose() }
        leftHandTextures.values.forEach { it.dispose() }
        rightHandTextures.values.forEach { it.dispose() }
        leftLegTextures.values.forEach { it.dispose() }
        rightLegTextures.values.forEach { it.dispose() }
        eyesOpenTextures.values.toSet().forEach { it.dispose() }
        eyesClosedTextures.values.toSet().forEach { it.dispose() }
        weaponSkin.dispose()
    }
}