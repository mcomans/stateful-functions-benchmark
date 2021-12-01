package types.product

import types.WrappedMessage

data class GetFrequentlyBoughtTogetherGraph(val top: Int = 3, val depth: Int = 3, val visited: Set<String>) : WrappedMessage
