package com.codingfeline.ripple

import com.codingfeline.ripple.data.Link
import com.codingfeline.ripple.data.Message
import com.codingfeline.ripple.data.Network
import com.codingfeline.ripple.data.Node
import com.codingfeline.ripple.data.NodeState
import java.util.Random

class Simulator {

    val random by lazy { Random(Config.RANDOM_SEED) }
    val e2cRandom by lazy { random.ints(Config.MIN_E2C_LATENCY, Config.MAX_E2C_LATENCY).iterator() }
    val c2cRandom by lazy { random.ints(Config.MIN_C2C_LATENCY, Config.MAX_C2C_LATENCY).iterator() }
    val unlRandom by lazy { random.ints(Config.UNL_MIN, Config.UNL_MAX).iterator() }
    val nodesRandom by lazy { random.ints(0, Config.NODES - 1).iterator() }

    val nodes by lazy { mutableListOf<Node>() }

    fun run() {
        println("Creating nodes")

        // create nodes
        0.until(Config.NODES).forEach { nodeId ->
            val node = Node.generate(nodeId, Config.NODES)
            node.e2cLatency = e2cRandom.nextInt()

            // our own position starts as 50/50 split
            if (nodeId % 2 != 0) {
                node.knowledge[nodeId] = "1"
                node.nts[nodeId] = 1
                ++GlobalState.positiveNodes
            } else {
                node.knowledge[nodeId] = "-1"
                node.nts[nodeId] = 1
                ++GlobalState.negativeNodes
            }

            // build our UNL
            val unlCount = unlRandom.nextInt()
            unlCount.downTo(1).forEach {
                val cn = nodesRandom.nextInt()
                if (cn != nodeId &&
                    !node.isOnUNL(cn)) {
                    node.unl.add(cn)
                }
            }
            nodes.add(node)
        }

        println("Nodes: ${nodes.size}")

        println("Creating links")
        0.until(Config.NODES).forEach { nodeId ->

            var links = Config.OUT_BOUND_LINKS
            while (links > 0) {
                val linkTarget = nodesRandom.nextInt()
                if (linkTarget != nodeId &&
                    !nodes[nodeId].hasLinkTo(linkTarget)) {

                    val linkLatency = nodes[nodeId].e2cLatency + nodes[linkTarget].e2cLatency + c2cRandom.nextInt()
                    nodes[nodeId].links.add(Link(to = linkTarget, totalLatency = linkLatency))
                    nodes[linkTarget].links.add(Link(to = nodeId, totalLatency = linkLatency))
                    --links
                }
            }
        }

        val network = Network()

        // trigger all node to make initial broardcasts of their own positions
        println("Creating initial messages")
        nodes.flatMap { node -> node.links.map { link -> node to link } }
            .forEach { (node, link) ->
                val msg = Message(from = node.n, to = link.to)
                msg.data += node.n to NodeState(node = node.n, ts = 1, state = node.knowledge[node.n])
                network.sendMessage(msg, link, 0)
            }
        println("Created ${network.messages.size} events")

        // run simulation
        do {
            if (GlobalState.positiveNodes > (Config.NODES * Config.CONSENSUS_PERCENT / 100)) break

            if (GlobalState.negativeNodes > (Config.NODES * Config.CONSENSUS_PERCENT / 100)) break

            if (network.messages.isEmpty()) {
                println("Fatal: Radio Silence")
                System.exit(0)
            }

            val event = network.messages.minBy { it.key }!!

            if ((event.key / 100) > (network.masterTime / 100)) {
                println("Time: ${event.key} ms - ${GlobalState.positiveNodes}/${GlobalState.negativeNodes}")
            }

            network.masterTime = event.key

            event.value.messages.forEach { msg ->
                if (msg.data.isEmpty()) {
                    // message was never sent
                    --nodes[msg.from].messagesSent
                } else {
                    nodes[msg.to].receiveMessage(msg, network)
                }
            }

            network.messages.remove(event.key)
        } while (true)

        val mc = network.messages.values.sumBy { it.messages.size }

        println("Consensus reached in ${network.masterTime} ms with $mc messages on the wire")
        println("Result: ${GlobalState.positiveNodes}/${GlobalState.negativeNodes}")

        val totalMessagesSent = nodes.sumBy { it.messagesSent }
        println("The average node sent ${totalMessagesSent / Config.NODES} messages")
    }
}
