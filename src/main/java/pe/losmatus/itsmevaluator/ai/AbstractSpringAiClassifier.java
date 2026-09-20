package pe.losmatus.itsmevaluator.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.api.OpenAiApi;

import pe.losmatus.itsmevaluator.domain.AiProvider;
import pe.losmatus.itsmevaluator.domain.ResultStatus;

abstract class AbstractSpringAiClassifier implements AiClassifier {

    private static final BigDecimal MILLION =
            new BigDecimal("1000000");

    private final AiProvider provider;
    private final String model;
    private final ChatModel chatModel;
    private final PromptCatalog prompts;
    private final ObjectMapper objectMapper;
    private final BigDecimal inputPrice;
    private final BigDecimal cachedInputPrice;
    private final BigDecimal cacheWritePrice;
    private final BigDecimal outputPrice;

    protected AbstractSpringAiClassifier(
            AiProvider provider,
            String model,
            ChatModel chatModel,
            PromptCatalog prompts,
            ObjectMapper objectMapper,
            BigDecimal inputPrice,
            BigDecimal cachedInputPrice,
            BigDecimal cacheWritePrice,
            BigDecimal outputPrice
    ) {
        this.provider = provider;
        this.model = model;
        this.chatModel = chatModel;
        this.prompts = prompts;
        this.objectMapper = objectMapper;
        this.inputPrice = nonNull(inputPrice);
        this.cachedInputPrice = nonNull(cachedInputPrice);
        this.cacheWritePrice = nonNull(cacheWritePrice);
        this.outputPrice = nonNull(outputPrice);
    }

    @Override
    public AiProvider provider() {
        return provider;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public AiClassification classify(String description) {
        long started = System.nanoTime();

        ChatResponse response;

        try {
            var prompt = new Prompt(List.of(
                    new SystemMessage(prompts.systemPrompt()),
                    new UserMessage(prompts.userPrompt(description))
            ));

            response = chatModel.call(prompt);

        } catch (Exception ex) {
            return error(started, ex);
        }

        long latencyMs = elapsedMs(started);

        String raw = response == null
                ? null
                : response.getResults().stream()
                        .map(result -> result.getOutput().getText())
                        .filter(Objects::nonNull)
                        .filter(text -> !text.isBlank())
                        .reduce((first, second) -> second)
                        .orElse(null);

        TokenUsage tokens = tokenUsage(response);

        try {
            JsonNode json = objectMapper.readTree(cleanJson(raw));

            int categoryId = intField(
                    json,
                    "categoriaId",
                    "categoria_id"
            );

            int severityId = intField(
                    json,
                    "severidadId",
                    "severidad_id"
            );

            if (categoryId < 1 || categoryId > 5
                    || severityId < 1 || severityId > 4) {

                throw new IllegalArgumentException(
                        "La respuesta contiene valores fuera del catálogo"
                );
            }

            return new AiClassification(
                    categoryId,
                    severityId,
                    ResultStatus.SUCCESS,
                    raw,
                    latencyMs,
                    tokens.input(),
                    tokens.output(),
                    tokens.total(),
                    tokens.cacheRead(),
                    tokens.cacheWrite(),
                    estimateCost(tokens),
                    null
            );

        } catch (Exception ex) {
            return new AiClassification(
                    null,
                    null,
                    ResultStatus.INVALID_RESPONSE,
                    raw,
                    latencyMs,
                    tokens.input(),
                    tokens.output(),
                    tokens.total(),
                    tokens.cacheRead(),
                    tokens.cacheWrite(),
                    estimateCost(tokens),
                    "JSON inválido: " + ex.getMessage()
            );
        }
    }

    private AiClassification error(
            long started,
            Exception ex
    ) {
        return new AiClassification(
                null,
                null,
                ResultStatus.API_ERROR,
                null,
                elapsedMs(started),
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                ex.getClass().getSimpleName()
                        + ": "
                        + ex.getMessage()
        );
    }

    private TokenUsage tokenUsage(
            ChatResponse response
    ) {
        if (response == null
                || response.getMetadata() == null) {

            return TokenUsage.EMPTY;
        }

        Usage usage = response.getMetadata().getUsage();

        if (usage == null) {
            return TokenUsage.EMPTY;
        }

        int input = value(usage.getPromptTokens());
        int output = value(usage.getCompletionTokens());
        int total = value(usage.getTotalTokens());

        int cacheRead = cacheReadTokens(usage);
        int cacheWrite = cacheWriteTokens(usage);

        if (provider == AiProvider.CLAUDE) {
            total = input + output + cacheRead + cacheWrite;
        }

        return new TokenUsage(
                input,
                output,
                total,
                cacheRead,
                cacheWrite
        );
    }

    /**
     * Obtiene tokens leídos desde caché.
     *
     * Primero intenta utilizar la API estándar de Spring AI.
     * Si Spring AI 1.1.8 no expone directamente el dato,
     * consulta la información nativa de OpenAI.
     */
    private int cacheReadTokens(
            Usage usage
    ) {
        int standardizedValue = reflectInt(
                usage,
                "getCacheReadInputTokens"
        );

        if (standardizedValue > 0) {
            return standardizedValue;
        }

        Object nativeUsage = usage.getNativeUsage();

        if (nativeUsage instanceof AnthropicApi.Usage anthropicUsage
                && anthropicUsage.cacheReadInputTokens() != null) {

            return anthropicUsage.cacheReadInputTokens();
        }

        if (nativeUsage instanceof OpenAiApi.Usage openAiUsage) {
            var promptDetails =
                    openAiUsage.promptTokensDetails();

            if (promptDetails != null
                    && promptDetails.cachedTokens() != null) {

                return promptDetails.cachedTokens();
            }
        }

        return 0;
    }

    /**
     * Obtiene tokens escritos en caché cuando el proveedor
     * y la versión de Spring AI reportan este valor.
     */
    private int cacheWriteTokens(
            Usage usage
    ) {
        int standardizedValue = reflectInt(
                usage,
                "getCacheWriteInputTokens"
        );

        if (standardizedValue > 0) {
            return standardizedValue;
        }

        Object nativeUsage = usage.getNativeUsage();

        if (nativeUsage == null) {
            return 0;
        }

        if (nativeUsage instanceof AnthropicApi.Usage anthropicUsage
                && anthropicUsage.cacheCreationInputTokens() != null) {

            return anthropicUsage.cacheCreationInputTokens();
        }

        int nativeValue = reflectInt(
                nativeUsage,
                "cacheWriteTokens"
        );

        if (nativeValue > 0) {
            return nativeValue;
        }

        return reflectInt(
                nativeUsage,
                "cacheWriteInputTokens"
        );
    }

    private int reflectInt(
            Object target,
            String methodName
    ) {
        if (target == null) {
            return 0;
        }

        try {
            Method method = target.getClass()
                    .getMethod(methodName);

            return value(method.invoke(target));

        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    private int value(Object value) {
        return value instanceof Number number
                ? number.intValue()
                : 0;
    }

    private BigDecimal estimateCost(
            TokenUsage usage
    ) {
        int ordinaryInput = provider == AiProvider.CLAUDE
                ? usage.input()
                : Math.max(
                        0,
                        usage.input()
                                - usage.cacheRead()
                                - usage.cacheWrite()
                );

        return inputPrice
                .multiply(BigDecimal.valueOf(ordinaryInput))

                .add(cachedInputPrice.multiply(
                        BigDecimal.valueOf(
                                usage.cacheRead()
                        )
                ))

                .add(cacheWritePrice.multiply(
                        BigDecimal.valueOf(
                                usage.cacheWrite()
                        )
                ))

                .add(outputPrice.multiply(
                        BigDecimal.valueOf(
                                usage.output()
                        )
                ))

                .divide(
                        MILLION,
                        8,
                        RoundingMode.HALF_UP
                );
    }

    private String cleanJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "respuesta vacía"
            );
        }

        String cleaned = raw.trim();

        if (cleaned.startsWith("```")) {
            cleaned = cleaned
                    .replaceFirst(
                            "^```(?:json)?\\s*",
                            ""
                    )
                    .replaceFirst(
                            "\\s*```$",
                            ""
                    );
        }

        int first = cleaned.indexOf('{');
        int last = cleaned.lastIndexOf('}');

        if (first < 0 || last < first) {
            throw new IllegalArgumentException(
                    "no se encontró un objeto JSON"
            );
        }

        return cleaned.substring(first, last + 1);
    }

    private int intField(
            JsonNode json,
            String preferred,
            String compatibleAlias
    ) {
        if (json.has(preferred)) {
            return json.path(preferred).asInt(-1);
        }

        return json.path(compatibleAlias).asInt(-1);
    }

    private long elapsedMs(long started) {
        return Math.max(
                0,
                (System.nanoTime() - started) / 1_000_000
        );
    }

    private static BigDecimal nonNull(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private record TokenUsage(
            int input,
            int output,
            int total,
            int cacheRead,
            int cacheWrite
    ) {
        private static final TokenUsage EMPTY =
                new TokenUsage(0, 0, 0, 0, 0);
    }
}