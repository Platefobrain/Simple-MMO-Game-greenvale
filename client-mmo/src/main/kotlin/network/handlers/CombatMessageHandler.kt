/*
 * This file is part of [GreenVale]
 *
 * [GreenVale] is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * [GreenVale] is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with [GreenVale].  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.decodesoft.network.handlers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import pl.decodesoft.MMOGame
import pl.decodesoft.network.BaseMessageHandler
import pl.decodesoft.npc.NPCClient

// Handler obsługujący wiadomości związane z walką
class CombatMessageHandler(game: MMOGame) : BaseMessageHandler(game) {
    override val supportedMessageTypes = setOf(
        "HIT", "HIT_DETAILED", "HEALTH_UPDATE", "MANA_UPDATE", "PLAYER_DIED", "RESPAWN",
        "RANGED_ATTACK", "SPELL_ATTACK", "MELEE_ATTACK", "COOLDOWN", "OUT_OF_RANGE",
        "NOT_ENOUGH_MANA", "ATTACK_BLOCKED", "TARGET_DEAD"
    )

    override fun handleMessage(parts: List<String>) {
        when (parts[0]) {
            "HIT" -> handleHitMessage(parts)
            "HIT_DETAILED" -> handleHitDetailedMessage(parts)
            "HEALTH_UPDATE" -> handleHealthUpdateMessage(parts)
            "MANA_UPDATE" -> handleManaUpdateMessage(parts)
            "PLAYER_DIED" -> handlePlayerDiedMessage(parts)
            "RESPAWN" -> handleRespawnMessage(parts)
            "RANGED_ATTACK", "SPELL_ATTACK", "MELEE_ATTACK" -> handleAttackMessage(parts)
            "COOLDOWN" -> handleCooldownMessage(parts)
            "OUT_OF_RANGE" -> handleOutOfRange(parts)
            "NOT_ENOUGH_MANA" -> handleNotEnoughMana(parts)
            "ATTACK_BLOCKED" -> handleAttackBlocked(parts)
            "TARGET_DEAD" -> handleTargetDead(parts)
        }
    }

    private fun handleHitMessage(parts: List<String>) {
        if (parts.size >= 5) {
            val targetId = parts[1]
            val currentHealth = parts[4].toIntOrNull() ?: 0
            val maxHealth = if (parts.size >= 6) parts[5].toIntOrNull() ?: 100 else 100

            if (targetId.startsWith("enemy_")) {
                val enemyId = targetId.substringAfter("enemy_")
                game.updateEnemyHealthExplicit(enemyId, currentHealth, maxHealth)
            } else if (targetId.startsWith("npc_")) {
                val npcId = targetId.substringAfter("npc_")
                game.npcs[npcId]?.let { npc ->
                    game.npcs[npcId] = NPCClient(
                        npc.id, npc.name, npc.type, npc.x, npc.y,
                        currentHealth, maxHealth, npc.level, npc.isSelected,
                        npc.faction
                    )
                }
            } else {
                game.updatePlayerHealth(targetId, currentHealth, maxHealth)
            }
        }
    }

    private fun handleHitDetailedMessage(parts: List<String>) {
        if (parts.size >= 8) {
            val targetId = parts[1]
            val attackerId = parts[2]
            val attackType = parts[3]
            val currentHealth = parts[4].toIntOrNull() ?: 0
            val maxHealth = parts[5].toIntOrNull() ?: 100
            val damage = parts[6].toIntOrNull() ?: 0
            val isCrit = parts[7].toBooleanStrictOrNull() ?: false

            if (targetId.startsWith("enemy_")) {
                val enemyId = targetId.substringAfter("enemy_")
                game.updateEnemyHealthExplicit(enemyId, currentHealth, maxHealth)

                if (attackerId == game.localPlayerId) {
                    val enemy = game.getEnemy(enemyId)
                    enemy?.let {
                        val damageColor = if (isCrit) Color.ORANGE else Color.WHITE
                        game.addDamageText(it.x, it.y + 20f, "-$damage", damageColor)
                    }
                }
            } else if (targetId.startsWith("npc_")) {
                val npcId = targetId.substringAfter("npc_")

                if (attackerId == game.localPlayerId) {
                    game.npcs[npcId]?.let { npc ->
                        val damageColor = if (isCrit) Color.ORANGE else Color.WHITE
                        game.addDamageText(npc.x, npc.y + 20f, "-$damage", damageColor)
                    }
                }
            } else {
                game.updatePlayerHealth(targetId, currentHealth, maxHealth)

                if (targetId == game.localPlayerId) {
                    val damageColor = if (isCrit) Color.ORANGE else Color.RED
                    game.gameUI?.addPlayerDamageText("-$damage", damageColor)
                }

                if ((targetId == game.localPlayerId || attackerId == game.localPlayerId) && targetId != game.localPlayerId) {
                    val player = game.getPlayer(targetId)
                    player?.let {
                        val damageColor = if (isCrit) Color.ORANGE else Color.WHITE
                        game.addDamageText(it.x, it.y + 20f, "-$damage", damageColor)
                    }
                }
            }
        }
    }

    private fun handleHealthUpdateMessage(parts: List<String>) {
        if (parts.size >= 4) {
            val playerId = parts[1]
            val currentHealth = parts[2].toIntOrNull() ?: 0
            val maxHealth = parts[3].toIntOrNull() ?: 100

            game.updatePlayerHealth(playerId, currentHealth, maxHealth)
        }
    }

    // mana update
    private fun handleManaUpdateMessage(parts: List<String>) {
        if (parts.size >= 4) {
            val playerId = parts[1]
            val currentMana = parts[2].toIntOrNull() ?: 0
            val maxMana = parts[3].toIntOrNull() ?: 100

            game.updatePlayerMana(playerId, currentMana, maxMana)
        }
    }

    private fun handlePlayerDiedMessage(parts: List<String>) {
        if (parts.size >= 2) {
            val playerId = parts[1]
            game.handlePlayerDeath(playerId)
        }
    }

    private fun handleRespawnMessage(parts: List<String>) {
        if (parts.size >= 4) {
            val playerId = parts[1]
            val currentHealth = parts[2].toIntOrNull() ?: 100
            val maxHealth = parts[3].toIntOrNull() ?: 100

            // Sprawdź czy wiadomość zawiera pozycję (nowy format)
            if (parts.size >= 6) {
                val x = parts[4].toFloatOrNull() ?: 0f
                val y = parts[5].toFloatOrNull() ?: 0f

                game.respawnPlayerWithPosition(playerId, currentHealth, maxHealth, x, y)
            } else {
                // Stary format bez pozycji
                game.respawnPlayerHealth(playerId, currentHealth, maxHealth)
            }
        }
    }

    private fun handleAttackMessage(parts: List<String>) {
        game.handleAttackMessage(parts[0], parts)

        // Sprawdź czy wiadomość ma wszystkie potrzebne dane
        if (parts.size >= 7) {
            val attackType = parts[0]
            val startX = parts[1].toFloat()
            val startY = parts[2].toFloat()
            val targetX = parts[3].toFloat()
            val targetY = parts[4].toFloat()
            val attackerId = parts[5]
            val targetId = parts[6]

            // === TWORZENIE POCISKU DLA LOKALNEGO GRACZA ===
            // Tylko jeśli to lokalny gracz atakuje, utwórz pocisk wizualny
            if (attackerId == game.localPlayerId) {
                val dirX = targetX - startX
                val dirY = targetY - startY
                val distance = kotlin.math.sqrt(dirX * dirX + dirY * dirY)
                val normalizedDirX = dirX / distance
                val normalizedDirY = dirY / distance

                val characterClass = game.playerController.getCharacterClass()

                when (attackType) {
                    "RANGED_ATTACK" -> {
                        // Utwórz strzałę
                        val arrow = pl.decodesoft.klasy.projectiles.Arrow(
                            startX, startY,
                            normalizedDirX, normalizedDirY,
                            attackerId, targetId,
                            targetX, targetY
                        )
                        characterClass.skileManager.addSkill(arrow)
                    }
                    "SPELL_ATTACK" -> {
                        // Utwórz kulę ognia
                        val fireball = pl.decodesoft.klasy.projectiles.Fireball(
                            startX, startY,
                            normalizedDirX, normalizedDirY,
                            attackerId, targetId,
                            targetX, targetY
                        )
                        characterClass.skileManager.addSkill(fireball)
                    }
                    "MELEE_ATTACK" -> {
                        // Utwórz atak mieczem
                        val sword = pl.decodesoft.klasy.projectiles.Sword(
                            startX, startY,
                            normalizedDirX, normalizedDirY,
                            attackerId, targetId,
                            targetX, targetY
                        )
                        characterClass.skileManager.addSkill(sword)
                    }
                }

                // === RESETUJ COOLDOWN DOPIERO TERAZ (po zatwierdzeniu przez serwer) ===
                characterClass.resetAttackCooldown()
            }

            // Uruchom animację broni dla gracza który atakuje
            val attackDirection = game.playerSkinManager.calculateDirectionToTarget(
                startX, startY, targetX, targetY
            )
            game.playerSkinManager.startWeaponAnimation(attackerId, attackType, attackDirection)

            // Przykładowa wiadomość tekstowa w czacie
            val readableAttackType = when (attackType) {
                "MELEE_ATTACK" -> "atakuje wręcz"
                "RANGED_ATTACK" -> "strzela do"
                "SPELL_ATTACK" -> "rzuca czar na"
                else -> "atakują"
            }

            Gdx.app.postRunnable {
                game.receiveNetworkCombatLog("$attackerId $readableAttackType $targetId")
            }
        } else {
            println("DEBUG ERROR: Not enough parts! Expected >= 7, got ${parts.size}")
        }
    }

    // cooldown
    private fun handleCooldownMessage(parts: List<String>) {
        if (parts.size >= 2 && parts[1] == game.localPlayerId) {
            // Pokaż wiadomość o cooldownie na górze ekranu
            game.messageManager.showMessage("Masz cooldown!", 2f, Color.RED)
        }
    }

    // range
    private fun handleOutOfRange(parts: List<String>) {
        if (parts.size >= 2 && parts[1] == game.localPlayerId) {
            // Pokaż wiadomość o zasięgu na górze ekranu
            game.messageManager.showMessage("Jesteś za daleko!", 2f, Color.RED)
        }
    }

    // not enough mana
    @Suppress("UNUSED_PARAMETER")
    private fun handleNotEnoughMana(parts: List<String>) {
        // ZAWSZE wyświetl komunikat
        game.messageManager.showMessage("Brak many!", 2f, Color.RED)
    }

    // attack blocked (NPC protected)
    private fun handleAttackBlocked(parts: List<String>) {
        // ZAWSZE wyświetl komunikat
        val message = if (parts.size >= 2) parts[1] else "Nie możesz zaatakować tego celu"
        game.messageManager.showMessage(message, 2f, Color.RED)
    }

    // target dead
    private fun handleTargetDead(parts: List<String>) {
        // ZAWSZE wyświetl komunikat
        val message = if (parts.size >= 2) parts[1] else "Cel jest martwy"
        game.messageManager.showMessage(message, 2f, Color.YELLOW)
    }
}