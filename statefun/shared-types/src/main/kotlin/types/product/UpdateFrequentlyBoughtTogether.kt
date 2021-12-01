package types.product

import types.WrappedMessage

data class UpdateFrequentlyBoughtTogether(val productIds: List<String>): WrappedMessage
