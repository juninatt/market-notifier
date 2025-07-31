# DD+ (Due Diligence Plus)

DD+ is a personal Java project built with Maven and Spring Boot.  
The goal is to create a modular, scheduled news subscription service with support for multiple sources (e.g. Finnhub, RSS) 
and multiple notification channels (email, SMS, push).

The project focuses on:
- Simplicity: no database, all configuration via JSON/YAML
- Privacy: no personal data is stored
- Extensibility: easy to add new modules for providers or notifiers

### News providers: 
#### Finnhub
Finnhub is a financial data API that offers a wide range of information, including general business news.
In this project, it's used to retrieve broad market-related news headlines.  

#### Marketaux
Marketaux is a financial news API focused on delivering headline articles filtered by criteria like company, region, or language.
In this project, it's used to complement Finnhub with more granular or targeted news sources.

---

## 🔧 Required Configuration

Before running the project, you must obtain API keys for both news providers
and add them to the `application.yml` file in the `app-runner` module:

```yaml
finnhub:
  api:
    token: YOUR_FINNHUB_API_KEY

marketaux:
  api:
    token: YOUR_MARKETAUX_API_KEY
```
     
Free keys are easily generated at:
- [finnhub.io](https://finnhub.io)
- [marketaux.com](https://marketaux.com)

---

## Running the Project

Before starting the application, make sure you have inserted valid API keys for both Finnhub and Marketaux. (see "Required Configuration" above).
Once configured, you can run the project in one of the following ways:

### From the command line (with Maven Wrapper)
Navigate to the root of the project (`ddplus/`) and run:

```bash
./mvnw -f app-runner/pom.xml spring-boot:run
```  

💡 This will start the Spring Boot application located in the `app-runner` module,
which serves as the entry point for the entire DD+ system.

### From an IDE

Open the project in your preferred IDE and locate the `DDPlusApplication` class  
inside the `app-runner` module (`se.pbt.runner.DDPlusApplication`). 

You can run it directly as a Spring Boot application using the IDE's run button or context menu.

---

### Development Notes

Lombok is used in this project so if you're using an IDE, make sure annotation processing is enabled in your settings.
No additional setup is needed when building or running from the command line with Maven.
