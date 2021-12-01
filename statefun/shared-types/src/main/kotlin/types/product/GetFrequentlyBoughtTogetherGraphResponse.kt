package types.product

import types.WrappedMessage

data class GetFrequentlyBoughtTogetherGraphResponse(val products: Set<String>) : WrappedMessage