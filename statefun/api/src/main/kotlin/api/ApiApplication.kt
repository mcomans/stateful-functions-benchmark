package api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*


@SpringBootApplication
class ApiApplication(private val kafkaProperties: KafkaProperties) {

    @Bean
    fun producerConfigs(): Map<String, Any> {
        return HashMap<String, Any>(kafkaProperties.buildProducerProperties())
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        return DefaultKafkaProducerFactory(producerConfigs())
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory())
    }

//    @Bean
//    fun consumerConfigs(): Map<String, Any> {
//        return HashMap<String, Any>(kafkaProperties.buildConsumerProperties())
//    }

//    @Bean
//    fun consumerFactory(): ConsumerFactory<String, String> {
//        return DefaultKafkaConsumerFactory(consumerConfigs())
//    }

//    @Bean
//    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
//        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
//        factory.consumerFactory = consumerFactory()
//        return factory
//    }
}

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}

