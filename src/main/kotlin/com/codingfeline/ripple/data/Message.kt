package com.codingfeline.ripple.data

import com.codingfeline.ripple.NodeId

data class Message(
    val from: NodeId,
    val to: NodeId,
    val data: MutableMap<NodeId, NodeState> = mutableMapOf()
) {
    fun addPositions(update: Map<NodeId, NodeState>) {
        update.entries.forEach { (nodeId, nodeState) ->
            if (nodeId != to) {
                val savedCurrentNode = data[nodeId]
                if (savedCurrentNode != null) {
                    // we already had data about this node going in this message

                    if (nodeState.ts > savedCurrentNode.ts && nodeId > 0) {
                        savedCurrentNode.ts = nodeState.ts
                        savedCurrentNode.state = nodeState.state
                    }
                } else {
                    data += nodeId to nodeState
                }
            }
        }
    }

    fun subPositions(received: Map<NodeId, NodeState>) {
        received.entries.forEach { (nodeId, nodeState) ->
            if (nodeId != to) {
                val savedCurrentNode = data[nodeId]
                if (savedCurrentNode != null &&
                    (nodeState.ts >= savedCurrentNode.ts)) {
                    data.remove(nodeId)
                }
            }
        }
    }
}
