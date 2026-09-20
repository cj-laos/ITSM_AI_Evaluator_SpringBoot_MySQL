package pe.losmatus.itsmevaluator.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import pe.losmatus.itsmevaluator.config.AppProperties;

@Component
public class PromptCatalog {
    private final String systemPrompt;
    private final String version;

    public PromptCatalog(ResourceLoader resourceLoader, AppProperties properties) throws IOException {
        this.version = properties.prompt().version();
        var resource = resourceLoader.getResource(properties.prompt().resource());
        this.systemPrompt = resource.getContentAsString(StandardCharsets.UTF_8);
    }

    public String systemPrompt() { return systemPrompt; }
    public String version() { return version; }

    public String userPrompt(String description) {
        return "DESCRIPCIÓN DEL TICKET:\n" + description;
    }
}
