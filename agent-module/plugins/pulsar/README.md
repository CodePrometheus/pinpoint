## Apache Pulsar
* Since: Pinpoint 2.2.0
* See: https://pulsar.apache.org/
* Range: org.apache.pulsar/pulsar-client [4.0,]

### Pulsar Configuration
To enable Pulsar Producer, set the following option in *pinpoint.config*:
```
profiler.Pulsar.producer.enable=true
```

To enable Pulsar Consumer, set the following option in *pinpoint.config*:
```
profiler.Pulsar.consumer.enable=true
```
### Caution
#### Caution for Consumer