package me.voxelsquid.thoughts

import me.voxelsquid.bifrost.Bifrost
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.java.JavaPlugin

// I prefer Kotlin. If you need Java code, ask any AI to transmute the gold back to rock.
class PersonalThoughts : JavaPlugin(), Listener {

    // 1. You need Bifrost instance.
    private lateinit var bifrost: Bifrost

    override fun onEnable() {
        this.bifrost = Bifrost.pluginInstance
        if (!bifrost.isEnabled) {
            logger.severe("Bifrost found but not working. Did you configured it?")
            server.pluginManager.disablePlugin(this)
            return
        }
        this.generateRandomPhrases()
        server.pluginManager.registerEvents(this, this)
    }

    // 2. Create data class with EXACT JSON structure you want.
    private var phrases: DynamicPhrases? = null
    data class DynamicPhrases(
        val onRespawnPhrases: List<String>,
        val onNetherEnterPhrases: List<String>,
        val onDiamondOreBreakPhrases: List<String>,
        val onCreeperNearbyExplodePhrases: List<String>
    )

    // 3. Generate the content you want. It can be static (like pre-created phrases), or dynamic (context-based).
    private fun generateRandomPhrases() {

        // 4. Send request to AI. Make sure you correctly described the JSON structure you want in prompt (you can describe logic as well).
        //    Also, pay attention to the response type. On success you have a lambda with generated content.
        bifrost.client.sendRequest(
            prompt = "Your task is to generate the Minecraft player's thoughts after various events and place them in JSON with specified keys: " +
                    "‘onRespawnPhrases’ (array of 15 strings; [try to be humorous]), " +
                    "‘onNetherEnterPhrases’ (array of 15 strings), " +
                    "‘onDiamondOreBreakPhrases’ (array of 15 strings), " +
                    "‘onCreeperNearbyExplodePhrases’ (array of 15 strings [try to be humorously (or sarcastically aggressive) annoyed, joke about creepers (you can do capslock and even swear)])",
            responseType = DynamicPhrases::class,
            onSuccess = { phrases ->
                // 5. Make sure you handle the result. Save it somewhere or.. well.. your choice.
                this.phrases = phrases
            },
            onFailure = { error ->
                println("Error during phrases generation: ${error.message}.")
                error.printStackTrace()
            }
        )

    }

    @EventHandler
    private fun whenPlayerRespawns(event: PlayerRespawnEvent) {
        event.player.sendMessage("§7§o" + phrases!!.onRespawnPhrases.random())
    }

    @EventHandler
    private fun whenPlayerGoesToTheNether(event: PlayerTeleportEvent) {
        if (event.cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            server.scheduler.runTaskLater(this, { _ ->
                if (event.player.location.world!!.name.contains("nether")) {
                    event.player.sendMessage("§7§o" + phrases!!.onNetherEnterPhrases.random())
                }
            }, 20L)
        }
    }

    @EventHandler
    private fun whenPlayerBreaksDiamonds(event: BlockBreakEvent) {
        when (event.block.type) {
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE -> event.player.sendMessage("§7§o" + phrases!!.onDiamondOreBreakPhrases.random())
            else -> {}
        }
    }

    @EventHandler
    private fun whenCreeperExplodes(event: EntityExplodeEvent) {
        if (event.entityType == EntityType.CREEPER) {
            event.entity.getNearbyEntities(10.0, 10.0, 10.0).filterIsInstance<Player>().forEach { it.sendMessage("§7§o" + phrases!!.onCreeperNearbyExplodePhrases.random()) }
        }
    }

}