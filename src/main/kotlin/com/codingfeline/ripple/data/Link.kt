package com.codingfeline.ripple.data

import com.codingfeline.ripple.NodeId

data class Link(
    val to: NodeId,
    val totalLatency: Int,
    var lmSendTime: Int = 0,
    var lmReceiveTime: Int = 0,
    var lm: Message? = null
) {
}
