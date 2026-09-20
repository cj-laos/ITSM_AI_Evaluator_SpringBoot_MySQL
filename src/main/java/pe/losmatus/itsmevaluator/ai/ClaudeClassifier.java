package pe.losmatus.itsmevaluator.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicCacheOptions;
import org.springframework.ai.anthropic.api.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.api.AnthropicCacheTtl;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.losmatus.itsmevaluator.config.AppProperties;
import pe.losmatus.itsmevaluator.domain.AiProvider;

@Component
@ConditionalOnProperty(name = "app.providers.claude.enabled", havingValue = "true")
public class ClaudeClassifier extends AbstractSpringAiClassifier {

    private final AnthropicChatOptions requestOptions;

    public ClaudeClassifier(@Qualifier("claudeChatModel") ChatModel model, PromptCatalog prompts,
                            ObjectMapper mapper, AppProperties properties) {
        super(AiProvider.CLAUDE, properties.providers().claude().model(), model, prompts, mapper,
                properties.providers().claude().inputUsdPerMillion(),
                properties.providers().claude().cacheReadUsdPerMillion(),
                properties.providers().claude().cacheWriteUsdPerMillion(),
                properties.providers().claude().outputUsdPerMillion());

        this.requestOptions = buildRequestOptions(properties.providers().claude());
    }

    /**
     * Spring AI 1.1.8 no conserva cacheOptions desde defaultOptions cuando el
     * Prompt no contiene opciones de ejecución. Se envían por solicitud para
     * garantizar que Anthropic reciba el bloque cache_control del sistema.
     */
    @Override
    protected Prompt createPrompt(String description) {
        Prompt basePrompt = super.createPrompt(description);
        return new Prompt(basePrompt.getInstructions(), requestOptions);
    }

    private static AnthropicChatOptions buildRequestOptions(AppProperties.Claude config) {
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

        return optionsBuilder.build();
    }
}
