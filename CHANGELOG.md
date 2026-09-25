# Changelog

## [1.2.0](https://github.com/juninatt/market-notifier/compare/v1.1.0...v1.2.0) (2026-09-25)


### Features

* **dispatch,telegram:** send the same subscription digest from the scheduler and digest-now ([8f4bae6](https://github.com/juninatt/market-notifier/commit/8f4bae6484f5b0c04c124e2f6cc843b057ba2136))
* **dispatch:** add a digest-now profile that sends every enabled subscription once and exits ([e7dcb75](https://github.com/juninatt/market-notifier/commit/e7dcb758aeac8c98154ae9c2080fab8a3971ace7))
* **dispatch:** match companies and categories against a subscription's filter ([7afef7b](https://github.com/juninatt/market-notifier/commit/7afef7b0fdbfc3fc3019386f17717ce1f069c1f8))
* **dispatch:** send a one-off watchlist digest on startup ([861dbbd](https://github.com/juninatt/market-notifier/commit/861dbbd980314521b84806e88f485b278c3d6f16)) — superseded within this release by the digest-now profile; the watchlist is not part of 1.2.0
* **sources:** add Spaceflight News as a news source ([30ab88e](https://github.com/juninatt/market-notifier/commit/30ab88ec34dd8764701749b03ab294c4b4459e9a))


### Bug Fixes

* **dispatch:** stop matching everything on empty or blank filter values ([ea3b40d](https://github.com/juninatt/market-notifier/commit/ea3b40da1834286c3aa5fee250f34f6e776fe3ce))
* **telegram:** tolerate a missing bot token and chat ids at startup ([44b515a](https://github.com/juninatt/market-notifier/commit/44b515a33613630e8ba5ef2eb355c48ecc6ffffa))

## [1.1.0](https://github.com/juninatt/market-notifier/compare/v1.0.2...v1.1.0) (2026-09-18)


### Features

* **build:** migrate to Jackson 3 on Spring Boot 4.1.1 ([baec2f7](https://github.com/juninatt/market-notifier/commit/baec2f794f6bb11dc487d6a6636f61297cb0b4d6))


### Bug Fixes

* **build:** pin jackson, netty, and logback to patched versions ([d65a68b](https://github.com/juninatt/market-notifier/commit/d65a68b88a21e437a4658fb0fe4f61c08317b808))

## [1.0.2](https://github.com/juninatt/market-notifier/compare/v1.0.1...v1.0.2) (2026-09-16)


### Bug Fixes

* **sources:** declare jackson-databind as an explicit dependency ([5a142f0](https://github.com/juninatt/market-notifier/commit/5a142f09c6cd4a85ddeaa232cf59b190a04c33df))

## [1.0.1](https://github.com/juninatt/market-notifier/compare/v1.0.0...v1.0.1) (2026-09-09)


### Bug Fixes

* **subscription:** commit missing test fixtures for SubscriptionStorageTest ([2b2224e](https://github.com/juninatt/market-notifier/commit/2b2224edb0b180da21e4575dce579d26b0154616))
