{{/*
Expand the name of the chart.
*/}}
{{- define "vibecheck.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "vibecheck.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels applied to all resources
*/}}
{{- define "vibecheck.labels" -}}
helm.sh/chart: {{ include "vibecheck.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: vibecheck
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "vibecheck.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Selector labels helper function parameterized by service name
Usage: {{ include "vibecheck.selectorLabels" (dict "context" . "serviceName" "auth-service") }}
*/}}
{{- define "vibecheck.selectorLabels" -}}
app.kubernetes.io/name: {{ .serviceName }}
app.kubernetes.io/instance: {{ .context.Release.Name }}
{{- end }}

{{/*
initContainer: wait for MySQL to be connectable on port 3306
Usage: {{- include "vibecheck.waitForMySQL" . | nindent 8 }}
*/}}
{{- define "vibecheck.waitForMySQL" -}}
- name: wait-for-mysql
  image: busybox:1.36
  command:
    - /bin/sh
    - -c
    - |
      echo "Waiting for MySQL at mysql:3306..."
      until nc -z mysql 3306; do
        echo "MySQL not ready, retrying in 3s..."
        sleep 3
      done
      echo "MySQL is up!"
{{- end }}

{{/*
initContainer: wait for Kafka to be connectable on port 9092
Usage: {{- include "vibecheck.waitForKafka" . | nindent 8 }}
*/}}
{{- define "vibecheck.waitForKafka" -}}
- name: wait-for-kafka
  image: busybox:1.36
  command:
    - /bin/sh
    - -c
    - |
      echo "Waiting for Kafka at kafka:9092..."
      until nc -z kafka 9092; do
        echo "Kafka not ready, retrying in 3s..."
        sleep 3
      done
      echo "Kafka is up!"
{{- end }}
