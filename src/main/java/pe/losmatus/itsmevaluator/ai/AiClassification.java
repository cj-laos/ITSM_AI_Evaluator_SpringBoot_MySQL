package pe.losmatus.itsmevaluator.ai;

import java.math.BigDecimal;
import pe.losmatus.itsmevaluator.domain.ResultStatus;

public record AiClassification(
        Integer categoryId,
        Integer severityId,
        ResultStatus status,
        String rawResponse,
        long latencyMs,
        int inputTokens,
        int outputTokens,
        int totalTokens,
        int cacheReadTokens,
        int cacheWriteTokens,
        BigDecimal estimatedCostUsd,
        String errorMessage
) {}
