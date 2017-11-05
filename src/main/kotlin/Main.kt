import com.vdurmont.emoji.EmojiManager
import org.slf4j.LoggerFactory
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IEmoji
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
    val index = if (list.isNotEmpty()) Math.abs(rand.nextInt())%list.size else 0
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

fun main(args: Array<String>) {


    if (args.isEmpty()) {
        System.err.println("Please specify bot token!")
        exitProcess(1)
    }

    val client = ClientBuilder().withToken(args[0])
            .build()

    client.dispatcher.registerListener(Reactions())
//    client.dispatcher.registerListener(Hello())

    client.login()
}

class Reactions {

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {

        logger.trace("Received message ${event.message}")

        if (oneOf(100)) {
            logger.trace("Adding heart reaction")
            val emoji = oneOf(EmojiManager.getAll())
            logger.debug("Adding reaction: $emoji")
            event.message.addReaction(emoji)
        }
    }
}

class Hello {
    @EventSubscriber
    fun onGuildCreate(event: GuildCreateEvent) {
        logger.trace("Guild create event $event")

        val bomoji: IEmoji? = event.guild.emojis.find { emoji -> "bo".equals(emoji.name) }

        for (channel in event.guild.channels) {

            // Find hello emoji
            if (bomoji != null) {
                sendMessage(channel, "$bomoji")
            } else {
                sendMessage(channel, "Hello!")
            }
        }
    }
}


