# Market Notifier

[![CI](https://github.com/juninatt/market-notifier/actions/workflows/ci.yml/badge.svg)](https://github.com/juninatt/market-notifier/actions/workflows/ci.yml)
[![CodeQL](https://github.com/juninatt/market-notifier/actions/workflows/codeql.yml/badge.svg)](https://github.com/juninatt/market-notifier/actions/workflows/codeql.yml)
[![codecov](https://codecov.io/gh/juninatt/market-notifier/branch/main/graph/badge.svg)](https://codecov.io/gh/juninatt/market-notifier)

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
- **Via email**: send an email to the configured inbox with a body of `subscribe "<keywords>" <language> [schedule] <maxItems>` (same syntax, no leading `/` needed). The subscription is delivered to your own sending address unless the body specifies a different one. Requires IMAP polling to be configured — see Configuration below.

---

## ✅ Requirements

Java 17+, Maven, and accounts for [Finnhub](https://finnhub.io) and [Marketaux](https://marketaux.com). For delivery, at least one of [Telegram](https://core.telegram.org/bots/api) or [Resend](https://resend.com) (email) — see [Running Telegram-free](#running-telegram-free).

---

## 🧩 Project structure

```text
market-notifier/
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

Before running the application, make sure you have valid API tokens for **Finnhub** and **Marketaux**, plus at least one delivery channel: a **Telegram bot token** and/or a **Resend API key**.

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
2. Verify a sending domain, or use Resend's shared test sender (`onboarding@resend.dev`) while developing —
   note that the shared sender can only deliver to the email address registered on your own Resend account,
   not to arbitrary recipients. Verify a real domain once you need to send to anyone else.
3. Create an API key.

**Gmail IMAP (subscribing by email, optional)**
1. Use a dedicated Gmail account (not a personal one) as the subscription inbox.
2. Enable 2-Step Verification on that account, then create an [App Password](https://myaccount.google.com/apppasswords).
3. Set `EMAIL_IMAP_ENABLED=true`, `EMAIL_IMAP_USERNAME` to the Gmail address, and `EMAIL_IMAP_PASSWORD` to the App Password (not the regular account password).
4. Other Microsoft consumer accounts (outlook.com, live.com, hotmail.com) no longer support this — they require OAuth 2.0 instead of a plain username/password, which this app doesn't implement.

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
└── application-email.yml
```

Each file contains placeholders for its own API tokens and settings, read from environment variables:
`FINNHUB_API_KEY`, `MARKETAUX_API_KEY`, `RESEND_API_KEY`, `RESEND_FROM_ADDRESS`,
(optional, Telegram is not required — see [Running Telegram-free](#running-telegram-free)) `TELEGRAM_BOT_ENABLED`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_IDS`,
(optional, for subscribing by email) `EMAIL_IMAP_ENABLED`, `EMAIL_IMAP_HOST`, `EMAIL_IMAP_USERNAME`, `EMAIL_IMAP_PASSWORD`,
and (optional, for the `digest-now` profile — see below) `DIGEST_TOP_PER_CATEGORY`.
All configuration files are loaded automatically when the application starts.

### Running Telegram-free

`TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_IDS` can both be left unset. The Telegram bot simply
stays off (logged once at startup) instead of blocking the rest of the application — useful if
you only want email delivery. Set `TELEGRAM_BOT_ENABLED=false` explicitly to turn that startup
error about a missing bot token into a plain info line. The reverse holds too: if you only use
Telegram, `RESEND_API_KEY` and `RESEND_FROM_ADDRESS` can be left unset — the email channel just
won't deliver anything, with no effect on Telegram.

### 3) Editing the subscriptions file directly

Every subscription — however it was created — lives in one YAML file (path set by
`subscription.storage.path`, default `subscriptions/subscriptions.yml`). Subscribing via
Telegram or email (see above) appends to it automatically; you can also hand-edit it, which is
the only way to set `companies` and `categories` (not exposed through `/subscribe` syntax yet):

```yaml
subscriptions:
- id: "sub-1"
  chatId: 123456789
  email: "you@example.com"
  schedule: MORNING
  filter:
    keywords: ["Tesla"]
    tickers: ["TSLA"]
    companies: ["Apple", "MSFT"]
    categories: ["AI", "Space"]
    language: "en"
  maxItems: 10
  enabled: true
```

- `keywords` / `tickers` / `language` — the original filter: each non-empty one must match
  (AND), matched items capped to `maxItems`. Settable via `/subscribe`.
- `companies` — tickers or company names, independent of the above. Matches on ticker OR name
  appearing in the text, with every match included (no cap). Hand-edit only.
- `categories` — free-text topics, independent of everything else, matched case-insensitively
  against title/description (so `"AI"`, `"ai"`, and `"Ai"` are equivalent). Only the most
  recently published matches per category are included, up to `digest.top-per-category`
  (default `10`). Hand-edit only.
- `email` — ignored entirely by Telegram delivery; only the `email` channel reads it. Leave it
  unset (or `null`) to only deliver via Telegram.
- `schedule` — optional. Omit it entirely for a subscription that should never fire on the
  recurring scheduler and only ever be sent by the `digest-now` profile below.

### 4) The `digest-now` profile: send everything right now, once, and exit

Instead of waiting for a subscription's `schedule` to fire, you can run the application in the
`digest-now` Spring profile: it sends every **enabled** subscription its own digest exactly
once — combining its regular `keywords`/`tickers` matches, its `companies` matches, and its
`categories` matches into a single message — then exits the process. The recurring scheduler,
Telegram polling, and IMAP polling are all excluded under this profile, so nothing else starts
in the background.

```bash
mvn spring-boot:run -pl app-runner -Dspring-boot.run.profiles=digest-now
```

This is the "run it in the morning and get one email/Telegram message right away" use case.
A subscription with no `schedule` at all is only ever reachable this way. If nothing in a
subscription's filter currently matches any fetched news, that subscription is silently
skipped — no blank message is sent.

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
2. Import module-specific configurations for Telegram, Finnhub, Marketaux, subscriptions, and email.
3. Initialize all services and start the news dispatch scheduler, which checks every minute for a due schedule preset, fetches and groups news from all sources, and delivers matching items to each subscription's configured channels.

Run with `-Dspring-boot.run.profiles=digest-now` instead to skip the recurring scheduler
entirely and send every enabled subscription's digest once immediately — see
[The `digest-now` profile](#4-the-digest-now-profile-send-everything-right-now-once-and-exit).

---

## 💻 Development Notes

Lombok is used in this project so if you're using an IDE, make sure annotation processing is enabled in your settings.
No additional setup is needed when building or running from the command line with Maven.
