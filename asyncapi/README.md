# Microcks with async

```bash
docker rmi quay.io/microcks/microcks:latest quay.io/microcks/microcks-async-minion:latest quay.io/microcks/microcks-postman-runtime:latest
git clone https://github.com/microcks/microcks
cd microcks/install/docker-compose
docker-compose -f docker-compose.yml -f docker-compose-async-addon.yml up -d
docker ps
#verify
kcat -b localhost:9092 -L
kcat -b localhost:9092 -t PaymentAvroAPI-0.1.2-transactions -s value=avro -o end
```

## Resources

- <https://microcks.io/blog/async-features-with-docker-compose/>
