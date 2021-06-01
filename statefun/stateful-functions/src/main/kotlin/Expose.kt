import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import io.undertow.util.HttpString
import order.OrderFn
import org.apache.flink.statefun.sdk.java.StatefulFunctions
import org.apache.flink.statefun.sdk.java.handler.RequestReplyHandler
import org.apache.flink.statefun.sdk.java.slice.Slice
import org.apache.flink.statefun.sdk.java.slice.Slices
import product.ProductFn
import shoppingcart.ShoppingCartFn
import user.UserFn

object Expose {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Starting...")
        val functions = StatefulFunctions()
            .withStatefulFunction(ShoppingCartFn.SPEC)
            .withStatefulFunction(ProductFn.SPEC)
            .withStatefulFunction(UserFn.SPEC)
            .withStatefulFunction(OrderFn.SPEC)

        val httpServer = Undertow.builder()
            .addHttpListener(1108, "0.0.0.0")
            .setHandler(UndertowStateFunHandler(functions.requestReplyHandler()))
            .build()

        httpServer.start()
    }

    private class UndertowStateFunHandler(private val handler: RequestReplyHandler) : HttpHandler {
        override fun handleRequest(exchange: HttpServerExchange) {
            exchange.requestReceiver.receiveFullBytes { exchange: HttpServerExchange, requestBytes: ByteArray ->
                onRequestBody(
                    exchange,
                    requestBytes
                )
            }
        }

        private fun onRequestBody(exchange: HttpServerExchange, requestBytes: ByteArray) {
            try {
                val future = handler.handle(Slices.wrap(requestBytes))
                exchange.dispatch()
                future.whenComplete { responseBytes: Slice, ex: Throwable? ->
                    if (ex != null) {
                        onException(exchange, ex)
                    } else {
                        onSuccess(exchange, responseBytes)
                    }
                }
            } catch (t: Throwable) {
                onException(exchange, t)
            }
        }

        private fun onException(exchange: HttpServerExchange, t: Throwable) {
            t.printStackTrace(System.out)
            exchange.responseHeaders.put(Headers.STATUS, 500)
            exchange.endExchange()
        }

        private fun onSuccess(exchange: HttpServerExchange, result: Slice) {
            exchange.responseHeaders.put(Headers.CONTENT_TYPE, "application/octet-stream")
            exchange.responseSender.send(result.asReadOnlyByteBuffer())
        }
    }
}

