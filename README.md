# Market Notifier

[![CI](https://github.com/juninatt/telegram-market-notifier/actions/workflows/ci.yml/badge.svg)](https://github.com/juninatt/telegram-market-notifier/actions/workflows/ci.yml)
[![CodeQL](https://github.com/juninatt/telegram-market-notifier/actions/workflows/codeql.yml/badge.svg)](https://github.com/juninatt/telegram-market-notifier/actions/workflows/codeql.yml)
[![codecov](https://codecov.io/gh/juninatt/telegram-market-notifier/branch/main/graph/badge.svg)](https://codecov.io/gh/juninatt/telegram-market-notifier)

**Market Notifier** is a personal Java project built with Maven and Spring Boot.
It integrates multiple financial data providers (e.g., [Finnhub](https://finnhub.io/), [Marketaux](https://www.marketaux.com/))
and delivers automated, scheduled updates through [Telegram](https://telegram.org/) and/or email.

Users interact with the application via the [Telegram Bot API](https://core.telegram.org/bots/api), where they can create and manage news subscriptions.
By sending simple commands such as `/subscribe <keyword>` or `/list`, the bot allows users to follow specific topics and receive financial news updates directly in Telegram.

For a more detailed usage guide, including full `/subscribe` syntax, examples, and parameter rules, see the [How to Use guide](_docs/how-to-use.md).

### Market data providers

- **Finnhub**
Financial data API that offers a wide range of information, including general business news.

- **Marketaux**
Financial news API focused on delivering headline articles filtered by criteria like company, region, or language.

### Delivery channels

- **Telegram** — interactive; subscriptions are created and managed via bot commands.
- **Email** — delivered via [Resend](https://resend.com).

A subscription can use either channel, both, or neither (if disabled) — the dispatcher resolves whichever address each registered channel needs per subscription.

### Creating a subscription

- **Via Telegram**: `/subscribe "<keywords>" <language> [schedule] <maxItems> [email]` — see the [How to Use guide](_docs/how-to-use.md). Adding an email as the last argument delivers that subscription to both Telegram and email.
- **Via email**: send an email to the configured inbox with a body of `subscribe "<keywords>" <language> [schedule] <maxItems>` (same syntax, no leading `/` needed). The subscription is delivered to your own sending address unless the body specifies a different one. Requires IMAP polling to be configured -- see Configuration below.

---

## ✅ Requirements

Java 17+, Maven, and accounts for [Finnhub](https://finnhub.io), [Marketaux](https://marketaux.com), [Telegram](https://core.telegram.org/bots/api), and — if you want email delivery — [Resend](https://resend.com).

---

## 🧩 Project structure

```text
telegram-market-notifier/
 ├── app-runner/         # Application entry point and global configuration
 ├── core/               # Shared domain models (NewsItem, NewsGroup, Notification, SchedulePreset, ...)
 ├── sources/            # Integrations for external financial news APIs (Finnhub, Marketaux)
 ├── subscription/       # Subscription storage, validation, and formatting
 ├── telegram/           # Telegram integration, bot commands, and message delivery
 ├── email/              # Email delivery via Resend
 └── dispatch/           # Ties schedules, sources, filtering, grouping, and delivery together
```

---

## ⚙️ Configuration

Before running the application, make sure you have valid API tokens for **Finnhub**, **Marketaux**, a **Telegram bot token**, and — for email delivery — a **Resend API key**.

### 1) Get API keys / tokens

**Finnhub / Marketaux (news sources)**
- Sign up to get free API keys:
- [finnhub.io](https://finnhub.io)
- [marketaux.com](https://marketaux.com)

**Telegram**
1. In Telegram, start a chat with **@BotFather**.
2. Send `/newbot`, follow the prompts (choose a name and a unique username ending in `bot`).
3. BotFather will reply with your **bot token** — keep it secret.
4. Start a chat with your new bot so it can message you back.
5. (Optional: for group delivery) add your bot to the group and send a message in the group.

**Find your chat ID(s)**
- Quick way: call `getUpdates` and read the `chat.id` from the response.
```bash
  curl -s "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getUpdates"
```

**Resend (email delivery)**
1. Sign up at [resend.com](https://resend.com) (free tier: 3,000 emails/month).
2. Verify a sending domain, or use Resend's shared test sender (`onboarding@resend.dev`) while developing --
   note that the shared sender can only deliver to the email address registered on your own Resend account,
   not to arbitrary recipients. Verify a real domain once you need to send to anyone else.
3. Create an API key.

**Gmail IMAP (subscribing by email, optional)**
1. Use a dedicated Gmail account (not a personal one) as the subscription inbox.
2. Enable 2-Step Verification on that account, then create an [App Password](https://myaccount.google.com/apppasswords).
3. Set `EMAIL_IMAP_ENABLED=true`, `EMAIL_IMAP_USERNAME` to the Gmail address, and `EMAIL_IMAP_PASSWORD` to the App Password (not the regular account password).
4. Other Microsoft consumer accounts (outlook.com, live.com, hotmail.com) no longer support this -- they require OAuth 2.0 instead of a plain username/password, which this app doesn't implement.

### 2) Configure application.yml

A central `application.yml` in `app-runner/resources` loads separate YAML files for each module.
**Configuration file structure:**

```yaml
app-runner/
└── src/main/resources/
├── application.yml
├── application-finnhub.yml
├── application-marketaux.yml
├── application-telegram.yml
├── application-subscription.yml
├── application-email.yml
└── application-watchlist.yml
```

Each file contains placeholders for its own API tokens and settings, read from environment variables:
`FINNHUB_API_KEY`, `MARKETAUX_API_KEY`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_IDS`, `RESEND_API_KEY`, `RESEND_FROM_ADDRESS`,
(optional, for subscribing by email) `EMAIL_IMAP_ENABLED`, `EMAIL_IMAP_HOST`, `EMAIL_IMAP_USERNAME`, `EMAIL_IMAP_PASSWORD`,
and (optional, for the startup watchlist digest -- see below) `WATCHLIST_DIGEST_ENABLED`, `WATCHLIST_DIGEST_TOP_PER_CATEGORY`.
All configuration files are loaded automatically when the application starts.

### 3) Enable email delivery for a subscription manually

Subscribing via Telegram or email (see above) sets this automatically. To edit it directly instead, add an `email` field to the subscription's entry in the subscriptions file (path set by `subscription.storage.path`, default `subscriptions.yml`):

```yaml
subscriptions:
- id: "sub-1"
  chatId: 123456789
  email: "you@example.com"
  schedule: MORNING
  filter:
    keywords: ["Tesla"]
    tickers: ["TSLA"]
    language: "en"
  maxItems: 10
  enabled: true
```

Leave `email` unset (or `null`) to only deliver via Telegram for that subscription.

### 4) Startup watchlist digest (optional)

In addition to scheduled subscriptions, you can maintain a personal watchlist that gets emailed to you once, every time the application starts -- handy for firing off `mvn spring-boot:run` in the morning and getting a digest immediately instead of waiting for the next scheduled delivery.

Create a `watchlist.yml` file (path set by `watchlist.storage.path`, default `watchlist.yml` in the working directory):

```yaml
email: "you@example.com"
companies:
  - "Tesla"
  - "TSLA"
  - "Apple"
categories:
  - "Space"
  - "AI"
  - "Quantum Computing"
```

- `companies` -- tickers or company names. Every news item matching any of them, from every configured source, is included with no upper limit.
- `categories` -- free-text topics matched case-insensitively against article titles and descriptions (so `"AI"`, `"ai"`, and `"Ai"` are equivalent). Only the most recently published items per category are included, up to `watchlist.digest.top-per-category` (default `10`).

If the file is missing, empty, or nothing in it currently matches any news, no email is sent. Set `watchlist.digest.enabled: false` (or `WATCHLIST_DIGEST_ENABLED=false`) to keep the file around without triggering a send on every startup.

---

## 🗓️ Delivery schedule

Each subscription picks a `schedule` from a fixed set of presets, each evaluated in its own timezone:

| Preset                  | Fires                                  | Timezone            |
|--------------------------|-----------------------------------------|----------------------|
| `MORNING`                 | 08:00 daily                             | Europe/Stockholm    |
| `EVENING`                 | 20:00 daily                             | Europe/Stockholm    |
| `MORNING_EVENING`         | 08:00 and 20:00 daily                   | Europe/Stockholm    |
| `MORNING_LUNCH_EVENING`   | 08:00, 12:00, and 20:00 daily           | Europe/Stockholm    |
| `EUROPE_MARKET_OPEN`      | 09:00, weekdays                         | Europe/Stockholm    |
| `EUROPE_MARKET_CLOSE`     | 17:30, weekdays                         | Europe/Stockholm    |
| `US_MARKET_OPEN`          | 09:30, weekdays                         | America/New_York    |
| `US_MARKET_CLOSE`         | 16:00, weekdays                         | America/New_York    |

The market-hour presets only fire on weekdays and are evaluated in the market's own timezone, so they land correctly even when US and European daylight saving transitions fall on different dates.

---

## ▶️ Run

To run the application, first build all modules and generate the necessary artifacts using Maven.

From the project root:
```bash
    mvn clean install
```

Once the build is complete, start the application with Spring Boot:
```bash
    mvn spring-boot:run -pl app-runner
```

This will:

1. Load the main configuration from `app-runner/resources/application.yml`.
2. Import module-specific configurations for Telegram, Finnhub, Marketaux, subscriptions, email, and the watchlist.
3. Send the one-off watchlist digest, if `watchlist.yml` exists (see [Startup watchlist digest](#4-startup-watchlist-digest-optional)).
4. Initialize all services and start the news dispatch scheduler, which checks every minute for a due schedule preset, fetches and groups news from all sources, and delivers matching items to each subscription's configured channels.

---

## 💻 Development Notes

Lombok is used in this project so if you're using an IDE, make sure annotation processing is enabled in your settings.
No additional setup is needed when building or running from the command line with Maven.
