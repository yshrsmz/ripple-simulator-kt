package com.codingfeline.ripple.data

class Network(
    var masterTime: Int = 0,
    val messages: MutableMap<Int, Event> = mutableMapOf()
) {
    fun sendMessage(message: Message, link: Link, sendTime: Int) {
        assert(message.to == link.to)

        link.lmSendTime = sendTime
        link.lmReceiveTime = sendTime + link.totalLatency
        link.lm = messages.putIfAbsent(link.lmReceiveTime, Event())?.addMessage(message)
    }
}
