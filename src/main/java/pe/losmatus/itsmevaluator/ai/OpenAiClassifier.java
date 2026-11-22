package pe.losmatus.itsmevaluator.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.losmatus.itsmevaluator.config.AppProperties;
import pe.losmatus.itsmevaluator.domain.AiProvider;

@Component
@ConditionalOnProperty(name = "app.providers.openai.enabled", havingValue = "true")
public class OpenAiClassifier extends AbstractSpringAiClassifier {
    public OpenAiClassifier(@Qualifier("openAiChatModel") ChatModel model, PromptCatalog prompts,
                            ObjectMapper mapper, AppProperties properties) {
        super(AiProvider.OPENAI, properties.providers().openai().model(), model, prompts, mapper,
                properties.providers().openai().inputUsdPerMillion(),
                properties.providers().openai().cachedInputUsdPerMillion(),
                BigDecimal.ZERO,
                properties.providers().openai().outputUsdPerMillion());
    }
}
