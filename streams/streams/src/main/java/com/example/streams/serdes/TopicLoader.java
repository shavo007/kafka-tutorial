package com.example.streams.serdes;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.serialization.StringSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

import io.confluent.developer.avro.ProductOrder;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.example.streams.basic.StreamsUtils;

public class TopicLoader {

    public static void main(String[] args) throws IOException {
        runProducer();
    }

     static void runProducer() throws IOException {
        Properties properties = StreamsUtils.loadProperties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        properties.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        try(Admin adminClient = Admin.create(properties);
            Producer<String, ProductOrder> producer = new KafkaProducer<String, ProductOrder>(properties)) {
            final String inputTopic = properties.getProperty("sr.input.topic");
            final String outputTopic = properties.getProperty("sr.output.topic");
            var topics = List.of(StreamsUtils.createTopic(inputTopic), StreamsUtils.createTopic(outputTopic));
            adminClient.createTopics(topics);

            Callback callback = (metadata, exception) -> {
                if(exception != null) {
                    System.out.printf("Producing records encountered error %s %n", exception);
                } else {
                    System.out.printf("Record produced - offset - %d timestamp - %d %n", metadata.offset(), metadata.timestamp());
                }

            };
            final List<ProductOrder> rawRecords = new ArrayList<>();
            for (long i = 0; i < 10; i++) {
                final String orderId = "id" + Long.toString(i);
                final ProductOrder productOrder = new ProductOrder(orderId, "product" + Long.toString(i), Uuid.randomUuid().toString(), Instant.now().toEpochMilli());
                rawRecords.add(productOrder);
            }

            var producerRecords = rawRecords.stream().map(r -> new ProducerRecord<>(inputTopic,"order-key", r)).collect(Collectors.toList());
            producerRecords.forEach((pr -> producer.send(pr, callback)));


        }
    }
}
