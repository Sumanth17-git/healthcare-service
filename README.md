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
If you don’t see the link, ensure APM → Services → healthcare-service → Configuration → “Connect Logs and Traces” = Enabled.

============================================KUBERNETES =======================================
# ☸️ Kubernetes: APM Tracing, Profiling, Dynamic Instrumentation, and Log Correlation

This section explains how to enable **Datadog APM, Profiling, Dynamic Instrumentation, and Log Correlation** for **Java microservices running inside Kubernetes/Docker containers**.

---

## 🔹 Step 1: Logging — Write to STDOUT

- In containers, Datadog tails the **container logs**.  
- **Do not write to files** inside containers. Instead, configure Logback to **write to console (stdout)**.  
- Keep MDC fields (`dd.trace_id`, `dd.span_id`) to enable **trace ↔ log correlation**.

Update your `src/main/resources/logback-spring.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
      <providers>
        <timestamp/>
        <logLevel/>
        <threadName/>
        <loggerName/>
        <message/>
        <mdc/> <!-- includes dd.trace_id & dd.span_id injected by the Java agent -->
        <jsonFields>
          <field name="service" value="healthcare-service"/>
          <field name="env" value="dev"/>
        </jsonFields>
      </providers>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
  </root>
</configuration>
```
```bash

👉 If you don’t want JSON, you can log as plain text but include trace IDs:

perl
Copy code
%msg | dd.trace_id=%X{dd.trace_id} dd.span_id=%X{dd.span_id}
🔹 Step 2: Rebuild Application
Run Maven build to apply changes:

mvn clean install
```
This generates the new executable .jar in the target/ folder.

🔹 Step 3: Containerize Application
Create a Dockerfile
```yaml
FROM openjdk:17-jdk-slim
WORKDIR /usr/app
ADD target/*.jar app.jar
# --- Datadog defaults (can be overridden at runtime) ---
ENV DD_SERVICE="healthcare-service" \
    DD_ENV="dev" \
    DD_VERSION="0.0.1" \
    DD_LOGS_INJECTION="true" \
    DD_DYNAMIC_INSTRUMENTATION_ENABLED="true"
# Optional: source-code integration (set from CI)
ARG COMMIT_SHA=""
ENV DD_GIT_COMMIT_SHA="${COMMIT_SHA}"
ENV DD_GIT_REPOSITORY_URL="https://github.com/Sumanth17-git/healthcare-service.git"

EXPOSE 8081
ENTRYPOINT exec java  -jar app.jar
```
```bash
You can also skip baking the agent and let Datadog auto-inject it with the Admission Controller
•	ENV DD_SERVICE/DD_ENV/DD_VERSION are now defaults; Kubernetes can still override them via env: if you want different values per environment.
•	Keep logs to stdout (ConsoleAppender / JSON) so the Datadog Agent DaemonSet can collect them.
•	With the Admission Controller enabled, the Java agent and JAVA_TOOL_OPTIONS are auto-injected—no changes needed here
```
```yaml
Option 1:
COMMIT_SHA=$(git rev-parse HEAD)
docker build --build-arg COMMIT_SHA=$COMMIT_SHA -t <registry>/healthcare-service:0.0.1 .
Option 2:
docker build sumanth17121988/healthcare-service:1 .
Validation : docker run -d -p 8081:8081 sumanth17121988/healthcare-service:1
```

# ☸️ Step 5: Kubernetes Deployment (APM, Profiling, DI, Log Correlation)

This step covers deploying the **healthcare-service** into Kubernetes with **Datadog auto-injection** enabled for:  
✅ APM Tracing  
✅ Profiling  
✅ Dynamic Instrumentation  
✅ Log Correlation  

The **admission controller** mounts the **Datadog Java agent** and injects `JAVA_TOOL_OPTIONS` automatically — no need to modify the container image.

---

## 🔹 Deployment YAML

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: healthcare
  labels:
    app: healthcare
    tags.datadoghq.com/env: dev
    tags.datadoghq.com/service: healthcare-service
    tags.datadoghq.com/version: "0.0.1"
spec:
  replicas: 1
  selector:
    matchLabels:
      app: healthcare
      tags.datadoghq.com/service: healthcare-service
      tags.datadoghq.com/env: dev
      tags.datadoghq.com/version: "0.0.1"
  template:
    metadata:
      labels:
        app: healthcare
        tags.datadoghq.com/env: dev
        tags.datadoghq.com/service: healthcare-service
        tags.datadoghq.com/version: "0.0.1"
        admission.datadoghq.com/enabled: "true"
      annotations:
        # Auto-inject the Datadog Java tracer
        admission.datadoghq.com/enabled: "true"
        admission.datadoghq.com/java-lib.version: "v1.50.0"

        # Configure injector to point tracer at the node's Agent
        admission.datadoghq.com/config.mode: "hostip"

        # Collect stdout/stderr as logs for THIS container
        ad.datadoghq.com/healthcare-service.logs: >-
          [{"source":"java","service":"healthcare-service","auto_multi_line_detection": true}]
    spec:
      containers:
        - name: healthcare-service
          image: sumanth17121988/healthcare-service:2
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8081
          env:
            # Unified service tags
            - name: DD_ENV
              valueFrom:
                fieldRef:
                  fieldPath: metadata.labels['tags.datadoghq.com/env']
            - name: DD_SERVICE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.labels['tags.datadoghq.com/service']
            - name: DD_VERSION
              valueFrom:
                  fieldRef:
                    fieldPath: metadata.labels['tags.datadoghq.com/version']

            # Tracing, profiling, DI, JMX, AppSec
            - name: DD_TRACE_SAMPLE_RATE
              value: "1"
            - name: DD_TRACE_HEALTH_METRICS_ENABLED
              value: "true"
            - name: DD_LOGS_INJECTION
              value: "true"
            - name: DD_PROFILING_ENABLED
              value: "true"
            - name: DD_DYNAMIC_INSTRUMENTATION_ENABLED
              value: "true"
            - name: DD_JMXFETCH_ENABLED
              value: "true"
            - name: DD_APPSEC_ENABLED
              value: "true"
            - name: DD_IAST_ENABLED
              value: "true"
            - name: DD_APPSEC_SCA_ENABLED
              value: "true"

            # Optional: global tags (maps to dd.tags)
            - name: DD_TAGS
              value: "app-name:healthcare-service,team:devops"

            # Tracer -> node Agent (DaemonSet) on 8126
            - name: DD_AGENT_HOST
              valueFrom:
                fieldRef:
                  fieldPath: status.hostIP
            - name: DD_TRACE_AGENT_URL
              value: "http://$(DD_AGENT_HOST):8126"
---
apiVersion: v1
kind: Service
metadata:
  name: healthcare-lb
  labels:
    app: healthcare
spec:
  type: LoadBalancer
  selector:
    app: healthcare
    tags.datadoghq.com/service: healthcare-service
  ports:
    - name: http
      port: 8081        # external LB port
      targetPort: 8081  # container port
```
# 🔄 How It Works on Kubernetes (Recap)

## 🧩 Admission Controller
- Automatically **injects the Datadog Java tracer** into your pod.  
- Adds `JAVA_TOOL_OPTIONS` automatically — no Dockerfile changes required.  
---
## 📝 Logs
- Application logs are written to **stdout**.  
- The **Datadog Agent DaemonSet** tails container logs and ships them to Datadog.  
- Logs include `dd.trace_id` and `dd.span_id`, enabling **log ↔ trace correlation**.  
---
## 📈 APM Traces
- Traces are exported from the injected Java agent to the **node’s Datadog Agent** (`port 8126`).  
- Configured with:  
  ```yaml
  admission.datadoghq.com/config.mode: "hostip"

# 🔄 How the End-to-End Flow Works (Kubernetes + Datadog)

- 🟢 **APM Tracing**  
  - Your app runs with the **Datadog Java agent (auto-injected)**.  
  - The agent automatically instruments **Spring Boot** and emits **spans**.  

- 🌐 **Tracer → Agent Communication**  
  - `DD_AGENT_HOST` and `DD_TRACE_AGENT_URL` ensure the tracer sends spans to the **Datadog Agent DaemonSet** on the **same node (port 8126)**.  

- 📝 **Logs**  
  - Application logs are written to **stdout**.  
  - The Datadog Agent (with `logs.containerCollectAll: true`) tails all container logs.  
  - Pod annotations ensure logs from this container are parsed/tagged correctly.  

- 🔗 **Logs ↔ Traces Correlation**  
  - Since logs contain `dd.trace_id` and `dd.span_id`, Datadog **automatically links logs to their corresponding traces/spans**.  

- 📊 **Datadog UI Check**  
  - Go to **APM → Services → healthcare-service → Configuration**.  
  - Ensure **“Connect Logs and Traces”** is enabled (only needs to be done once per environment).  

---

✅ With this setup, every log line and trace from your **healthcare-service** in Kubernetes is:  
- Collected automatically  
- Tagged with service/env/version metadata  
- Correlated in the Datadog UI for full **end-to-end observability**

# ⚙️ Step 6: Datadog Operator / Helm Chart Configuration

When deploying with the **Datadog Operator** or **Helm chart**, ensure the following configuration is enabled:

```yaml
datadog:
  site: us5.datadoghq.com
  apiKey: "<YOUR_API_KEY>"
  logs:
    enabled: true
    containerCollectAll: true   # ship all container logs
  apm:
    enabled: true               # APM/trace agent on 8126
  processAgent:
    enabled: true
  kubeStateMetricsEnabled: true
clusterAgent:
  enabled: true
admissionController:
  enabled: true                 # needed for auto-injection option
```
# 🔗 How Correlation Works in Kubernetes

## 1️⃣ Your Application
- Outputs **logs to stdout** with `dd.trace_id` & `dd.span_id`  
  (injected via `DD_LOGS_INJECTION=true`).  
- Sends **traces** to the node’s Datadog Agent:  
http://$status.hostIP:8126

---

## 2️⃣ Datadog Agent (DaemonSet)
- **Logs**:  
- Tails container `stdout` / `stderr`.  
- Autodiscovery annotations map logs to the service `healthcare-service`.  
- **APM**:  
- Receives spans on **port 8126**.  
- Because log events include `dd.trace_id` and `dd.span_id`, Datadog **automatically links logs to the exact trace/span**.  

---

## 3️⃣ In the Datadog UI
- **Logs Explorer** → each log entry contains a **Trace link**.  
- **APM Trace view** → the **Logs tab** shows correlated logs for that request.  
- One-time setup per environment:  
<img width="940" height="571" alt="image" src="https://github.com/user-attachments/assets/cd2e6e8f-d82b-46ae-aa11-a4824f680294" />
 <img width="940" height="632" alt="image" src="https://github.com/user-attachments/assets/3d24d3d4-3ba6-4c54-8253-13413d375005" />

## Testing/Validation
```bash
curl -s localhost:8080/api/v1/patients | jq .
curl -s localhost:8080/api/v1/patients/P-1001 | jq .
curl -s localhost:8080/api/v1/appointments | jq .
curl -s -X POST localhost:8080/api/v1/appointments -H "Content-Type: application/json" -d '{"patientId":"P-1002","doctor":"Dr. Mehta"}' | jq .
curl -s localhost:8080/api/v1/simulate-error | jq .
```
<img width="953" height="466" alt="image" src="https://github.com/user-attachments/assets/98ada45a-ecef-49b1-9d09-0380df3f5eb4" />
 





