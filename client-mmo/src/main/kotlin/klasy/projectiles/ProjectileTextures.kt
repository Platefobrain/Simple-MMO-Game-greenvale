package pl.decodesoft.klasy.projectiles

import com.badlogic.gdx.graphics.Texture

object ProjectileTextures {
    lateinit var arrow: Texture
        private set
    lateinit var fireball: Texture
        private set

    fun load() {
        arrow = Texture("textures/archer/arrows/arrow.png")
        fireball = Texture("textures/mage/spell/fireball.png")
    }

    fun dispose() {
        arrow.dispose()
        fireball.dispose()
    }
}