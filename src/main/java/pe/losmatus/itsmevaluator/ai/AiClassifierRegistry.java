package pe.losmatus.itsmevaluator.ai;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import pe.losmatus.itsmevaluator.domain.AiProvider;

@Component
public class AiClassifierRegistry {
    private final Map<AiProvider, AiClassifier> classifiers = new EnumMap<>(AiProvider.class);

    public AiClassifierRegistry(List<AiClassifier> available) {
        available.forEach(classifier -> classifiers.put(classifier.provider(), classifier));
    }

    public AiClassifier require(AiProvider provider) {
        var classifier = classifiers.get(provider);
        if (classifier == null) {
            throw new IllegalStateException("El proveedor " + provider
                    + " no está habilitado o no tiene API key configurada");
        }
        return classifier;
    }

    public Map<AiProvider, String> availableModels() {
        var result = new EnumMap<AiProvider, String>(AiProvider.class);
        classifiers.forEach((provider, classifier) -> result.put(provider, classifier.model()));
        return result;
    }
}
