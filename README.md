### healthcare-service
```bash
git clone  https://github.com/Sumanth17-git/healthcare-service.git
```
Make sure the Agent ships the logs
Datadog Agent on a VM:
```bash
/etc/datadog-agent/datadog.yaml
```
### Log Monitoring
Collecting logs is disabled by default in the Datadog Agent. Add the following in datadog.yaml:
```bash
logs_enabled: true
logs_config:
  container_collect_all: true
```
Write logs under /var/log and let the Agent own them
### 1.	Create a service log dir the Agent can read:
```bash
sudo mkdir -p /var/log/healthcare-service
sudo chown dd-agent:dd-agent /var/log/healthcare-service
sudo chmod 755 /var/log/healthcare-service
sudo chown -R dd-agent:dd-agent /var/log/healthcare-service
sudo chmod 640 /var/log/healthcare-service/app.log
sudo chown -R root:dd-agent /var/log/healthcare-service
sudo chmod -R 755 /var/log/healthcare-service
```
### 2. Create the configuration directory for Datadog logs
```bash
sudo mkdir -p /etc/datadog-agent/conf.d/java.d
sudo chown -R dd-agent:dd-agent /etc/datadog-agent/conf.d/java.d
sudo chmod 755 /etc/datadog-agent/conf.d/java.d
cd /etc/datadog-agent/conf.d/java.d
vi config.yaml
```
```bash
logs:
  - type: file
    path: /var/log/healthcare-service/app.log
    service: healthcare-service
    source: java
```
```bash
systemctl restart datadog-agent
systemctl status datadog-agent
datadog-agent status

```

## Validate it
```yaml
POST http://3.91.194.213:8081/api/v1/appointments
{
        "patientId": "P-1002",
        "doctor": "Dr. Sharma"
}
http://3.91.194.213:8081/api/v1/patients
http://3.91.194.213:8081/api/v1/appointments
```

### Datadog APM Tracing 
```bash
java -javaagent:/home/ubuntu/dd-java-agent.jar \
  -Ddd.profiling.enabled=true \
  -XX:FlightRecorderOptions=stackdepth=256 \
  -Ddd.data-streams.enabled=true \
  -Ddd.trace.remove.integration-service-names.enabled=true \
  -Ddd.logs.injection=true \
  -Ddd.trace.sample.rate=1 \
  -Ddd.service=healthcare-service \
  -Ddd.env=prod \
  -Ddd.version=0.0.1 \
  -Ddd.profiling.directallocation.enabled=true \
  -Ddd.profiling.ddprof.liveheap.enabled=true \
  -Ddd.profiling.heap.enabled=true \
  -jar healthcare-service-0.0.1-SNAPSHOT.jar

```
