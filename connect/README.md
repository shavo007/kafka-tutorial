# Kafa connect

## Create topics

```bash
docker exec broker \
kafka-topics --bootstrap-server broker:9092 \
             --create \
             --topic pageviews \

docker exec broker \
kafka-topics --bootstrap-server broker:9092 \
             --create \
             --topic users

#create connector
curl -X POST http://localhost:8083/connectors \
  -H 'Content-Type:application/json' \
  -H 'Accept:application/json' \
  -d @contrib.sink.avro.neo4j.json

#pageviews
curl -X POST http://localhost:8083/connectors \
  -H 'Content-Type:application/json' \
  -H 'Accept:application/json' \
  -d @contrib.datagen-pageview.avro.json

#users
curl -X POST http://localhost:8083/connectors \
  -H 'Content-Type:application/json' \
  -H 'Accept:application/json' \
  -d @datagen_users_oss.json



curl -s  http://localhost:8083/connectors | jq '.'
curl -s http://localhost:8083/connector-plugins | jq '.'

 curl -s "http://localhost:8083/connectors?expand=info&expand=status" | \
  jq '. | to_entries[] | [ .value.info.type, .key, .value.status.connector.state,.value.status.tasks[].state, .value.info.config."connector.class"] |join(":|:")' | \
  column -s : -t| sed 's/\"//g'| sort


kcat -b localhost:9092 -t pageviews -s value=avro -r http://localhost:8081
kcat -b localhost:9092 -t users -s value=avro -r http://localhost:8081

# generate users
java -jar neo4j-streams-sink-tester-1.0.jar -f AVRO
kcat -b localhost:9092 -t my-topic -s value=avro -r http://localhost:8081

# Access neo4j at `http://localhost:7474`
# username: neo4j pwd:connect
# run cmd `MATCH p=()-->() RETURN p LIMIT 25`

 curl -s -XDELETE "http://localhost:8083/connectors/Neo4jSinkConnector"
 curl -s -XDELETE "http://localhost:8083/connectors/datagen-users"
 curl -s -XDELETE "http://localhost:8083/connectors/datagen-pageviews"

```

![Image of Graph](./graph.svg)

## TODO

example with sink connector and neo4j
example with cdc and postgres for src connector
