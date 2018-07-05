package com.codingfeline.ripple.data

import com.codingfeline.ripple.Config
import com.codingfeline.ripple.GlobalState
import com.codingfeline.ripple.NodeId

data class Node(
    val n: NodeId,
    var e2cLatency: Int,
    val unl: MutableList<NodeId>,
    val links: MutableList<Link>,
    val nts: MutableList<Int>,
    val knowledge: MutableList<String>,
    var messagesSent: Int = 0,
    var messagesReceived: Int = 0
) {
    companion object {
        fun generate(nodeId: NodeId, nodeCounts: Int): Node {
            return Node(
                n = nodeId,
                nts = MutableList(nodeCounts) { 0 },
                knowledge = MutableList(nodeCounts) { "0" },
                messagesSent = 0,
                messagesReceived = 0,
                links = mutableListOf(),
                unl = mutableListOf(),
                e2cLatency = 0
            )
        }
    }

    fun processMessage(message: Message) {
    }

    fun receiveMessage(message: Message, network: Network) {
        messagesReceived++

        // if we we goint to send any of this data to that node, skip it
        links.forEach {
            if (it.to == message.to && it.lmSendTime >= network.masterTime) {
                // we can still update a waiting outbound message
                it.lm?.subPositions(message.data)
                return@forEach
            }
        }

        // 1) update our knowledge
        val changes = mutableMapOf<NodeId, NodeState>()

        message.data.forEach { nodeId, nodeState ->
            if (nodeId != n &&
                knowledge[nodeId] != nodeState.state &&
                nodeState.ts > nts[nodeId]) {
                // this gives us new information about a node
                knowledge[nodeId] = nodeState.state
                nts[nodeId] = nodeState.ts
                changes += nodeId to nodeState
            }
        }

        // return if nothing changed
        if (changes.isEmpty()) return

        // 2) choose our position change, if any
        var unlCount = 0
        var unlBalance = 0
        unl.forEach {
            when (knowledge[it]) {
                "1" -> {
                    ++unlCount
                    ++unlBalance
                }
                "-1" -> {
                    ++unlCount
                    --unlBalance
                }
            }
        }

        if (n < Config.MALICIOUS_NODES) {
            // if we are a malicious node, be contrarian
            unlBalance = -unlBalance
        }

        // add a bias in favor of 'no' as time passes
        // (agree to disagree)
        unlBalance -= network.masterTime / 250

        var positionChange = false
        if (unlCount >= Config.UNL_THRESHOLD) {
            // we have enough data to make decisions
            if (knowledge[n] == "1" &&
                unlBalance < -Config.SELF_WEIGHT) {

                // we switch to -
                knowledge[n] = "-1"
                --GlobalState.positiveNodes
                ++GlobalState.negativeNodes
                changes[n] = NodeState(n, ++nts[n], "-1")
                positionChange = true
            } else if (knowledge[n] == "-1" &&
                unlBalance > Config.SELF_WEIGHT) {

                knowledge[n] = "1"
                ++GlobalState.positiveNodes
                --GlobalState.negativeNodes
                changes[n] = NodeState(n, ++nts[n], "1")
                positionChange = true
            }
        }

        // 3) broadcast the message
        links.forEach { link ->
            if (positionChange || link.to != message.from) {

                // can we update an unsent message?
                if (link.lmSendTime > network.masterTime) {
                    link.lm?.addPositions(changes)
                } else {
                    // no, we need a new message
                    var sendTime = network.masterTime
                    if (!positionChange) {
                        // delay the message a bit to permit coalescing and suppression
                        sendTime += Config.BASE_DELAY
                        if (link.lmReceiveTime > sendTime) {
                            // a packet is on the wire
                            // pwait a bit extra to send
                            sendTime += link.totalLatency / Config.PACKETS_ON_WIRE
                        }
                    }
                    network.sendMessage(Message(n, link.to, changes), link, sendTime)
                    messagesSent++
                }
            }
        }
    }

    fun isOnUNL(nodeId: Int): Boolean {
        return unl.contains(nodeId)
    }

    fun hasLinkTo(nodeId: Int): Boolean {
        return links.filter { it.to == nodeId }.isNotEmpty()
    }
}
