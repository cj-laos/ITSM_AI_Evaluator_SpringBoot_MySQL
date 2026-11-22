package pe.losmatus.itsmevaluator.config;

import com.google.genai.Client;
import java.util.Locale;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.anthropic.api.AnthropicCacheOptions;
import org.springframework.ai.anthropic.api.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.api.AnthropicCacheTtl;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfiguration {

    @Bean
    @Qualifier("openAiChatModel")
    @ConditionalOnProperty(
            name = "app.providers.openai.enabled",
            havingValue = "true"
    )
    ChatModel openAiChatModel(AppProperties properties) {
        var config = properties.providers().openai();

        var api = OpenAiApi.builder()
                .apiKey(config.apiKey())
                .build();

        var options = OpenAiChatOptions.builder()
                .model(config.model())
                .maxCompletionTokens(256)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @Qualifier("geminiChatModel")
    @ConditionalOnProperty(
            name = "app.providers.gemini.enabled",
            havingValue = "true"
    )
    ChatModel geminiChatModel(AppProperties properties) {
        var config = properties.providers().gemini();

        var client = Client.builder()
                .apiKey(config.apiKey())
                .build();

        GoogleGenAiThinkingLevel thinkingLevel =
                parseGeminiThinkingLevel(config.thinkingLevel());

        var options = GoogleGenAiChatOptions.builder()
                .model(config.model())
                .maxOutputTokens(config.maxOutputTokens())
                .responseMimeType("application/json")
                .thinkingLevel(thinkingLevel)
                .build();

        return GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .defaultOptions(options)
                .build();
    }

    private GoogleGenAiThinkingLevel parseGeminiThinkingLevel(String value) {
        if (value == null || value.isBlank()) {
            return GoogleGenAiThinkingLevel.THINKING_LEVEL_UNSPECIFIED;
        }

        String normalizedValue = value
                .trim()
                .toUpperCase(Locale.ROOT);

        try {
            return GoogleGenAiThinkingLevel.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Nivel de razonamiento de Gemini no válido: "
                            + value
                            + ". Valores admitidos por esta versión: "
                            + "LOW, HIGH o THINKING_LEVEL_UNSPECIFIED.",
                    exception
            );
        }
    }

    @Bean
    @Qualifier("claudeChatModel")
    @ConditionalOnProperty(
            name = "app.providers.claude.enabled",
            havingValue = "true"
    )
    ChatModel claudeChatModel(AppProperties properties) {
        var config = properties.providers().claude();

        var api = AnthropicApi.builder()
                .apiKey(config.apiKey())
                .build();

        var optionsBuilder = AnthropicChatOptions.builder()
                .model(config.model())
                .maxTokens(config.maxOutputTokens());

        if (config.cacheEnabled()) {
            optionsBuilder.cacheOptions(
                    AnthropicCacheOptions.builder()
                            .strategy(AnthropicCacheStrategy.SYSTEM_ONLY)
                            .messageTypeTtl(
                                    MessageType.SYSTEM,
                                    AnthropicCacheTtl.FIVE_MINUTES
                            )
                            .build()
            );
        }

        return AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(optionsBuilder.build())
                .build();
    }
}