#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
desktop_socket="${HOME}/.docker/desktop/docker.sock"
if [[ -z "${DOCKER_HOST:-}" && -S "$desktop_socket" ]]; then
  export DOCKER_HOST="unix://${desktop_socket}"
fi
if [[ -d .run ]]; then
  for pidfile in .run/*.pid; do
    [[ -f "$pidfile" ]] || continue
    pid="$(cat "$pidfile")"
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
    rm -f "$pidfile"
  done
fi
docker compose stop
echo "Платформа остановлена"
