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

import com.badlogic.gdx.graphics.Color
import pl.decodesoft.MMOGame
import pl.decodesoft.network.BaseMessageHandler
import pl.decodesoft.player.Faction
import pl.decodesoft.player.Race

// Handler obsługujący wiadomości związane z graczami
class PlayerMessageHandler(game: MMOGame) : BaseMessageHandler(game) {
    override val supportedMessageTypes = setOf(
        "JOIN", "MOVE", "MOVE_FAILED", "LEAVE", "XP_GAINED", "LEAVE_WORLD", "LEVEL_UP",
        "FACTION_INFO", "FACTION_UPDATE", "PVP_BLOCKED", "PVP_STATUS"
    )

    override fun handleMessage(parts: List<String>) {
        when(parts[0]) {
            "JOIN" -> handleJoinMessage(parts)
            "MOVE" -> handleMoveMessage(parts)
            "MOVE_FAILED" -> handleMoveFailedMessage(parts)
            "LEAVE" -> handleLeaveMessage(parts)
            "XP_GAINED" -> handleXpGained(parts)
            "LEAVE_WORLD" -> handleLeaveWorld(parts)
            "LEVEL_UP" -> handleLevelUp(parts)
            "FACTION_INFO" -> handleFactionInfo(parts)
            "FACTION_UPDATE" -> handleFactionUpdate(parts)
            "PVP_BLOCKED" -> handlePvpBlocked(parts)
            "PVP_STATUS" -> handlePvpStatus(parts)
        }
    }

    private fun handleJoinMessage(parts: List<String>) {
        if (parts.size >= 22) {  // ZMIENIONE z 21 na 22
            val x = parts[1].toFloat()
            val y = parts[2].toFloat()
            val id = parts[3]
            val playerUsername = parts[4]
            val characterClass = parts[5].toIntOrNull() ?: 2
            val currentHealth = parts[6].toIntOrNull() ?: 100
            val maxHealth = parts[7].toIntOrNull() ?: 100
            val currentMana = parts[8].toIntOrNull() ?: 100
            val maxMana = parts[9].toIntOrNull() ?: 100
            val level = parts[10].toIntOrNull() ?: 1
            val experience = parts[11].toIntOrNull() ?: 0
            val strength = parts[12].toIntOrNull() ?: 0
            val agility = parts[13].toIntOrNull() ?: 0
            val spellPower = parts[14].toIntOrNull() ?: 0
            val stamina = parts[15].toIntOrNull() ?: 0
            val mana = parts[16].toIntOrNull() ?: 0
            val armor = parts[17].toIntOrNull() ?: 0
            val attackSpeed = parts[18].toIntOrNull() ?: 0
            val critChance = parts[19].toDoubleOrNull() ?: 0.0
            val factionString = parts.getOrNull(20) ?: "NONE"
            val faction = Faction.fromString(factionString)
            val raceString = parts.getOrNull(21) ?: "HUMAN"
            val race = Race.fromString(raceString)

            if (id == game.localPlayerId) {
                // Aktualizuj WSZYSTKIE dane lokalnego gracza
                game.characterNickname = playerUsername
                game.localPlayer.x = x
                game.localPlayer.y = y
                game.localPlayer.characterClass = characterClass
                game.localPlayer.currentHealth = currentHealth
                game.localPlayer.maxHealth = maxHealth
                game.localPlayer.currentMana = currentMana
                game.localPlayer.maxMana = maxMana
                game.localPlayer.level = level
                game.localPlayer.experience = experience
                game.localPlayer.strength = strength
                game.localPlayer.agility = agility
                game.localPlayer.spellPower = spellPower
                game.localPlayer.stamina = stamina
                game.localPlayer.mana = mana
                game.localPlayer.armor = armor
                game.localPlayer.attackSpeed = attackSpeed
                game.localPlayer.critChance = critChance
                game.localPlayer.faction = faction
                game.localPlayer.race = race
            } else {
                game.addPlayer(id, x, y, playerUsername, characterClass, currentHealth, maxHealth, currentMana, maxMana, level, experience, faction, race)  // DODANE race
            }
        }
    }

    private fun handleMoveMessage(parts: List<String>) {
        if (parts.size >= 4) {
            val x = parts[1].toFloat()
            val y = parts[2].toFloat()
            val id = parts[3]

            game.updatePlayerPosition(id, x, y)
        }
    }

    private fun handleMoveFailedMessage(parts: List<String>) {
        if (parts.size >= 3) {
            val failedPlayerId = parts[1]
            val reason = parts[2]

            game.handleMoveFailed(failedPlayerId)
        }
    }

    private fun handleLeaveMessage(parts: List<String>) {
        if (parts.size >= 2) {
            val id = parts[1]

            // Używamy metody z MMOGame
            game.removePlayer(id)
        }
    }

    private fun handleXpGained(parts: List<String>) {
        if (parts.size >= 5) {
            val playerId = parts[1]
            val gained = parts[2].toIntOrNull() ?: return
            val currentXp = parts[3].toIntOrNull() ?: return
            val currentLevel = parts[4].toIntOrNull() ?: return

            val player = game.getPlayer(playerId) ?: return
            player.experience = currentXp
            player.level = currentLevel

            println("Gracz ${player.username} zdobył $gained XP (lvl: $currentLevel)")
        }
    }

    private fun handleLevelUp(parts: List<String>) {
        if (parts.size < 17) {
            println("ERROR: Not enough parts in LEVEL_UP message")
            return
        }

        val playerId = parts[1]
        val newLevel = parts[2].toIntOrNull() ?: return
        val newMaxHealth = parts[3].toIntOrNull() ?: return
        val newCurrentHealth = parts[4].toIntOrNull() ?: return
        val currentXp = parts[5].toIntOrNull() ?: return
        val newStrength = parts[6].toIntOrNull() ?: return
        val newAgility = parts[7].toIntOrNull() ?: return
        val newSpellPower = parts[8].toIntOrNull() ?: return
        val newStamina = parts[9].toIntOrNull() ?: return
        val newMaxMana = parts[10].toIntOrNull() ?: return
        val newCurrentMana = parts[11].toIntOrNull() ?: return
        val newBaseMana = parts[12].toIntOrNull() ?: return
        val newArmor = parts[13].toIntOrNull() ?: return
        val newAttackSpeed = parts[14].toIntOrNull() ?: return
        val newDamage = parts[15].toIntOrNull() ?: return
        val newCritChance = parts[16].toDoubleOrNull() ?: 0.0

        val player = game.getPlayer(playerId) ?: return

        // Aktualizuj wszystkie statystyki gracza
        player.level = newLevel
        player.maxHealth = newMaxHealth
        player.currentHealth = newCurrentHealth
        player.experience = currentXp
        player.strength = newStrength
        player.agility = newAgility
        player.spellPower = newSpellPower
        player.stamina = newStamina
        player.mana = newBaseMana
        player.maxMana = newMaxMana
        player.currentMana = newCurrentMana
        player.armor = newArmor
        player.attackSpeed = newAttackSpeed
        player.damage = newDamage
        player.critChance = newCritChance

        // Jeśli to lokalny gracz - pokaż notyfikację
        if (playerId == game.localPlayerId) {
            game.localPlayer.level = newLevel
            game.localPlayer.maxHealth = newMaxHealth
            game.localPlayer.currentHealth = newCurrentHealth
            game.localPlayer.experience = currentXp
            game.localPlayer.strength = newStrength
            game.localPlayer.agility = newAgility
            game.localPlayer.spellPower = newSpellPower
            game.localPlayer.stamina = newStamina
            game.localPlayer.mana = newBaseMana
            game.localPlayer.maxMana = newMaxMana
            game.localPlayer.currentMana = newCurrentMana
            game.localPlayer.armor = newArmor
            game.localPlayer.attackSpeed = newAttackSpeed
            game.localPlayer.damage = newDamage
            game.localPlayer.critChance = newCritChance

            val statName = player.getPrimaryStatName()
            game.showNotification("LEVEL UP! Poziom $newLevel! +1 $statName +1 Stamina +1 Mana", "levelup")
        }
    }

    private fun handleLeaveWorld(parts: List<String>) {
        if (parts.size >= 2) {
            val playerId = parts[1]
            // Obsługa opuszczenia świata
            println("Gracz $playerId opuścił świat")
        }
    }

    private fun handleFactionInfo(parts: List<String>) {
        // Format: FACTION_INFO|playerId1:WATAHA;playerId2:ZAKON;playerId3:NONE
        if (parts.size >= 2) {
            val factionData = parts[1].split(";")

            factionData.forEach { playerFaction ->
                val playerParts = playerFaction.split(":")
                if (playerParts.size == 2) {
                    val playerId = playerParts[0]
                    val factionString = playerParts[1]
                    val faction = Faction.fromString(factionString)

                    // Aktualizuj frakcję gracza
                    if (playerId == game.localPlayerId) {
                        game.localPlayer.faction = faction
                    } else {
                        game.players[playerId]?.faction = faction
                    }
                }
            }

            println("Załadowano informacje o frakcjach ${factionData.size} graczy")
        }
    }

    private fun handleFactionUpdate(parts: List<String>) {
        // Format: FACTION_UPDATE|playerId|WATAHA
        if (parts.size >= 3) {
            val playerId = parts[1]
            val factionString = parts[2]
            val faction = Faction.fromString(factionString)

            if (playerId == game.localPlayerId) {
                game.localPlayer.faction = faction
                game.messageManager.showMessage("Dołączyłeś do frakcji: ${faction.displayName}!", 3f, Color.GOLD)
            } else {
                game.players[playerId]?.let { player ->
                    player.faction = faction
                    game.messageManager.showMessage("${player.username} dołączył do frakcji: ${faction.displayName}", 2f, Color.LIGHT_GRAY)
                }
            }

            println("Gracz $playerId zmienił frakcję na ${faction.displayName}")
        }
    }

    private fun handlePvpBlocked(parts: List<String>) {
        // Format: PVP_BLOCKED|wiadomość
        if (parts.size >= 2) {
            val message = parts[1]
            game.messageManager.showMessage(message, 2f, Color.ORANGE)
        }
    }

    private fun handlePvpStatus(parts: List<String>) {
        // Format: PVP_STATUS|enabled lub PVP_STATUS|disabled
        if (parts.size >= 2) {
            val status = parts[1]
            val isEnabled = status == "enabled"

            val message = if (isEnabled) {
                "PvP między frakcjami jest WŁĄCZONE"
            } else {
                "PvP między frakcjami jest WYŁĄCZONE"
            }

            game.messageManager.showMessage(message, 3f, Color.CYAN)
        }
    }
}