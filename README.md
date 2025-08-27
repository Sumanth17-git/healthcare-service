# 📊 Datadog APM + Profiling + Dynamic Instrumentation + Logs Correlation (Java Microservices)

This project is created to **practice deep Datadog observability enablement** for Java microservices running on **Linux-based VMs** and **Kubernetes clusters** using **Datadog**.

The goal is to enable:  
✅ APM Tracing  
✅ Profiling  
✅ Dynamic Instrumentation (DI)  
✅ Logs ingestion  
✅ Logs & Traces Correlation  

---
## ⚠️ Common Gotchas

- **Service name mismatch**: `-Ddd.service`, `log service`, and `APM service` **must match**.  
- **Logs not shipped**: Ensure `logs_enabled: true` and correct file path/permissions.  
- **Layout missing MDC**: Without `trace_id` in raw logs → update **Logback layout**.  
- **Multiple apps on same host**: Each app must set its own `-Ddd.service`.  
- **Sampling**: Too high sampling may drop traces. For testing:  

---
## 🔹 Use Case 1: Host-Based Enablement (Linux VM)
### Step 1. Install & Configure Datadog Agent
My expectation is that I want to enable the APM tracing, Profiling, Dynamic Instrumentation, Logs ,Logs correlation with traces. We need to make few tweeks for this.
Install the agent on your VM and edit `/etc/datadog-agent/datadog.yaml`:
```yaml
tags:
  - env:dev
  - role:web
  - team:platform
  - datadog_monitor:yes
# Enable the environment
env: dev
remote_updates: true
remote_configuration:
  enabled: true
logs_enabled: true
logs_config:
  container_collect_all: true
process_config:
  enabled: "true"      # string "true" is accepted; or use 'enabled: true'
  process_collection:
    enabled: true
  container_collection:
    enabled: true
  run_in_core_agent:
    enabled: true
apm_config:
  enabled: true
  env: dev
  receiver_port: 8126
  receiver_socket: /var/run/datadog/apm.socket
  apm_non_local_traffic: true
  instrumentation:
    enabled: true
use_dogstatsd: true
dogstatsd_port: 8125
dogstatsd_socket: /opt/datadog/apm/inject/run/dsd.socket
listeners:
   - name: auto
   - name: docker
## Container detection ##
container_cgroup_prefix: "/docker/"
## Autoconfig Configuration ##
config_providers:
  - name: docker
    polling: true
sudo -u dd-agent install -m 0640 /etc/datadog-agent/system-probe.yaml.example /etc/datadog-agent/system-probe.yaml
Edit /etc/datadog-agent/system-probe.yaml to enable the process module:
system_probe_config:
  process_config:
    enabled: true

service_monitoring_config:
  enabled: true
  process_service_inference:
    enabled: true

network_config:   # use system_probe_config for Agent's older than 7.24.1
  ## @param enabled - boolean - optional - default: false
  ## Set to true to enable Network Performance Monitoring.
  #
  enabled: true

sudo systemctl start datadog-agent-sysprobe
sudo systemctl enable datadog-agent-sysprobe
sudo systemctl restart datadog-agent
#Setting up Cloud Security on Linux
https://docs.datadoghq.com/security/cloud_security_management/setup/agent/linux/

/etc/datadog-agent/datadog.yaml

compliance_config:
  ## @param enabled - boolean - optional - default: false
  ## Set to true to enable CIS benchmarks for Misconfigurations.
  #
  enabled: true
  host_benchmarks:
    enabled: true

# Vulnerabilities are evaluated and scanned against your containers and hosts every hour.
sbom:
  enabled: true
  # Set to true to enable Container Vulnerability Management
  container_image:
    enabled: true
  # Set to true to enable Host Vulnerability Management  
  host:
    enabled: true

sudo cp /etc/datadog-agent/security-agent.yaml.example /etc/datadog-agent/security-agent.yaml
sudo chmod 640 /etc/datadog-agent/security-agent.yaml
sudo chgrp dd-agent /etc/datadog-agent/security-agent.yaml

/etc/datadog-agent/security-agent.yaml
compliance_config:
  ## @param enabled - boolean - optional - default: false
  ## Set to true to enable CIS benchmarks for Misconfigurations.
  #
  enabled: true
  host_benchmarks:
    enabled: true


```

Usecase 1: Host Based enablement
1.Firstly we need to make sure the Datadog agent is installed and update the datadog-agent.yaml as per below
2. We will use this repository for this usecase , my expectation is that I want to enable the APM tracing, Profiling, Dynamic Instrumentation, Logs ,Logs correlation with traces. We need to make few tweeks for this.

Step 1:  Update the Logging configuration logback-spring.xml 
Make Logback print the injected IDs , The agent injects MDC keys dd.trace_id and dd.span_id. Your Logback layout must include them.
```yaml
JSON logs (nice in Datadog Logs):
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <property name="LOG_DIR" value="/var/log/healthcare-service"/>
  <property name="LOG_FILE" value="${LOG_DIR}/app.log"/>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <timestamp/>
        <logLevel/>
        <threadName/>
        <loggerName/>
        <message/>
        <mdc/>  <!-- includes dd.trace_id & dd.span_id injected by the Java agent -->
        <jsonFields>
          <field name="service" value="healthcare-service"/>
          <field name="env" value="dev"/>
        </jsonFields>
      </providers>
    </encoder>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_FILE}</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
      <fileNamePattern>${LOG_DIR}/app-%d{yyyy-MM-dd}.log</fileNamePattern>
      <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <timestamp/>
        <logLevel/>
        <threadName/>
        <loggerName/>
        <message/>
        <mdc/>
        <jsonFields>
          <field name="service" value="healthcare-service"/>
          <field name="env" value="dev"/>
        </jsonFields>
      </providers>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
  </root>
</configuration>
```
Your controller code is fine; just keep using logger.info(...). Once the layout prints the MDC, correlation works.
Step 2: Make sure the Agent ships the logs
Datadog Agent on a VM: /etc/datadog-agent/datadog.yaml

logs_enabled: true
apm_config:
  enabled: true

Create a log integration file (point it to your app’s log OR use console/stdout if you run it under a supervisor that captures stdout):

/etc/datadog-agent/conf.d/java.d/conf.yaml   (  if you dont see java.d directory, create a one)

```yaml
logs:
  - type: file
    path: /var/log/your-app/app.log        # adjust if using a file , this is the log location you are printing the springboot logs
    service: java-service                   # must match -Ddd.service
    source: java
    env: dev
```
If you emit to stdout, point the Agent to that stream (systemd/journald) or run the app under a process manager that writes to a file the Agent can read.
Restart the Agent:

```yaml
sudo systemctl restart datadog-agent
```
Give the Agent read access
```yaml
sudo usermod -a -G $(id -gn) dd-agent
sudo chmod -R g+rX /home/terraform17/healthcare-service/logs
sudo chown -R dd-agent:dd-agent /home/terraform17/healthcare-service/logs
sudo mkdir -p /var/log/healthcare-service
sudo chown dd-agent:dd-agent /var/log/healthcare-service
sudo chmod 755 /var/log/healthcare-service
sudo chown -R dd-agent:dd-agent /var/log/healthcare-service
sudo chmod 640 /var/log/healthcare-service/app.log
```
Switch to JSON logs so dd.trace_id & dd.span_id are real fields
```yaml
Add encoder dependency:
<!-- pom.xml -->
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>7.4</version>
</dependency>

Restart & check status.
sudo systemctl restart datadog-agent
```
```yaml
mvn clean install
java -javaagent:/home/terraform17/dd-java-agent.jar \
  -Ddd.service=healthcare-service \
  -Ddd.env=dev \
  -Ddd.version=0.0.1 \
  -Ddd.tags=app-name:healthcare-service,team:devops \
  -Ddd.logs.injection=true \
  -Ddd.trace.sample.rate=1 \
  -Ddd.trace.health.metrics.enabled=true \
  -Ddd.profiling.enabled=true \
  -Ddd.dynamic.instrumentation.enabled=true \
  -Ddd.jmxfetch.enabled=true \
  -XX:FlightRecorderOptions=stackdepth=1024 \
  -Ddd.dynamic.instrumentation.enabled=true \
  -Ddd.code.origin.for.spans.enabled=true \
  -Ddd.jmxfetch.enabled=true  \
  -Ddd.profiling.heap.enabled=true \
  -Ddd.git.commit.sha=${COMMIT_SHA} \
  -Ddd.git.repository.url=https://github.com/Sumanth17-git/healthcare-service.git \
  -Ddd.profiling.ddprof.liveheap.enabled=true \
  -jar healthcare-service-0.0.1-SNAPSHOT.jar

```
Flip the Datadog UI toggle you showed
In the APM > Services > java-service > Configuration drawer (your screenshot), set Connect Logs and Traces → Enabled.
Monitoring → Leave Not Configured (unless you use queues)

•	If this service produces/consumes Kafka/SQS/Pub/Sub, set Enabled here and start your app with a tracer setting that turns on DSM (e.g., environment variable DD_DATA_STREAMS_ENABLED=true; confirm in startup logs that it’s recognized).

•	Otherwise keep it off.
<img width="661" height="457" alt="image" src="https://github.com/user-attachments/assets/594eb109-af76-4100-ae31-3544ae7a0d9d" />
This tells Datadog’s pipelines to use the injected trace_id/span_id to auto-link.
your logs will look like normal application logs but with the Datadog trace/span IDs appended.

From Trace:
 <img width="677" height="310" alt="image" src="https://github.com/user-attachments/assets/fe946720-e35d-47d7-afa4-e5ea694c53f5" />
```yaml
In Logs, search:
service:java-service @trace_id:*
From Logs → Trace
1.	Go to Logs.
2.	Search: service:healthcare-service @trace_id:*
3.	Open any log line → you should see a Trace link/button. Click it → it opens the exact trace/span.
 
From APM → Logs
1.	Go to APM → Services → healthcare-service.
2.	Open any recent Trace.
3.	In the trace view, open the Logs tab → you should see your log lines for that request.
   <img width="792" height="383" alt="image" src="https://github.com/user-attachments/assets/0c25e8a6-c68f-464f-9318-ae07aff5557e" />

5.	Open Traces Click Log section  Choose Show Logs on Traces ( This confirms the Logs are correlated with Trace)
 <img width="768" height="362" alt="image" src="https://github.com/user-attachments/assets/2a95bdcc-a474-4bf9-a465-29644c4a5928" />

6.	Choose Show Logs on Infrastructure  ( This confirm the logs are ingested into NewRelic)
 <img width="700" height="326" alt="image" src="https://github.com/user-attachments/assets/7f9a7267-7b1c-4a02-b52a-cd3ac414ccf6" />
```
If you don’t see the link, ensure APM → Services → healthcare-service → Configuration → “Connect Logs and Traces” = Enabled.

