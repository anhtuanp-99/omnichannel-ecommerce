#!/usr/bin/env bash
set -euo pipefail

# 1. Chống lỗi tự đổi đường dẫn trên Git Bash Windows
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# 2. Nạp file .env nếu có để lấy đúng cổng/user
if [ -f .env ]; then
  # shellcheck disable=SC1091
  source .env
fi

fail() { echo "❌ $1"; exit 1; }

echo "🔍 Smoke test..."

# Postgres
docker exec -i omni-postgres pg_isready -U "${DB_USER:-postgres}" >/dev/null 2>&1 \
    || fail "Postgres không ready"
echo "✅ Postgres"

# 3 DB đã được tạo
for db in identity_db order_db payment_db; do
    docker exec -i omni-postgres psql -U "${DB_USER:-postgres}" -lqt 2>/dev/null \
        | cut -d '|' -f1 | grep -qw "$db" || fail "Missing DB: $db"
done
echo "✅ 3 Postgres databases"

# MongoDB
docker exec -i omni-mongodb mongosh --quiet --eval "db.adminCommand('ping').ok" 2>/dev/null \
    | grep -q 1 || fail "MongoDB"
echo "✅ MongoDB"

# Redis
docker exec -i omni-redis redis-cli ping 2>/dev/null | grep -q PONG || fail "Redis"
echo "✅ Redis"

# Kafka
docker exec -i omni-kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 --list >/dev/null 2>&1 || fail "Kafka"
echo "✅ Kafka"

# Kafka topics
topics=$(docker exec -i omni-kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 --list 2>/dev/null)
echo "$topics" | grep -q "order.events"   || fail "Missing topic: order.events"
echo "$topics" | grep -q "payment.events" || fail "Missing topic: payment.events"
echo "✅ Kafka topics"

# Mailpit
curl -sf "http://localhost:${MAILPIT_UI_PORT:-8025}/api/v1/messages" >/dev/null \
    || fail "Mailpit"
echo "✅ Mailpit"

# Elasticsearch (chỉ khi bật profile search)
if docker ps --format '{{.Names}}' | grep -q omni-elasticsearch; then
    curl -sf "http://localhost:${ES_PORT:-9200}/_cluster/health" >/dev/null || fail "Elasticsearch"
    echo "✅ Elasticsearch"
else
    echo "⏭️  Elasticsearch không bật (bình thường nếu chưa cần)"
fi

echo ""
echo "🎉 Tất cả infrastructure healthy."