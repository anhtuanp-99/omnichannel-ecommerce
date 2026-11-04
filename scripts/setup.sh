#!/usr/bin/env bash
set -euo pipefail

# 1. Chống lỗi tự đổi đường dẫn /opt/kafka thành C:/Program Files/Git/... trên Git Bash Windows
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# 2. Nạp cấu hình từ .env nếu tồn tại (để lấy port hiển thị log)
if [ -f .env ]; then
  # shellcheck disable=SC1091
  source .env
fi

echo "🚀 [1/3] Build Maven (common + common-web)..."
mvn -q -DskipTests clean install

echo "🐳 [2/3] Khởi động infrastructure..."
docker compose up -d --wait --wait-timeout 180

echo "📨 [3/3] Tạo Kafka topics..."
docker compose exec omni-kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 --create --if-not-exists \
    --topic order.events --partitions 3 --replication-factor 1
docker compose exec omni-kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 --create --if-not-exists \
    --topic payment.events --partitions 3 --replication-factor 1
docker compose exec omni-kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 --create --if-not-exists \
    --topic catalog.events --partitions 1 --replication-factor 1

echo ""
echo "✅ Setup hoàn tất!"
echo "   Kafka UI:   http://localhost:${KAFKA_UI_PORT:-8090}"
echo "   Mailpit:    http://localhost:${MAILPIT_UI_PORT:-8025}"
echo "   Postgres:   localhost:${DB_PORT:-5432}"
echo "   MongoDB:    localhost:${MONGO_PORT:-27017}"
echo "   Redis:      localhost:${REDIS_PORT:-6379}"
echo "   Kafka:      localhost:${KAFKA_PORT:-29092} (từ host) / kafka:9092 (từ container)"
echo ""
echo "   Bật Elasticsearch: docker compose --profile search up -d"