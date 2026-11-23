#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p .run

desktop_socket="${HOME}/.docker/desktop/docker.sock"
if [[ -z "${DOCKER_HOST:-}" && -S "$desktop_socket" ]]; then
  export DOCKER_HOST="unix://${desktop_socket}"
fi

docker compose up -d
echo "Жду Kafka на порту 9092"
for _ in $(seq 1 60); do
  if (echo >/dev/tcp/127.0.0.1/9092) >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
if ! (echo >/dev/tcp/127.0.0.1/9092) >/dev/null 2>&1; then
  echo "Kafka не открыла порт 9092. Смотрите: docker compose logs"
  exit 1
fi

mvn -q -DskipTests package

start() {
  local name="$1"
  local jar="$2"
  if [[ -f ".run/${name}.pid" ]] && kill -0 "$(cat ".run/${name}.pid")" 2>/dev/null; then
    echo "$name уже запущен"
    return
  fi
  java -jar "$jar" > ".run/${name}.log" 2>&1 &
  echo $! > ".run/${name}.pid"
  echo "запущен $name"
}

wait_port() {
  local port="$1"
  for _ in $(seq 1 90); do
    if (echo >/dev/tcp/127.0.0.1/"$port") >/dev/null 2>&1; then
      return
    fi
    sleep 1
  done
  echo "Порт $port не открылся. Смотрите логи в .run"
  exit 1
}

start catalog catalog/target/platform-catalog-0.0.1-SNAPSHOT-exec.jar
start pricing pricing/target/platform-pricing-0.0.1-SNAPSHOT-exec.jar
start payment payment/target/platform-payment-0.0.1-SNAPSHOT-exec.jar
start notification notification/target/platform-notification-0.0.1-SNAPSHOT-exec.jar
start analytics analytics/target/platform-analytics-0.0.1-SNAPSHOT-exec.jar
start assistant assistant/target/platform-assistant-0.0.1-SNAPSHOT-exec.jar
start identity identity/target/platform-identity-0.0.1-SNAPSHOT-exec.jar
wait_port 9082
wait_port 9083
wait_port 9085
wait_port 9086
wait_port 9087
wait_port 9089
wait_port 8081
start booking booking/target/platform-booking-0.0.1-SNAPSHOT-exec.jar
wait_port 9084
start gateway gateway/target/platform-gateway-0.0.1-SNAPSHOT.jar
wait_port 8088
echo "GraphiQL: http://localhost:8088/graphiql"
echo "guest@hotel.local / guest, worker@hotel.local / worker, admin@hotel.local / admin"
