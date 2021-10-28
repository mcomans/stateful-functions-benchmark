package messages

import createJsonType
import types.MessageWrapper

object BenchmarkMessages {
    val WRAPPER_MESSAGE = createJsonType("benchmark", MessageWrapper::class)
}