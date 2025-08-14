package pl.decodesoft.player.manager

import pl.decodesoft.player.model.CharacterInfo

object SpawnManager {
    private val defaultSpawn = Pair(500f, 600f)

    fun getDefaultSpawn(): Pair<Float, Float> {
        return defaultSpawn
    }

    fun getSpawnForCharacter(character: CharacterInfo): Pair<Float, Float> {
        return Pair(character.lastX, character.lastY)
    }
}