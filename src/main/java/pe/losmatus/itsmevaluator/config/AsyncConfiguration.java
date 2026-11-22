package pe.losmatus.itsmevaluator.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfiguration {

    @Bean(name = "evaluationExecutor")
    public Executor evaluationExecutor(AppProperties properties) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.execution().threadPoolSize());
        executor.setMaxPoolSize(properties.execution().threadPoolSize());
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("evaluation-");
        executor.initialize();
        return executor;
    }
}
