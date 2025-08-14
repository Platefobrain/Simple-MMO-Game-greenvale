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

package pl.decodesoft.player.combat

import io.ktor.websocket.*
import pl.decodesoft.enemy.EnemyLevelManager
import pl.decodesoft.enemy.manager.EnemyManager
import pl.decodesoft.level.LevelManager
import pl.decodesoft.player.manager.UserManager
import pl.decodesoft.player.model.PlayerData
import java.util.concurrent.ConcurrentHashMap

class PlayerCombatManager(
    private val connections: ConcurrentHashMap<String, DefaultWebSocketSession>,
    private val playerPositions: ConcurrentHashMap<String, PlayerData>,
    private val userManager: UserManager,
    private val enemyManager: EnemyManager
) {
    private fun calculateDamage(attackType: String, attackerId: String): Int {
        // Ataki NPC bez gracza
        if (attackType == "ENEMY_BITE") return 2

        // Pobierz dane gracza atakującego
        val attacker = playerPositions[attackerId] ?: return 0

        val characterClass = attacker.characterClass
        val baseDamage = characterClass.baseDamage
        val statBonus = attacker.getPrimaryStat() * characterClass.damageModifier

        return (baseDamage + statBonus).toInt()
    }

    // Główna funkcja obsługująca trafienia
    suspend fun processHitMessage(targetId: String, attackerId: String, attackType: String) {
        // Sprawdź, czy gracz nie atakuje sam siebie
        if (targetId == attackerId) {
            return
        }

        // Oblicz obrażenia z uwzględnieniem statów
        val damage = calculateDamage(attackType, attackerId)

        // Sprawdź czy cel to przeciwnik czy gracz
        if (targetId.startsWith("enemy_")) {
            processEnemyHit(targetId, attackerId, attackType, damage)
        } else {
            processPlayerHit(targetId, attackerId, attackType, damage)
        }
    }

    // Obsługa trafienia przeciwnika
    private suspend fun processEnemyHit(targetId: String, attackerId: String, attackType: String, damage: Int) {
        val enemyId = targetId.substringAfter("enemy_")
        enemyManager.getEnemies().find { it.id == enemyId }?.let { enemy ->
            // Zadaj obrażenia przeciwnikowi
            val died = enemyManager.damageEnemy(enemy.id, damage)

            // Wyślij informację o trafieniu do wszystkich graczy
            val broadcastHitMessage = "HIT|$targetId|$attackerId|$attackType|${enemy.currentHealth}|${enemy.maxHealth}"
            broadcastToAll(broadcastHitMessage)

            // Szczegółowa wiadomość dla atakującego
            val detailedHitMessage = "HIT_DETAILED|$targetId|$attackerId|$attackType|${enemy.currentHealth}|${enemy.maxHealth}|$damage"
            sendToSpecificPlayers(detailedHitMessage)

            // Obsługa śmierci przeciwnika
            if (died) {
                broadcastToAll("ENEMY_DIED|$enemyId")

                // Przyznanie XP graczowi
                playerPositions[attackerId]?.let { attacker ->
                    val xpGain = EnemyLevelManager.calculateExperienceReward(enemy.type, enemy.level)

                    // Użyj nowej funkcji która sprawdza level up
                    val result = LevelManager.addExperience(attacker, xpGain)

                    // Wyślij XP_GAINED do wszystkich
                    val xpMsg = "XP_GAINED|${attacker.id}|$xpGain|${attacker.experience}|${attacker.level}"
                    broadcastToAll(xpMsg)

                    // Jeśli nastąpił level up, wyślij LEVEL_UP
                    if (result.leveledUp) {
                        val levelUpMessage = "LEVEL_UP|${attacker.id}|${result.newLevel}|${result.newMaxHealth}|${result.newCurrentHealth}|${attacker.experience}|${result.newPrimaryStat}|${result.newStamina}"
                        broadcastToAll(levelUpMessage)
                    }

                    // Aktualizacja danych gracza w bazie (PO przyznaniu XP i level up!)
                    try {
                        userManager.getUserById(attackerId)?.let { user ->
                            user.getSelectedCharacter()?.let { character ->
                                // Aktualizuj podstawowe dane
                                character.level = attacker.level
                                character.experience = attacker.experience
                                character.maxHealth = attacker.maxHealth
                                character.currentHealth = attacker.currentHealth

                                // Aktualizuj wszystkie statystyki z PlayerData
                                character.strength = attacker.strength
                                character.agility = attacker.agility
                                character.spellPower = attacker.spellPower
                                character.stamina = attacker.stamina

                                // Zapisz do bazy
                                userManager.updateUser(user)
                                println("Zaktualizowano dane gracza $attackerId w bazie - Level: ${attacker.level}, XP: ${attacker.experience}")
                            }
                        }
                    } catch (e: Exception) {
                        println("Błąd podczas zapisywania danych gracza do bazy: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Obsługa trafienia gracza
    private suspend fun processPlayerHit(targetId: String, attackerId: String, attackType: String, damage: Int) {
        playerPositions[targetId]?.let { targetPlayer ->
            targetPlayer.takeDamage(damage)

            if (targetPlayer.currentHealth < 0) targetPlayer.currentHealth = 0

            userManager.getUserById(targetId)?.let { user ->
                user.getSelectedCharacter()?.let { character ->
                    character.currentHealth = targetPlayer.currentHealth
                    userManager.updateUser(user)
                }
            }

            val broadcastHitMessage = "HIT|$targetId|$attackerId|$attackType|${targetPlayer.currentHealth}|${targetPlayer.maxHealth}"
            broadcastToAll(broadcastHitMessage)

            val detailedHitMessage = "HIT_DETAILED|$targetId|$attackerId|$attackType|${targetPlayer.currentHealth}|${targetPlayer.maxHealth}|$damage"
            sendToSpecificPlayers(detailedHitMessage)

            if (targetPlayer.currentHealth <= 0) {
                targetPlayer.isDead = true
                broadcastToAll("PLAYER_DIED|$targetId")
                println("Gracz $targetId zginął.")
            }
        }
    }

    // Wysyła wiadomość do wszystkich
    private suspend fun broadcastToAll(message: String) {
        connections.forEach { (_, session) ->
            session.send(message)
        }
    }

    // Wysyła wiadomość do określonych graczy
    private suspend fun sendToSpecificPlayers(message: String) {
        connections.forEach { (_, session) ->
            session.send(message)
        }
    }
}