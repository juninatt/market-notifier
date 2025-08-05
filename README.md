# DD+ (Due Diligence Plus)

DD+ is a personal Java project built with Maven and Spring Boot.  
The goal is to create a modular, scheduled news subscription service with support for multiple sources (e.g. Finnhub, RSS) 
and multiple notification channels (email, SMS, push).

The project focuses on:
- ✅ Simplicity: no database, all configuration via JSON/YAML
- 🔐 Privacy: no personal data is stored
- 🧩 Extensibility: easy to add new modules for providers or notifiers

###  News providers: 

- **Finnhub**
Finnhub is a financial data API that offers a wide range of information, including general business news.
In this project, it's used to retrieve broad market-related news headlines.


- **Marketaux**
Marketaux is a financial news API focused on delivering headline articles filtered by criteria like company, region, or language.
In this project, it's used to complement Finnhub with more granular or targeted news sources.

### 🧾 User Subscriptions

Users define what news to receive by editing a simple YAML file.
The application loads this file at runtime and uses the subscriptions to query the correct providers.

By default, it looks for subscriptions.yml in the classpath.

---

## ⚙️ Configure & Run (in development)

Before running the project, you must provide valid API tokens for both Finnhub and Marketaux. 
These are required for the system to fetch any news — the application will not start without them.

Free tokens are easily obtained at:
- [finnhub.io](https://finnhub.io)
- [marketaux.com](https://marketaux.com)

API tokens are read from environment variables using the following format in `application.yml` in the `app-runner` module:

```yaml
 finnhub:
   api:
     token: ${FINNHUB_API_TOKEN}

 marketaux:
   api:
     token: ${MARKETAUX_API_TOKEN}
```

You can provide them in one of the following ways:

1. **Inline in terminal:**

 ```bash
    FINNHUB_API_TOKEN=your-token MARKETAUX_API_TOKEN=your-token ./mvnw -f app-runner/pom.xml spring-boot:run
 ```

2. **As system environment variables:** (Restart your terminal/IDE after setting.)
- **Windows**: [Set environment variables](https://learn.microsoft.com/en-us/windows/win32/procthread/environment-variables)
- **macOS / Linux**: [Set environment variables](https://wiki.archlinux.org/title/environment_variables)   

3. **Inside your IDE's run configuration (as environment variables):** 
Set the tokens as environment variables in your run configuration.


4. **Hardcode directly in application.yml:**
For local testing only, you can paste the tokens directly in application.yml.
(Not recommended if the file is tracked in version control.)

### ▶️ Start the application:

1. **Command line: Run the command above from the project root (ddplus/)**
or
2. **IDE: Run DDPlusApplication in the app-runner module**

---

## 💻 Development Notes

Lombok is used in this project so if you're using an IDE, make sure annotation processing is enabled in your settings.
No additional setup is needed when building or running from the command line with Maven.
