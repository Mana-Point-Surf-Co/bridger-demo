package com.bridger.job.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

interface WsNotifier {
    fun notifyUser(userId: String, payload: Any)
}

@Component
class UserWebSocketHandler(private val objectMapper: ObjectMapper): TextWebSocketHandler(), WsNotifier {
    // Could switch to cache.
    private val connections: MutableMap<String, MutableSet<WebSocketSession>> = ConcurrentHashMap()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.uri?.query
            ?.split("&")
            ?.mapNotNull {
                val kv = it.split("=")
                if (kv.size == 2) kv[0] to kv[1] else null
            }?.toMap()?.get("userId")
            ?: "aloha"

        connections.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
        session.sendMessage(TextMessage("{\"type\":\"ALOHA, WELCOME ABOARD\",\"userId\":\"$userId\"}"))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        connections.values.forEach { it.remove(session) }
    }

    override fun notifyUser(userId: String, payload: Any) {
        val json = objectMapper.writeValueAsString(payload)
        connections[userId]?.forEach { s ->
            if (s.isOpen) s.sendMessage(TextMessage(json))
        }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // TODO:
    }
}