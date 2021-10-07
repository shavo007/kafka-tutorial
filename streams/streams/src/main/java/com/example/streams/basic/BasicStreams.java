package com.example.streams.basic;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication
public class BasicStreams {

  public static void main(String[] args) throws IOException {
    // SpringApplication.run(BasicStreams.class, args);
    Properties streamsProperties = new Properties();
    try (FileInputStream fis = new FileInputStream("src/main/resources/streams.properties")) {
          streamsProperties.load(fis);
    } catch (Exception e) {
      //TODO: handle exception
    }
    streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "basic-streams");
    StreamsBuilder builder = new StreamsBuilder();
    final String inputTopic = streamsProperties.getProperty("basic.input.topic");
    final String outputTopic = streamsProperties.getProperty("basic.output.topic");
    final String orderNumberStart = "orderNumber-";
    KStream<String, String> firstStream = builder.stream(inputTopic, Consumed.with(Serdes.String(), Serdes.String()));
    firstStream.peek((key, value) -> System.out.println("key " + key + " value " + value))
              .filter((key, value) -> value.contains(orderNumberStart))
              .mapValues(value -> value.substring(value.indexOf("-") + 1))
              .filter((key, value) -> Long.parseLong(value) > 1000)
              .peek((key, value) -> System.out.println(" key " + key + " value " + value))
              .to(outputTopic, Produced.with(Serdes.String(), Serdes.String()));
 KafkaStreams kafkaStreams = new KafkaStreams(builder.build(), streamsProperties);
 TopicLoader.runProducer();
 kafkaStreams.start();



  }

}
