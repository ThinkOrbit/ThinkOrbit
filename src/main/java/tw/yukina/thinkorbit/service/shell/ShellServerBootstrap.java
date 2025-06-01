package tw.yukina.thinkorbit.service.shell;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Shell Server Bootstrap
 * Automatically creates and starts the default Shell Server when the Spring application starts
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "shell.enabled", havingValue = "true", matchIfMissing = true)
public class ShellServerBootstrap {
    
    private final ShellServerFactory shellServerFactory;
    private static final String DEFAULT_SERVER_NAME = "default";
    
    public ShellServerBootstrap(ShellServerFactory shellServerFactory) {
        this.shellServerFactory = shellServerFactory;
    }
    
    @PostConstruct
    public void startDefaultServer() {
        log.info("Starting default shell server...");
        try {
            ShellServer server = shellServerFactory.createServer(DEFAULT_SERVER_NAME);
            server.start();
            log.info("Default shell server started successfully");
        } catch (Exception e) {
            log.error("Failed to start default shell server", e);
        }
    }
    
    @PreDestroy
    public void stopAllServers() {
        log.info("Stopping all shell servers...");
        if (shellServerFactory instanceof ShellServerFactoryImpl) {
            ((ShellServerFactoryImpl) shellServerFactory).shutdown();
        }
    }
} 