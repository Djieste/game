package gg.rsmod.plugins.content.skills.slayer

import gg.rsmod.game.model.attr.SLAYER_AMOUNT
import gg.rsmod.game.model.attr.SLAYER_ASSIGNMENT
import gg.rsmod.game.model.attr.SLAYER_MASTER
import gg.rsmod.game.model.attr.STARTED_SLAYER
import gg.rsmod.plugins.content.skills.slayer.data.SlayerMaster
import gg.rsmod.plugins.content.skills.slayer.data.slayerData

/**
 * @author Alycia <https://github.com/alycii>
 */

val masters = listOf(Npcs.TURAEL, Npcs.VANNAKA, Npcs.MAZCHNA)
masters.forEach { npcId ->

    val slayerMaster = SlayerMaster.values().firstOrNull { it.id == npcId } ?: return@forEach

    on_npc_option(npc = npcId, option = "talk-to") {
        player.queue {
            chatNpc("'Ello, and what are you after then?")
            val firstOption = when (player.attr.has(STARTED_SLAYER)) {
                true -> "I need another assignment"
                false -> "Who are you?"
            }

            when (options(firstOption, "Do you have anything for trade?", "Er... Nothing...")) {
                FIRST_OPTION -> {
                    if (player.attr.has(STARTED_SLAYER)) {
                        giveTask(this, slayerMaster)
                    } else {
                        chatPlayer("Who are you?")
                        chatNpc("I'm one of the elite Slayer Masters.")
                        when (options("What's a slayer?", "Never heard of you...")) {
                            FIRST_OPTION -> {
                                chatPlayer("What's a slayer?")
                                chatNpc("Oh dear, what do they teach you in school?")
                                chatPlayer("Well.. er...")
                                chatNpc(
                                    "I suppose I'll have to educate you then. A slayer is",
                                    "someone who is trained to fight specific creatures. They",
                                    "know these creatures' every weakness and strength. As",
                                    "you can guess it makes killing them a lot easier."
                                )
                                tutorialDialogue(this, slayerMaster)
                            }

                            SECOND_OPTION -> {
                                chatPlayer("Never heard of you...")
                                chatNpc(
                                    "That's because my foe never lives to tell of me. We",
                                    "slayers are a dangerous bunch."
                                )
                                tutorialDialogue(this, slayerMaster)

                            }
                        }
                    }
                }
                SECOND_OPTION -> {
                    chatPlayer("Do you have anything for trade?")
                    chatNpc("I have a wide selection of Slayer equipment; take a look!")
                    player.openShop("Slayer Equipment")
                }
            }
        }
    }
}

suspend fun tutorialDialogue(it: QueueTask, slayerMaster: SlayerMaster) {
    when (it.options("Wow, can you teach me?", "Sounds useless to me..")) {
        FIRST_OPTION -> {
            it.chatPlayer("Wow, can you teach me?")
            it.chatNpc("Hmm well I'm not so sure...")
            it.chatPlayer("Pleeeaasssse!")
            it.chatNpc(
                "Oh okay then, you twisted my arm. You'll have to train",
                "against specific groups of creatures."
            )
            it.chatPlayer("Okay, what's first?")
            giveTask(it, slayerMaster)
        }
        SECOND_OPTION -> {
            it.chatPlayer("Sounds useless to me.")
            it.chatNpc("Suit yourself.")
        }
    }
}

suspend fun tipsDialogue(it: QueueTask) {
    // TODO: Find all the slayer tip dialogues
    when(it.options("Got any tips for me?", "Okay, great!")) {
        FIRST_OPTION -> {
            it.chatPlayer("Got any tips for me?")
            it.chatNpc("As you're a beginner, I recommend bringing food", "and a good weapon of your choice.")
            it.chatPlayer("Great, thanks!")
        }
        SECOND_OPTION -> {
            it.chatPlayer("Okay, great!")
        }
    }
}

suspend fun giveTask(it: QueueTask, slayerMaster: SlayerMaster) {
    val player = it.player

    if(player.getSlayerAssignment() != null) {
        it.chatNpc("You're still hunting ${player.getSlayerAssignment()!!.identifier.lowercase()}, you have ${player.attr[SLAYER_AMOUNT]} to go. Come", "back when you've finished your task.")
        return
    }

    val assignments = slayerData.getAssignmentsForMaster(slayerMaster)

    // Filter the assignments to only include those that meet the requirements
    val validAssignments = assignments.filter { assignment ->
        assignment.requirement.all { it.hasRequirement(player) }
    }

    if (validAssignments.isNotEmpty()) {
        // Get a random assignment from the valid assignments
        val randomAssignment = validAssignments.random()

        // Get the NPC and amount for the random assignment
        val assignment = randomAssignment.assignment
        val amount = when(randomAssignment.amount) {
            0..0 -> slayerMaster.defaultAmount
            else -> randomAssignment.amount
        }

        player.attr[SLAYER_ASSIGNMENT] = assignment.identifier
        player.attr[SLAYER_AMOUNT] = world.random(amount)
        player.attr[SLAYER_MASTER] = slayerMaster.id
    }


    if(!player.attr.has(STARTED_SLAYER)) {
        player.inventory.add(Item(Items.ENCHANTED_GEM))
        it.chatNpc("We'll start you off hunting ${player.getSlayerAssignment()!!.identifier.lowercase()}, you'll need to kill", "${player.attr[SLAYER_AMOUNT]} of them.")
        it.chatNpc("You'll also need this enchanted gem, it allows Slayer", "Masters like myself to contact you and update you on", "your progress. Don't worry if you lose it, you can buy", "another from any Slayer Master.")
        player.attr[STARTED_SLAYER] = true
        tipsDialogue(it)
    } else {
        it.chatNpc("Excellent, you're doing great. Your new task is to kill", "${player.attr[SLAYER_AMOUNT]} ${player.getSlayerAssignment()!!.identifier}.", npc = player.attr[SLAYER_MASTER]!!)
        tipsDialogue(it)
    }
}