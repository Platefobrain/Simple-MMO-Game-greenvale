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

import pl.decodesoft.MMOGame
import pl.decodesoft.network.BaseMessageHandler
import pl.decodesoft.player.Faction

class NPCMessageHandler(game: MMOGame) : BaseMessageHandler(game) {
    override val supportedMessageTypes = setOf("NPC_LIST", "NPC_RESPAWN", "NPC_DIED")

    override fun handleMessage(parts: List<String>) {
        when (parts[0]) {
            "NPC_LIST" -> handleNPCListMessage(parts)
            "NPC_RESPAWN" -> handleNPCRespawnMessage(parts)
            "NPC_DIED" -> handleNPCDiedMessage(parts)
        }
    }

    private fun handleNPCListMessage(parts: List<String>) {
        val npcData = parts.getOrNull(1) ?: return
        if (npcData.isEmpty()) return

        npcData.split(";").forEach { data ->
            val npcInfo = data.split(",")
            if (npcInfo.size >= 9) {
                val id = npcInfo[0]
                val name = npcInfo[1]
                val type = npcInfo[2]
                val x = npcInfo[3].toFloatOrNull() ?: return@forEach
                val y = npcInfo[4].toFloatOrNull() ?: return@forEach
                val currentHealth = npcInfo[5].toIntOrNull() ?: 100
                val maxHealth = npcInfo[6].toIntOrNull() ?: 100
                val level = npcInfo[7].toIntOrNull() ?: 1
                val faction = Faction.valueOf(npcInfo[8])

                game.addNPC(id, name, type, x, y, currentHealth, maxHealth, level, faction)
            }
        }
    }

    private fun handleNPCRespawnMessage(parts: List<String>) {
        val respawns = parts.getOrNull(1)?.split(";") ?: return
        respawns.forEach { data ->
            val npcInfo = data.split(",")
            if (npcInfo.size >= 9) {
                val id = npcInfo[0]
                val name = npcInfo[1]
                val type = npcInfo[2]
                val x = npcInfo[3].toFloatOrNull() ?: return@forEach
                val y = npcInfo[4].toFloatOrNull() ?: return@forEach
                val currentHealth = npcInfo[5].toIntOrNull() ?: 100
                val maxHealth = npcInfo[6].toIntOrNull() ?: 100
                val level = npcInfo[7].toIntOrNull() ?: 1
                val faction = Faction.valueOf(npcInfo[8])

                game.respawnNPC(id, name, type, x, y, currentHealth, maxHealth, level, faction)
            }
        }
    }

    private fun handleNPCDiedMessage(parts: List<String>) {
        if (parts.size >= 2) {
            val npcId = parts[1]
            game.markNPCAsDead(npcId)
        }
    }
}