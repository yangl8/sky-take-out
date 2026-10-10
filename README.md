# Sky Take Out

## Local configuration

1. Copy `.env.example` to `.env`.
2. Fill in the local database, JWT, and OSS values in `.env`.
3. In VS Code, run the `SkyApplication` launch configuration.

The `.env` file contains local credentials and is intentionally excluded from Git. The VS Code `SkyApplication` launch configuration explicitly uses the `dev` profile and loads this file.

### JWT keys

The application requires two independent HS256 signing keys in the local `.env` file: `JWT_ADMIN_SECRET_KEY` and `JWT_USER_SECRET_KEY`. Generate each value once on macOS with:

```sh
openssl rand -base64 32 | tr -d '\n'
```

Run the command separately for each variable, then put the two outputs in the matching `.env` entries. The command generates 32 random bytes and encodes them as Base64. Keep `.env` local, do not paste either value into tracked files or logs, and do not regenerate keys on each application start: changing a key invalidates tokens signed with its previous value. Startup fails with a configuration error if a key is missing, malformed, too short, or equal to the other key.

### Mock payment

The mock payment endpoint is enabled only by the local `dev` profile. The base configuration disables it, and the `prod` profile disables it even if `dev` is accidentally active at the same time. Use the provided VS Code launch configuration for local order-flow testing. Production deployments must explicitly activate `prod`; missing real WeChat payment configuration does not enable mock payment or fall back to it.

## Order status concurrency verification

Mockito tests cover service decisions and conditional-update arguments without connecting to a database. The separate MySQL integration test loads the project's actual `OrderMapper.xml` and races payment success against timeout cancellation in independent transactions and connections. It verifies that one update wins, the other affects zero rows, the final order and payment states agree, a timestamp exactly at the timeout cutoff is not cancelled, and repeated transitions do not overwrite a terminal state. This is a correctness test, not a load or performance test.

The test was verified against local MySQL 9.2.0. Each simultaneous-race round inserts a uniquely named row into `sky_take_out_test.orders` and deletes that row afterward; the application database `sky_take_out` is not used. The test also runs two coordinated ordering checks so both winner outcomes are covered deterministically without making the 100 simultaneous-race assertions depend on scheduler fairness.

### Test Results

| Verification | Result |
| --- | ---: |
| MySQL version | 9.2.0 |
| Formal simultaneous races | 100/100 passed |
| Payment / timeout cancellation wins | 72 / 28 |
| Inconsistent final order/payment states | 0 |
| Deterministic payment-first and timeout-first outcomes | Both passed |

Run the 10-round precheck with the same command below by setting `SKY_ORDER_RACE_ROUNDS=10`. Winner counts vary with thread scheduling; each round must still have exactly one successful conditional update. The two coordinated ordering checks guarantee both final-state outcomes without making correctness depend on scheduler fairness. These are correctness checks, not load or performance tests.

Create the isolated InnoDB test database and table once with the provided script:

```sh
mysql --protocol=TCP -h localhost -P 3306 -u root -p < sky-server/src/test/resources/mysql/order-status-race-schema.sql
```

Load the existing local `.env` values into the current shell, then set the test connection variables. Keep the password in a local environment variable and do not put it in a tracked file:

```sh
set -a
source ./.env
set +a
export SKY_ORDER_RACE_DB_URL='jdbc:mysql://localhost:3306/sky_take_out_test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true'
export SKY_ORDER_RACE_DB_USER='root'
export SKY_ORDER_RACE_DB_PASSWORD="$DB_PASSWORD"
```

Run the precheck and the formal test separately:

```sh
SKY_ORDER_RACE_ROUNDS=10 mvn -pl sky-server -am -Dtest=OrderStatusRaceMySqlIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
SKY_ORDER_RACE_ROUNDS=100 mvn -pl sky-server -am -Dtest=OrderStatusRaceMySqlIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The integration test skips when its dedicated connection variables are absent and rejects any JDBC URL that does not target `sky_take_out_test`. It requires an InnoDB `orders` table created by the test schema script. The MySQL run validates the mapper's conditional-update atomicity for these tested races; it does not replace broader service, payment-provider, or production-load testing.
