# DD+ (Due Diligence Plus) (In Development)

**DD+** is a personal Java project built with Maven and Spring Boot.  
It provides a modular, scheduled news subscription service with multiple sources (e.g., [Finnhub](https://finnhub.io/), [Marketaux](https://www.marketaux.com/))
and **delivers notifications via Telegram** using the [Telegram Bot API](https://core.telegram.org/bots/api)

The project focuses on:
- ✅ Simplicity: no database, all configuration via JSON/YAML
- 🔐 Privacy: no personal data is stored
- 🧩 Extensibility: easy to add new modules for providers or notifiers

###  News providers: 

- **Finnhub**
  Finnhub is a financial data API that offers a wide range of information, including general business news.

  - **Marketaux**
  Marketaux is a financial news API focused on delivering headline articles filtered by criteria like company, region, or language.

### Notifiers

- **Telegram**  
  Telegram is a cloud-based chat platform with private chats, large groups, and one-way broadcast channels.

---

## ⚙️ Configuration 

**Requirements:** Java 17+, Maven, Telegram, Finnhub & MarketAux accounts.

Before running the app you need valid API tokens for **Finnhub**, **Marketaux**, and a **Telegram bot token**.

### 1) Get API keys / tokens

**Finnhub / Marketaux (news sources)**
- Sign up and create free API keys:
- [finnhub.io](https://finnhub.io)
- [marketaux.com](https://marketaux.com)

**Telegram (notification channel)**
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

### 2) Configure application.yml

The app reads API keys/tokens from environment variables referenced in application.yml (module: app-runner):
```yaml
 finnhub:
   api:
     token: ${FINNHUB_API_TOKEN}

 marketaux:
   api:
     token: ${MARKETAUX_API_TOKEN}

 telegram:
   baseUrl: https://api.telegram.org
   botToken: ${TELEGRAM_BOT_TOKEN}
   chatIds: ${TELEGRAM_CHAT_IDS}  # e.g. 123456789,-100987654321
```

---

## 💻 Development Notes

Lombok is used in this project so if you're using an IDE, make sure annotation processing is enabled in your settings.
No additional setup is needed when building or running from the command line with Maven.
