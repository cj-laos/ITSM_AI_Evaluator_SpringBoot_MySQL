package pe.losmatus.itsmevaluator.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Prompt prompt,
        Execution execution,
        Providers providers
) {

    public record Prompt(
            String resource,
            String version
    ) {
    }

    public record Execution(
            int threadPoolSize,
            int maxAttempts,
            long retryBackoffMs
    ) {
    }

    public record Providers(
            OpenAi openai,
            Gemini gemini,
            Claude claude
    ) {
    }

    public record OpenAi(
            boolean enabled,
            String apiKey,
            String model,
            BigDecimal inputUsdPerMillion,
            BigDecimal cachedInputUsdPerMillion,
            BigDecimal outputUsdPerMillion
    ) {
    }

    public record Gemini(
            boolean enabled,
            String apiKey,
            String model,
            String thinkingLevel,
            Integer maxOutputTokens,
            BigDecimal inputUsdPerMillion,
            BigDecimal cachedInputUsdPerMillion,
            BigDecimal outputUsdPerMillion
    ) {
    }

    public record Claude(
        boolean enabled,
        String apiKey,
        String model,
        Integer maxOutputTokens,
        boolean cacheEnabled,
        BigDecimal inputUsdPerMillion,
        BigDecimal cacheReadUsdPerMillion,
        BigDecimal cacheWriteUsdPerMillion,
        BigDecimal outputUsdPerMillion
) {
}
}