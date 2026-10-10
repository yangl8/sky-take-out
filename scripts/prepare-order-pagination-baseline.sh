#!/usr/bin/env bash
set -euo pipefail

readonly expected_database="sky_take_out_perf_test"
readonly script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly repository_directory="$(cd "${script_directory}/.." && pwd)"

if [[ "${SKY_ORDER_PAGE_DB_NAME:-}" != "${expected_database}" ]]; then
  echo "拒绝执行：SKY_ORDER_PAGE_DB_NAME 必须明确设置为 ${expected_database}" >&2
  exit 2
fi

for required_variable in SKY_ORDER_PAGE_DB_HOST SKY_ORDER_PAGE_DB_PORT SKY_ORDER_PAGE_DB_USER SKY_ORDER_PAGE_DB_PASSWORD; do
  if [[ -z "${!required_variable:-}" ]]; then
    echo "缺少环境变量：${required_variable}" >&2
    exit 2
  fi
done

export MYSQL_PWD="${SKY_ORDER_PAGE_DB_PASSWORD}"
mysql --protocol=tcp --host="${SKY_ORDER_PAGE_DB_HOST}" --port="${SKY_ORDER_PAGE_DB_PORT}" \
  --user="${SKY_ORDER_PAGE_DB_USER}" < "${repository_directory}/sky-server/src/test/resources/mysql/order-pagination-baseline-schema.sql"
mysql --protocol=tcp --host="${SKY_ORDER_PAGE_DB_HOST}" --port="${SKY_ORDER_PAGE_DB_PORT}" \
  --user="${SKY_ORDER_PAGE_DB_USER}" --database="${expected_database}" \
  < "${repository_directory}/sky-server/src/test/resources/mysql/order-pagination-baseline-data.sql"

echo "已在 ${expected_database} 重建 10,000 个订单及 30,000 条明细"
