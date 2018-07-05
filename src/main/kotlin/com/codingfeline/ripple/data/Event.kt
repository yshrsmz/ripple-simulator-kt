package com.codingfeline.ripple.data

class Event(
    val messages: MutableList<Message> = mutableListOf()
) {
    fun addMessage(message: Message): Message {
        messages.add(message)
        return message
    }
}
