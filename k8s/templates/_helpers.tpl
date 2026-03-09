{{/*
공통 레이블
*/}}
{{- define "petradar.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
백엔드 이미지 주소
사용법: {{ include "petradar.image" (list . "service-name") }}
*/}}
{{- define "petradar.image" -}}
{{- $root := index . 0 -}}
{{- $service := index . 1 -}}
{{- printf "%s/%s/%s:%s" $root.Values.global.imageRegistry $root.Values.global.imageRepository $service $root.Values.global.imageTag -}}
{{- end }}
