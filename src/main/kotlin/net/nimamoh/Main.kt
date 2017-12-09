package net.nimamoh

import com.ibm.watson.developer_cloud.conversation.v1.Conversation
import com.ibm.watson.developer_cloud.conversation.v1.model.Context
import com.ibm.watson.developer_cloud.conversation.v1.model.InputData
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageOptions
import com.vdurmont.emoji.EmojiManager
import org.slf4j.LoggerFactory
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent
import sx.blah.discord.handle.impl.events.guild.GuildEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IEmoji
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.RequestBuffer
import java.util.*
import kotlin.system.exitProcess

val logger = LoggerFactory.getLogger("DumbledoreBot")
val rand = Random()

/** Returns true one of number time*/
fun oneOf(number: Int): Boolean {

    val total = if (number > 0) number else 10000
    val occurence = Math.abs(rand.nextInt())
    val of = occurence % total
    return of == 0
}

/** One of a collection*/
fun <T> oneOf(collection: Collection<T>): T? {
    val list = collection.toList()
    val index = if (list.isNotEmpty()) Math.abs(rand.nextInt()) % list.size else 0
    return if (index == 0) list.singleOrNull() else list.get(index = index)
}

fun sendMessage(channel: IChannel, message: String) {

    RequestBuffer.request {
        try {
            channel.sendMessage(message)
        } catch (e: DiscordException) {
            logger.error("Error when sending message $message", e)
        }
    }
}

/** Watson conversations singleton ************************************************/
object Watson {

    var conversationCourante: WatsonConversation? = null
    val conversationService = Conversation(Conversation.VERSION_DATE_2017_05_26)

    init {
        conversationService.setUsernameAndPassword("e6556242-1461-471a-98ce-9b00a81ca974", "qGnVhuhtbRlt")
    }
}

/** Discord singleton ************/
object Discord {

    var client: IDiscordClient? = null

    fun initializeClient(token: String) {

        client = ClientBuilder().withToken(token).build()

        client?.dispatcher?.registerListener(Reactions())
//        client?.dispatcher?.registerListener(WatsonConversation())
        client?.dispatcher?.registerListener(WatsonConversationPicker())

        client?.login()
    }
}

fun main(args: Array<String>) {


    if (args.isEmpty()) {
        System.err.println("Please specify bot token!")
        exitProcess(1)
    }

    Discord.initializeClient(args[0])
}

/** Une conversation avec le module dumblebot de watson */
class WatsonConversation {

    var context: Context? = null

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {

        logger.debug("Received ${event.message}. Sending to Watson...")
        val messageOptions = MessageOptions.Builder()
                .workspaceId("1d5a2c20-fc44-41fb-afd1-27b0819a0602")
                .input(InputData.Builder(event.message?.content).build())
                .context(context)
                .build()

        val response = Watson.conversationService.message(messageOptions).execute()
        logger.trace("Received response ${response}")
        context = response.context

        for (line in response.output.text) {
            respond(event.guild.channels, line)
        }

        /* Gestion de l'evenement de fin de conversation **********************/
        val action: String? = response.output?.get("action") as String?
        if ("end_conversation".equals(action, true) || "!q".equals(event.message?.content)) { // TODO: valeur en dur pour quitter la conversation
            logger.debug("Removing Watson listener...")
            Discord.client?.dispatcher?.unregisterListener(this)
            Watson.conversationCourante = null // FIN DE LA CONVERSATION. UNREGISTER
        }
    }
}

/** Listener qui démarre une conversation au hasard */
class WatsonConversationPicker {

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {

        if (Watson.conversationCourante == null && oneOf(5)) {
            logger.debug("Dumblebot initializes dialog")
            val conversation = WatsonConversation()
            Watson.conversationCourante = conversation // DEBUG DE LA CONVERSATION, REGISTER
            Discord.client?.dispatcher?.registerListener(conversation)
        } else {
            logger.debug("Dumblebot chose not to begin anew dialog")
        }
    }
}

/** Réactions par emote du bot */
class Reactions {

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {

        logger.trace("Received message ${event.message}")

        if (oneOf(100)) {
            val emoji = oneOf(EmojiManager.getAll())
            logger.debug("Adding reaction: $emoji")
            event.message.addReaction(emoji)
        }
    }
}

/** Lorsque le bot rejoint un salon, il dit bonjour*/
class Hello {
    @EventSubscriber
    fun onGuildCreate(event: GuildCreateEvent) {
        logger.trace("Guild create event $event")

        val bomoji: IEmoji? = bomoji(event)

        respond(event.guild.channels, ":bo:")
    }
}

/* Utils ************/
fun bomoji(event: GuildEvent): IEmoji? {

    return event.guild.emojis.findLast { emoji -> "bo".equals(emoji.name) }
}

fun bomoji(guild: IGuild): IEmoji? {

    return guild.emojis.findLast { emoji -> "bo".equals(emoji.name) }
}

fun respond(channel: IChannel, text: String) {

    val bomoji: IEmoji? = bomoji(channel.guild)

    if (":bo:".equals(text) && bomoji != null) {
        sendMessage(channel, "$bomoji")
    } else {
        sendMessage(channel, text)
    }
}

fun respond(channels: List<IChannel>, text: String) {
    for (channel in channels) {
        respond(channel, text)
    }
}


