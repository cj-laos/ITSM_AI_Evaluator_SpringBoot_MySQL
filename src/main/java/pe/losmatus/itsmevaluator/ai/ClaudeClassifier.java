package pe.losmatus.itsmevaluator.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.losmatus.itsmevaluator.config.AppProperties;
import pe.losmatus.itsmevaluator.domain.AiProvider;

@Component
@ConditionalOnProperty(name = "app.providers.claude.enabled", havingValue = "true")
public class ClaudeClassifier extends AbstractSpringAiClassifier {
    public ClaudeClassifier(@Qualifier("claudeChatModel") ChatModel model, PromptCatalog prompts,
                            ObjectMapper mapper, AppProperties properties) {
        super(AiProvider.CLAUDE, properties.providers().claude().model(), model, prompts, mapper,
                properties.providers().claude().inputUsdPerMillion(),
                properties.providers().claude().cacheReadUsdPerMillion(),
                properties.providers().claude().cacheWriteUsdPerMillion(),
                properties.providers().claude().outputUsdPerMillion());
    }
}
