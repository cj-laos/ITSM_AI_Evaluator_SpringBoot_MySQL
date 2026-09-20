package pe.losmatus.itsmevaluator.ai;

import org.springframework.web.bind.annotation.*;
import pe.losmatus.itsmevaluator.domain.AiProvider;

@RestController
@RequestMapping("/api/classifications")
public class ClassificationController {

    private final AiClassifierRegistry classifiers;

    public ClassificationController(AiClassifierRegistry classifiers) {
        this.classifiers = classifiers;
    }

    @PostMapping("/{provider}")
    public SingleClassificationResponse classify(
            @PathVariable AiProvider provider,
            @RequestBody ClassificationRequest request) {

        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException(
                    "La descripción no puede estar vacía"
            );
        }

        var classifier = classifiers.require(provider);
        var result = classifier.classify(request.description());

        return new SingleClassificationResponse(
                provider,
                classifier.model(),
                result
        );
    }

    public record ClassificationRequest(
            String description
    ) {}

    public record SingleClassificationResponse(
            AiProvider provider,
            String model,
            AiClassification result
    ) {}
}