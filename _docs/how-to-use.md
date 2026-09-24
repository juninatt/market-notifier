# How to use Market Notifier

Market Notifier delivers financial news updates directly through Telegram.  
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
/subscribe <keywords...> <language> [schedule] <maxItems> [email]
```

**Parameters**

| Parameter | Description |
|----------|-------------|
| `<keywords>` | One or more keywords to match against article titles and summaries. Quotes are required only if the keyword contains spaces. |
| `<language>` | **Required.** Must be exactly two letters (e.g. `en`, `sv`, `es`). |
| `[schedule]` | **Optional. Must appear before `<maxItems>` if provided.** Valid options:<br>• `morning`, `m` — morning only<br>• `evening`, `e` — evening only<br>• `morning_evening`, `me` — morning and evening<br>• `morning_lunch_evening`, `mle` — morning, lunch, and evening<br>• `europe_open`, `eo` / `europe_close`, `ec` — European market open/close<br>• `us_open`, `uo` / `us_close`, `uc` — US market open/close |
| `<maxItems>` | **Required.** Must be an integer. |
| `[email]` | **Optional. Must be the last token.** When present, this subscription is also delivered by email in addition to Telegram. |


Examples
```text
/subscribe Spotify sv 5 
/subscribe Microsoft Google en 10 
/subscribe "AI Bubble" "Silicon Valley" en me 20 
/subscribe "Green energy" sun wind es morning 15
/subscribe Tesla en 10 you@example.com
```

Notes:
* If [schedule] is omitted, `morning_evening` will be applied automatically.
* Each preset fires in its own configured timezone -- see the [README](../README.md) for details.
* Each subscription is stored in the subscriptions.yml file and linked to your Telegram chat ID (and, if provided, your email address).

**Subscribing by email instead:** if IMAP polling is configured (see the README), you can create
a subscription by emailing the configured inbox with the same syntax in the body, minus the
leading `/`:
```text
subscribe Tesla en 10
```
The subscription is delivered to the address you sent from, unless the body itself ends with a
different email address.

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

The bot automatically sends you the latest financial news based on your subscriptions, formatted with:
- Headline
- Summary
- Source and publish date
- Related tickers
- Direct link to the article

If you added an email address when subscribing, the same update is also sent there.

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
| `/subscribe <keywords> <language> [schedule] <maxItems> [email]` | Subscribe to a topic |
| `/unsubscribe <keyword>` | Remove a specific topic |
| `/list` | Show active subscriptions |
| `/help` | Show help message |

---

### Notes
- Subscriptions are saved locally in `subscriptions/subscriptions.yml` (configurable via `subscription.storage.path`).
- Each chat (private or group) has its own section in the file.
- News delivery frequency is defined by each subscription's schedule preset, dispatched by `NewsDispatchScheduler`.
- `/subscribe` only sets `keywords`, `tickers`, and `language`. Two more filter fields --
  `companies` (matches ticker or name, uncapped) and `categories` (top-N most recent per
  category) -- exist only for hand-editing `subscriptions.yml` directly; see the main
  [README](../README.md) for their syntax and the `digest-now` profile that sends every
  enabled subscription immediately instead of waiting for its schedule.
