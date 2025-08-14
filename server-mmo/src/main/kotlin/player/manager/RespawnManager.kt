package pl.decodesoft.player.manager

object RespawnManager {
    private val respawnPoint = Pair(700f, 200f) // kordy respawnu po smierci

    fun getRespawnPoint(): Pair<Float, Float> = respawnPoint
}