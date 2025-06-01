package tw.yukina.thinkorbit.service.shell;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tw.yukina.thinkorbit.configuration.ShellConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring implementation of ShellServerFactory
 */
@Slf4j
@Component
public class ShellServerFactoryImpl implements ShellServerFactory {
    
    private final ShellConfiguration defaultConfiguration;
    private final ShellService shellService;
    private final Map<String, ShellServer> servers = new ConcurrentHashMap<>();
    
    public ShellServerFactoryImpl(ShellConfiguration defaultConfiguration, ShellService shellService) {
        this.defaultConfiguration = defaultConfiguration;
        this.shellService = shellService;
    }
    
    @Override
    public ShellServer createServer(String name) {
        ShellServerProperties properties = ShellServerProperties.fromConfiguration(
                defaultConfiguration.getHost(),
                defaultConfiguration.getPort(),
                defaultConfiguration.getUsername(),
                defaultConfiguration.getPassword()
        );
        return createServer(name, properties);
    }
    
    @Override
    public ShellServer createServer(String name, ShellServerProperties config) {
        if (servers.containsKey(name)) {
            log.warn("Server with name [{}] already exists, returning existing instance", name);
            return servers.get(name);
        }
        
        log.info("Creating new ShellServer [{}] with configuration: host={}, port={}", 
                name, config.getHost(), config.getPort());
        
        ShellServer server = new ShellServer(name, config, shellService::handleShellSession);
        servers.put(name, server);
        
        return server;
    }
    
    /**
     * Get the server with the specified name
     */
    public ShellServer getServer(String name) {
        return servers.get(name);
    }
    
    /**
     * Get all servers
     */
    public Map<String, ShellServer> getAllServers() {
        return new ConcurrentHashMap<>(servers);
    }
    
    /**
     * Remove the specified server
     */
    public void removeServer(String name) {
        ShellServer server = servers.remove(name);
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
    
    /**
     * Stop and remove all servers
     */
    public void shutdown() {
        log.info("Shutting down all shell servers...");
        servers.values().forEach(server -> {
            if (server.isRunning()) {
                server.stop();
            }
        });
        servers.clear();
    }
} 