package pl.decodesoft.player.skin

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable

interface WeaponSkin : Disposable {

    fun renderWithHand(
        batch: SpriteBatch,
        x: Float,
        y: Float,
        scale: Float,
        animationProgress: Float,
        attackType: String,
        direction: Direction,
        rightHandOffset: Vector2,
        rightHandRotation: Float
    )

    fun render(
        batch: SpriteBatch,
        x: Float,
        y: Float,
        scale: Float,
        animationProgress: Float,
        attackType: String,
        direction: Direction
    )

    fun update(deltaTime: Float)

    fun getBaseOffset(): Pair<Float, Float>

    fun getAnimationTransform(
        progress: Float,
        scale: Float,
        attackType: String,
        direction: Direction
    ): Triple<Float, Float, Float>

    // Nowa metoda do animowania rąk
    fun animateAttack(
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
    )
}