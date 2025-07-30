# DD+ (Due Diligence Plus)

DD+ is a personal Java project built with Maven and Spring Boot.  
The goal is to create a modular, scheduled news subscription service with support for multiple sources (e.g. Finnhub, RSS) 
and multiple notification channels (email, SMS, push).

The project focuses on:
- Simplicity: no database, all configuration via JSON/YAML
- Privacy: no personal data is stored
- Extensibility: easy to add new modules for providers or notifiers

## Development Notes

Lombok is used in this project so if you're using an IDE, make sure annotation processing is enabled in your settings.

No additional setup is needed when building or running from the command line with Maven.