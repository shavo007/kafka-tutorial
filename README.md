# kafka tutorial

```bash
├── asyncapi #async api example with microcks
├── avro #avro schema consumer/producer example with schema registry
├── connect #connect with multiple examples
├── kotlin #kotlin producer/consumer example
└── ksqldb #database for stream processing apps
└── streams #streams examples (basic/serdes/ktable)
```

![The San Juan Mountains are beautiful!](/assets/images/ksqldb-kafka-streams-core-kafka-stack.png)

## Steps

`docker-compose up -d`

```bash
#~/.confluent/java.config
# Kafka
bootstrap.servers=localhost:9092

# Confluent Schema Registry
schema.registry.url=http://localhost:8081
```

### Create a topic

```bash
docker exec broker \
kafka-topics --bootstrap-server broker:9092 \
             --create \
             --topic quickstart

```

### Producer example

```bash
cd kotlin
./gradlew runApp -PmainClass="io.confluent.examples.clients.cloud.ProducerExample"      -PconfigPath="$HOME/.confluent/java.config"      -Ptopic="test1"
```

```bash
kcat -b localhost:9092 -t test1 -C \
  -f '\nKey (%K bytes): %k
  Value (%S bytes): %s
  Timestamp: %T
  Partition: %p
  Offset: %o
  Headers: %h\n'
```

### Consumer example

```bash
./gradlew runApp -PmainClass="io.confluent.examples.clients.cloud.ConsumerExample"\
     -PconfigPath="$HOME/.confluent/java.config"\
     -Ptopic="test1"
```

### Streams example

```bash
./gradlew runApp -PmainClass="io.confluent.examples.clients.cloud.StreamsExample" \
     -PconfigPath="$HOME/.confluent/java.config" \
     -Ptopic="test1"
```

## kafka without zookeeper and schema registry

https://docs.confluent.io/platform/current/tutorials/build-your-own-demos.html#kraft
https://docs.confluent.io/platform/current/schema-registry/schema_registry_onprem_tutorial.html

```bash
git clone https://github.com/confluentinc/cp-all-in-one.git
cd cp-all-in-one/cp-all-in-one-kraft
git checkout 6.2.0-post

#local file
docker-compose -f docker-compose-kraft.yaml up  -d

```

## Avro example and schema registry

```bash
#producer
cd avro
mvn exec:java -Dexec.mainClass=io.confluent.examples.clients.basicavro.ProducerExample \
  -Dexec.args="$HOME/.confluent/java.config"

#view latest schema
curl --silent -X GET http://localhost:8081/subjects/transactions-value/versions/latest | jq .
curl --silent -X GET http://localhost:8081/schemas/ids/1 | jq .

#consumer
mvn exec:java -Dexec.mainClass=io.confluent.examples.clients.basicavro.ConsumerExample \
  -Dexec.args="$HOME/.confluent/java.config"

```

### Schema evolution and compatability

```bash
#checking backwards compatiability with the latest registered schema for payments

mvn io.confluent:kafka-schema-registry-maven-plugin:test-compatibility
#you will get an error due to new field region
[ERROR] Schema kafka-tutorial/avro/src/main/resources/avro/io/confluent/examples/clients/basicavro/Payment2a.avsc is not compatible with subject(transactions-value) with error [Incompatibility{type:READER_FIELD_MISSING_DEFAULT_VALUE, location:/fields/2, message:region, reader:{"type":"record","name":"Payment","namespace":"io.confluent.examples.clients.basicavro","fields":[{"name":"id","type":"string"},{"name":"amount","type":"double"},{"name":"region","type":"string"}]}, writer:{"type":"record","name":"Payment","namespace":"io.confluent.examples.clients.basicavro","fields":[{"name":"id","type":"string"},{"name":"amount","type":"double"}]}}]
```

Now register the new version of schema with default value for field `region`

```bash
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  --data '{"schema": "{\"type\":\"record\",\"name\":\"Payment\",\"namespace\":\"io.confluent.examples.clients.basicavro\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"amount\",\"type\":\"double\"},{\"name\":\"region\",\"type\":\"string\",\"default\":\"\"}]}"}' \
  http://localhost:8081/subjects/transactions-value/versions

  #view the latest subject
  curl --silent -X GET http://localhost:8081/subjects/transactions-value/versions/latest | jq .

  #get global compatiability type
  curl --silent -X GET http://localhost:8081/config | jq .

  curl -X PUT -H "Content-Type: application/vnd.schemaregistry.v1+json" \
       --data '{"compatibility": "BACKWARD_TRANSITIVE"}' \
       http://localhost:8081/config/transactions-value

curl --silent -X GET http://localhost:8081/config/transactions-value | jq .
```

## Resources

- <https://docs.confluent.io/platform/current/tutorials/examples/clients/docs/kotlin.html>
- <https://www.confluent.io/learn/kafka-tutorial/>
- <https://www.asyncapi.com/blog/openapi-vs-asyncapi-burning-questions>

## TODO

- linting async API via spectral
- microcks and async API https://microcks.io/blog/async-features-with-docker-compose/
- https://microcks.io/documentation/guides/avro-messaging/
