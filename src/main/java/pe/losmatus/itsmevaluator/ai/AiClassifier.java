package pe.losmatus.itsmevaluator.ai;

import pe.losmatus.itsmevaluator.domain.AiProvider;

public interface AiClassifier {
    AiProvider provider();
    String model();
    AiClassification classify(String description);
}
