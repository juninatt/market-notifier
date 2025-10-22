package se.pbt.marketnotifier.newsprovider.finnhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.pbt.marketnotifier.newsprovider.finnhub.config.FinnhubConfig;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: Use @ActiveProfiles("test") once a test profile with mock API config is set up
@SpringBootTest(classes = {FinnhubConfig.class, FinnhubNewsService.class})
class FinnhubNewsServiceTest {

    @Autowired
    private FinnhubNewsService service;

    @Test
    void fetchGeneralNews_shouldReturnNonEmptyResponse() {
        String response = service.fetchGeneralNews().block();

        assertThat(response)
                .isNotNull()
                .isNotBlank()
                .contains("category");
    }
}
