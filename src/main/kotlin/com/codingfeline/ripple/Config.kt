package com.codingfeline.ripple

object Config {

    const val RANDOM_SEED = 1L

    const val NODES = 1000
    const val MALICIOUS_NODES = 15
    const val CONSENSUS_PERCENT = 80

    const val MIN_E2C_LATENCY = 5
    const val MAX_E2C_LATENCY = 50
    const val MIN_C2C_LATENCY = 5
    const val MAX_C2C_LATENCY = 200

    const val OUT_BOUND_LINKS = 10

    const val UNL_MIN = 20
    const val UNL_MAX = 30
    // unl datapoints we have to have before we change position
    const val UNL_THRESHOLD = UNL_MIN / 2

    // extra time we delay a message to coalesce/suppress
    const val BASE_DELAY = 1

    // how many UNL votes you give yourself
    const val SELF_WEIGHT = 1

    // how many packets can be "on the wire" per link per direction
    // simulates non-infinite bandwidth
    const val PACKETS_ON_WIRE = 3
}
