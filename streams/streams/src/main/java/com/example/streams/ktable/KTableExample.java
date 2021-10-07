package com.example.streams.ktable;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.common.utils.Bytes;

public class KTableExample {

  public static void main(String[] args) throws IOException {
    Properties streamsProperties = new Properties();
    try (FileInputStream fis = new FileInputStream("src/main/resources/streams.properties")) {
      streamsProperties.load(fis);
    } catch (Exception e) {
      // TODO: handle exception
    }
		streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "ktable-application");

    StreamsBuilder builder = new StreamsBuilder();
    final String inputTopic = streamsProperties.getProperty("ktable.input.topic");
    final String outputTopic = streamsProperties.getProperty("ktable.output.topic");
    final String orderNumberStart = "orderNumber-";
		// It will use caching and will only emit the latest records for each key after a commit (which is 30 seconds, or when the cache is full at 10 MB).
		// A KTable is an abstraction of a changelog stream, where each data record
		// represents an update
		//https://docs.confluent.io/platform/current/streams/concepts.html
    KTable<String, String> firstKTable = builder.table(inputTopic,
        Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("ktable-store").withKeySerde(Serdes.String())
            .withValueSerde(Serdes.String()));

    firstKTable.filter((key, value) -> value.contains(orderNumberStart))
        .mapValues(value -> value.substring(value.indexOf("-") + 1))
        .filter((key, value) -> Long.parseLong(value) > 1000).toStream()
        .peek((key, value) -> System.out.println("Outgoing record - key " + key + " value " + value))
        .to(outputTopic, Produced.with(Serdes.String(), Serdes.String()));

    KafkaStreams kafkaStreams = new KafkaStreams(builder.build(), streamsProperties);
    TopicLoader.runProducer();
    kafkaStreams.start();

  }
}
