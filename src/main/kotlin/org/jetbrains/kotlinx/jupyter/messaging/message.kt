package org.jetbrains.kotlinx.jupyter.messaging

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.jetbrains.kotlinx.jupyter.api.libraries.RawMessage
import org.jetbrains.kotlinx.jupyter.protocol.MessageFormat
import org.jetbrains.kotlinx.jupyter.protocol.RawMessageImpl
import org.jetbrains.kotlinx.jupyter.protocol.data
import org.jetbrains.kotlinx.jupyter.protocolVersion
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val ISO8601DateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSxxx'['VV']'")
internal val ISO8601DateNow: String get() = ZonedDateTime.now().format(ISO8601DateFormatter)

fun RawMessage.toMessage(): Message {
    return Message(id, MessageFormat.decodeFromJsonElement(data))
}

data class Message(
    val id: List<ByteArray> = listOf(),
    val data: MessageData = MessageData(),
) {
    val type: MessageType
        get() = data.header!!.type

    val content: MessageContent
        get() = data.content!!

    override fun toString(): String =
        "msg[${id.joinToString { it.toString(charset = Charsets.UTF_8) }}] " +
            MessageFormat.encodeToString(data)
}

fun Message.toRawMessage(): RawMessage {
    val dataJson = MessageFormat.encodeToJsonElement(data).jsonObject
    return RawMessageImpl(
        id,
        dataJson["header"]!!.jsonObject,
        dataJson["parent_header"] as? JsonObject,
        dataJson["metadata"] as? JsonObject,
        dataJson["content"]!!,
    )
}

@JvmName("jsonObjectForString")
fun jsonObject(vararg namedValues: Pair<String, String?>): JsonObject = MessageFormat.encodeToJsonElement(hashMapOf(*namedValues)) as JsonObject

@JvmName("jsonObjectForInt")
fun jsonObject(vararg namedValues: Pair<String, Int>): JsonObject = MessageFormat.encodeToJsonElement(hashMapOf(*namedValues)) as JsonObject

@JvmName("jsonObjectForPrimitive")
fun jsonObject(vararg namedValues: Pair<String, JsonElement>): JsonObject = MessageFormat.encodeToJsonElement(hashMapOf(*namedValues)) as JsonObject

fun jsonObject(namedValues: Iterable<Pair<String, Any?>>): JsonObject = buildJsonObject {
    namedValues.forEach { (key, value) -> put(key, MessageFormat.encodeToJsonElement(value)) }
}

internal operator fun JsonObject?.get(key: String) = this?.get(key)

fun makeReplyMessage(
    msg: RawMessage,
    msgType: MessageType? = null,
    sessionId: String? = null,
    header: MessageHeader? = null,
    parentHeader: MessageHeader? = null,
    metadata: JsonElement? = null,
    content: MessageContent? = null,
) =
    Message(
        id = msg.id,
        MessageData(
            header = header ?: makeHeader(msgType = msgType, incomingMsg = msg, sessionId = sessionId),
            parentHeader = parentHeader ?: MessageFormat.decodeFromJsonElement<MessageHeader>(msg.header),
            metadata = metadata,
            content = content,
        ),
    )

fun makeHeader(msgType: MessageType? = null, incomingMsg: RawMessage? = null, sessionId: String? = null): MessageHeader {
    val parentHeader = incomingMsg?.header?.let { MessageFormat.decodeFromJsonElement<MessageHeader>(it) }
    return makeHeader(
        msgType ?: MessageType.NONE,
        parentHeader?.session ?: sessionId,
        parentHeader?.username ?: "kernel",
    )
}

fun makeHeader(type: MessageType, sessionId: String?, username: String?): MessageHeader {
    return MessageHeader(
        UUID.randomUUID().toString(),
        type,
        sessionId,
        username,
        protocolVersion,
        ISO8601DateNow,
    )
}
