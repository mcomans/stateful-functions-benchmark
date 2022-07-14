package api.logging

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class EgressConsumer {
    private val logger: Logger = LoggerFactory.getLogger(EgressConsumer::class.java)

   @KafkaListener(topics = ["egress"])
   fun listen(record: ConsumerRecord<String, String>) {
       MDC.put("requestId", record.key())
       MDC.put("status", "EGRESS_DONE")
       logger.info(record.value())
       MDC.remove("requestId")
       MDC.remove("status")
   }
}