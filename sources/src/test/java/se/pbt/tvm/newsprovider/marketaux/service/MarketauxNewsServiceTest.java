package se.pbt.tvm.newsprovider.marketaux.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import se.pbt.tvm.newsprovider.marketaux.config.MarketauxConfig;

// TODO: Use @ActiveProfiles("test") once a test profile with mock API config is set up
@SpringBootTest(classes = {MarketauxConfig.class, MarketauxNewsService.class})
public class MarketauxNewsServiceTest {

    @Autowired
    private MarketauxNewsService newsService;

    @Test
    void fetchLatestNews_shouldReturnResults() {
        Mono<String> result = newsService.fetchLatestNews();
        result.blockOptional().ifPresent(news ->
                news.lines().forEach(System.out::println)
        );
    }
}
