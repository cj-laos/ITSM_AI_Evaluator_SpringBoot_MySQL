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
@ConditionalOnProperty(name = "app.providers.gemini.enabled", havingValue = "true")
public class GeminiClassifier extends AbstractSpringAiClassifier {
    public GeminiClassifier(@Qualifier("geminiChatModel") ChatModel model, PromptCatalog prompts,
                            ObjectMapper mapper, AppProperties properties) {
        super(AiProvider.GEMINI, properties.providers().gemini().model(), model, prompts, mapper,
                properties.providers().gemini().inputUsdPerMillion(),
                properties.providers().gemini().cachedInputUsdPerMillion(),
                BigDecimal.ZERO,
                properties.providers().gemini().outputUsdPerMillion());
    }
}
