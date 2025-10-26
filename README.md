# Market Notifier

**Market Notifier** is a personal Java project built with Maven and Spring Boot.  
It provides a modular, scheduled news subscription service with multiple sources (e.g., [Finnhub](https://finnhub.io/), [Marketaux](https://www.marketaux.com/))
and **delivers notifications via Telegram** using the [Telegram Bot API](https://core.telegram.org/bots/api)

Users interact with the application through a Telegram bot, where they can create and manage news subscriptions.  
By sending simple commands such as `/subscribe <keyword>` or `/list`, the bot allows users to follow specific topics and receive financial news updates directly in Telegram.

### News providers
- **Finnhub**  
Financial data API that offers a wide range of information, including general business news.

- **Marketaux**  
Financial news API focused on delivering headline articles filtered by criteria like company, region, or language.

### Notifiers
- **Telegram**  
Cloud-based chat platform with private chats, large groups, and one-way broadcast channels.

---

## ✅ Requirements
Java 17+, Maven, and accounts for [Finnhub](https://finnhub.io), [Marketaux](https://marketaux.com), and [Telegram](https://core.telegram.org/bots/api)

---

## 🧩 Project structure

```text
marketnotifier/
 ├── app-runner/         # Application entry point and global configuration
 ├── core/               # Shared logic, models and utilities
 ├── newsprovider/       # Modules for fetching financial news (e.g. Finnhub, Marketaux)
 ├── notifier/           # Notification channels (e.g. Telegram)
 └── subscription/       # Handles user subscriptions and scheduling
```
---

## ⚙️ Configuration
Before running the application, make sure you have valid API tokens for **Finnhub**, **Marketaux**, and a **Telegram bot token**.

### 1) Get API keys / tokens
**Finnhub / Marketaux (news sources)**
- Sign up to get free API keys:
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
A central `application.yml` in `app-runner/resources` includes separate YAML files for each provider and channel.

**Configuration file structure:**
```yaml
app-runner/  
└── src/main/resources/  
├── application.yml  
├── application-finnhub.yml  
├── application-marketaux.yml  
└── application-telegram.yml
```

Each file contains placeholders for its own API tokens and settings.  
After obtaining your tokens and Telegram chat ID, open the corresponding file and add or reference them as needed.  
All configuration files are loaded automatically when the application starts.

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
2. Import module-specific configurations for Telegram, Finnhub, and Marketaux.
3. Initialize all services and start scheduled background jobs.

---

## 💻 Development Notes
Lombok is used in this project so if you're using an IDE, make sure annotation processing is enabled in your settings.
No additional setup is needed when building or running from the command line with Maven.
