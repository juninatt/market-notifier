# How to use Telegram Market Notifier

Telegram Market Notifier delivers financial news updates directly through Telegram.  
Once the application is running, users interact with a Telegram bot to manage their news subscriptions.

---

## 🪄 1) Start the bot

1. Open Telegram and search for your bot by the username you created with **@BotFather**.  
   *(Example: `@marketnotifier_bot`)*
2. Start a chat with your bot and press **Start** or type `/start`.  
   The bot will reply with a welcome message and basic instructions.

---

## 📰 2) Create subscriptions

Use the `/subscribe` command to follow specific keywords or topics.  

**Syntax**
```text
/subscribe <keywords...> <language> [schedule] <maxItems>
```

**Parameters**

| Parameter | Description |
|----------|-------------|
| `<keywords>` | One or more keywords to match against article titles and summaries. Quotes are required only if the keyword contains spaces. |
| `<language>` | **Required.** Must be exactly two letters (e.g. `en`, `sv`, `es`). |
| `[schedule]` | **Optional. Must appear before `<maxItems>` if provided.** Valid options:<br>• `morning`, `m` — morning only<br>• `evening`, `e` — evening only<br>• `morning_evening`, `me` — morning and evening<br>• `morning_lunch_evening`, `mle` — morning, lunch, and evening |
| `<maxItems>` | **Required.** Must be an integer. |


Examples
```text
/subscribe Spotify sv 5 
/subscribe Microsoft Google en 10 
/subscribe "AI Bubble" "Silicon Valley" en me 20 
/subscribe "Green energy" sun wind es morning 15
```

Notes:
* If [schedule] is omitted, `morning_evening` will be applied automatically.
* All scheduled updates use the Europe/Stockholm timezone.
* Each subscription is stored in the subscriptions.yml file and linked to your Telegram chat ID.

---

## 📋 3) List active subscriptions

To see which topics you’re currently subscribed to, send:
```text
/list
```

The bot will respond with a numbered list of your saved keywords.

---

## 🆘 4) Get help

At any time, you can send:
```text
/help
```

to display a short description of all available commands.

---

## 🔄 5) Receive updates

The bot automatically sends you the latest financial news based on your subscriptions.  
Articles are delivered as plain text messages containing:
- Headline
- Summary
- Source
- Direct link to the article

Delivery happens at regular intervals based on the app’s scheduler configuration.

---

## ⚙️ 6) Manage group chats (optional)

Market Notifier also works in Telegram groups or channels:
1. Add your bot to the group.
2. Send at least one command (e.g. `/help`) so the bot can register the group’s chat ID.
3. Subscriptions created in a group apply to the entire group.

---

## ✅ Summary

| Command | Description |
|----------|--------------|
| `/subscribe <keywords> <language> [schedule] <maxItems>` | Subscribe to a topic |
| `/unsubscribe <keyword>` | Remove a specific topic |
| `/list` | Show active subscriptions |
| `/help` | Show help message |

---

### Notes
- Subscriptions are saved locally in `subscriptions/telegram-subscriptions.yml`.
- Each chat (private or group) has its own section in the file.
- News delivery frequency is defined by the internal scheduler in `JobRunner`.
