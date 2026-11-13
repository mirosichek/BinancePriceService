# Binance Price Service

Spring Boot-сервис, который периодически забирает среднюю цену пары с Binance,
сохраняет её в PostgreSQL и отдаёт по REST: последнюю цену, среднюю за период,
историю с пагинацией и тренд за период.

- Java 25, Spring Boot 3.5.16
- PostgreSQL + Flyway (миграции применяются автоматически)
- Метрики и health через Spring Boot Actuator

## Требования

- JDK 25
- PostgreSQL 16+
- Docker — опционально, для запуска в контейнере
- Переменная окружения `DB_PASSWORD` — обязательна

## Быстрый старт

```powershell
# 1. создать базу
#    CREATE DATABASE trading_pair_db;

# 2. задать пароль
$env:DB_PASSWORD = "ваш_пароль"

# 3. запустить
.\mvnw.cmd spring-boot:run
```

## Запуск

### 1. Подготовить базу

Создать базу данных (по умолчанию `trading_pair_db`):

```sql
CREATE DATABASE trading_pair_db;
```

Таблицы и индексы создаст Flyway при старте приложения.

### 2. Задать переменные окружения

PowerShell:

```powershell
$env:DB_PASSWORD = "ваш_пароль"
```

Bash:

```bash
export DB_PASSWORD="ваш_пароль"
```

Без `DB_PASSWORD` приложение не стартует: в `application.properties` он объявлен
как `${DB_PASSWORD}` без значения по умолчанию.

### 3. Запустить

Локально через Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Или собрать и запустить jar:

```powershell
.\mvnw.cmd -DskipTests package
java -jar target/Binance-0.0.1-SNAPSHOT.jar
```

Из IDE: запустить `BinancePriceServiceApplication`, предварительно задав
`DB_PASSWORD` в Run Configuration → Environment variables.

По умолчанию приложение слушает порт **8080**. Изменить:

```powershell
java -jar target/Binance-0.0.1-SNAPSHOT.jar --server.port=8081
```

### Запуск в Docker

```powershell
docker build -t binance-price-service:local .
docker run --rm -p 8080:8080 `
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/trading_pair_db" `
  -e DB_USERNAME=postgres `
  -e DB_PASSWORD="ваш_пароль" `
  binance-price-service:local
```

`host.docker.internal` — адрес хоста из контейнера (Docker Desktop).
Если база тоже в контейнере, используйте имя её сервиса/контейнера вместо хоста.

## API

Базовый адрес: `http://localhost:8080`.

### Последняя сохранённая цена

```http
GET /api/prices/latest
GET /api/prices/latest?symbol=BTCUSDT
```

`symbol` необязателен; по умолчанию берётся `BINANCE_SYMBOL`. Регистр не важен.

```json
{
  "symbol": "BNBUSDT",
  "price": 631.62039181,
  "fetchedAt": "2026-11-13T05:02:51Z"
}
```

Нет данных по паре → `404`.

### Средняя цена за период

```http
GET /api/v1/prices/average?from=2026-11-12T00:00:00Z&to=2026-11-13T00:00:00Z
GET /api/v1/prices/average?from=2026-11-12T00:00:00Z&to=2026-11-13T00:00:00Z&symbol=BTCUSDT
```

```json
{ "averagePrice": 654.49473022 }
```

Считается в базе по сохранённым записям, округляется до 8 знаков.
Нет данных за период → `404`.

### История за период с пагинацией

```http
GET /api/v1/prices?from=2026-11-12T00:00:00Z&to=2026-11-13T00:00:00Z&page=0&size=20
```

- `page` — номер страницы, по умолчанию `0`, не может быть отрицательным;
- `size` — размер страницы, по умолчанию `20`, должен быть ≥ 1;
- сортировка: новые записи первыми (`fetchedAt DESC, id DESC`);
- пара — из `BINANCE_SYMBOL`.

```json
{
  "items": [
    {
      "symbol": "BNBUSDT",
      "price": 654.85561546,
      "fetchedAt": "2026-11-12T21:04:31Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 135,
  "hasNext": true
}
```

Пустой период → `200` с `items: []`, `totalElements: 0`, `hasNext: false`.

### Тренд за период

```http
GET /api/v1/prices/trend?from=2026-11-12T00:00:00Z&to=2026-11-13T00:00:00Z
```

```json
{
  "symbol": "BNBUSDT",
  "start_date": "2026-11-12",
  "end_date": "2026-11-13",
  "trend": "UPWARD"
}
```

- `trend`: `UPWARD`, `DOWNWARD` или `FLAT`;
- считается линейной регрессией по последним `trend.window-size` точкам периода;
- если изменение по линии регрессии меньше `trend.flat-threshold-pct` процентов
  от средней цены — `FLAT`;
- `start_date`/`end_date` — даты из переданных `from`/`to`;
- нет данных за период → `404`.

### Формат дат

ISO-8601 с обязательным часовым поясом:

```
2026-11-12T00:00:00Z
2026-11-12T03:00:00+03:00   → в URL: 2026-11-12T03%3A00%3A00%2B03%3A00
```

Границы периода **включительные**: `fetchedAt >= from` и `fetchedAt <= to`.
Дни в `start_date`/`end_date` берутся из переданного смещения.

### Ошибки

Все ошибки возвращаются в формате `ProblemDetail` (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Invalid period",
  "status": 400,
  "detail": "'from' must be before or equal to 'to'",
  "instance": "/api/v1/prices/trend"
}
```

| Статус | Когда |
|---|---|
| `400` | отсутствует `from`/`to`, неверный формат даты, `from > to`, `page < 0`, `size < 1`, неверный `symbol` |
| `404` | нет данных по паре / за период |
| `405` | неподдерживаемый HTTP-метод |
| `503` | ошибка обращения к Binance дошла до API |
| `500` | непредвиденная ошибка |

### Служебные endpoint'ы

```http
GET /actuator/health
GET /actuator/metrics
GET /actuator/metrics/binance.poll.result
GET /actuator/metrics/binance.poll.duration
GET /actuator/metrics/binance.poll.rate_limited
```

## Переменные окружения

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/trading_pair_db` | JDBC-URL базы |
| `DB_USERNAME` | `postgres` | пользователь БД |
| `DB_PASSWORD` | — (обязательна) | пароль БД |
| `BINANCE_SYMBOL` | `BNBUSDT` | торговая пара по умолчанию |
| `BINANCE_POLL_INTERVAL` | `PT30S` | интервал опроса Binance, ISO-8601 (`PT30S`, `PT1M`) |
| `BINANCE_BASE_URL` | `https://api.binance.com` | адрес API Binance |
| `BINANCE_SCHEDULING_ENABLED` | `true` | `false` — отключить фоновый опрос |
| `SERVER_PORT` | `8080` | порт HTTP-сервера |

### Дополнительные настройки

Заданы значениями по умолчанию в коде, при необходимости переопределяются
через параметры запуска или `SPRING_APPLICATION_JSON`:

| Свойство | По умолчанию |
|---|---|
| `binance.connect-timeout` | `5s` |
| `binance.read-timeout` | `10s` |
| `binance.rate-limit-default-retry-after` | `1m` |
| `trend.window-size` | `20` |
| `trend.flat-threshold-pct` | `0.1` |

Пример:

```powershell
java -jar target/Binance-0.0.1-SNAPSHOT.jar `
  --trend.window-size=50 `
  --binance.poll-interval=PT1M
```

## Как это работает

1. Планировщик с фиксированной задержкой (`BINANCE_POLL_INTERVAL`, по умолчанию 30 c)
   запрашивает `avgPrice` у Binance.
2. Ответ маппится в сущность `TradingPair` и сохраняется в `trading_pair`
   (уникальный индекс `symbol + fetched_at` отсекает дубликаты).
3. REST-контроллеры читают данные из базы; Binance при чтении не вызывается.
4. При `429`/`418` опрос приостанавливается на время `Retry-After`.

## Тесты

Юнит- и интеграционные тесты (48 штук), не требующие Docker:

```powershell
.\mvnw.cmd test "-Dtest=TrendCalculatorTest,PriceQueryServiceTest,PricePollingServiceTest,BinanceScheduleTest,TradingPairWriterTest,TradingPairMapperTest,BinanceClientWireMockTest,PriceApiWebTest"
```

Полный прогон:

```powershell
.\mvnw.cmd test
```

`BinancePriceServiceApplicationTests` поднимает контекст с Testcontainers
(PostgreSQL в Docker), поэтому для полного прогона нужен запущенный Docker.

## Структура проекта

```
src/main/java/com/example/BinancePriceService
├── Binance/        клиент Binance и его настройки
├── config/         Clock, RestClient, планировщик, настройки тренда
├── Controller/     REST-контроллеры и глобальный обработчик ошибок
├── doming/         сущность TradingPair и расчёт тренда
├── DTO/            модели запросов/ответов
├── exception/      доменные исключения
├── mapper/         маппинг Binance → сущность → DTO
├── Repository/     JPA-репозиторий и запись в БД
├── Schedule/       запуск опроса по расписанию
└── service/        опрос Binance и запросы к БД
```
