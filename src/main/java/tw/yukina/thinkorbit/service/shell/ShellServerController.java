package tw.yukina.thinkorbit.service.shell;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for managing Shell Server instances
 * This is an example demonstrating how to use the Factory pattern to dynamically create and manage multiple servers
 */
@Slf4j
@RestController
@RequestMapping("/api/shell-servers")
public class ShellServerController {
    
    private final ShellServerFactoryImpl shellServerFactory;
    
    public ShellServerController(ShellServerFactoryImpl shellServerFactory) {
        this.shellServerFactory = shellServerFactory;
    }
    
    /**
     * Create a new Shell Server
     */
    @PostMapping
    public ResponseEntity<String> createServer(
            @RequestParam String name,
            @RequestParam(defaultValue = "127.0.0.1") String host,
            @RequestParam(defaultValue = "2223") int port,
            @RequestParam(defaultValue = "admin") String username,
            @RequestParam(defaultValue = "password") String password) {
        
        try {
            ShellServerProperties properties = ShellServerProperties.builder()
                    .host(host)
                    .port(port)
                    .username(username)
                    .password(password)
                    .authenticator((user, pass, session) -> 
                        username.equals(user) && password.equals(pass))
                    .build();
            
            ShellServer server = shellServerFactory.createServer(name, properties);
            server.start();
            
            return ResponseEntity.ok("Server " + name + " created and started on " + host + ":" + port);
        } catch (Exception e) {
            log.error("Failed to create server", e);
            return ResponseEntity.badRequest().body("Failed to create server: " + e.getMessage());
        }
    }
    
    /**
     * Get status of all servers
     */
    @GetMapping
    public ResponseEntity<Map<String, ServerStatus>> getAllServers() {
        Map<String, ServerStatus> status = shellServerFactory.getAllServers().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new ServerStatus(
                                entry.getValue().getName(),
                                entry.getValue().getProperties().getHost(),
                                entry.getValue().getProperties().getPort(),
                                entry.getValue().isRunning()
                        )
                ));
        return ResponseEntity.ok(status);
    }
    
    /**
     * Stop the specified server
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<String> stopServer(@PathVariable String name) {
        ShellServer server = shellServerFactory.getServer(name);
        if (server == null) {
            return ResponseEntity.notFound().build();
        }
        
        server.stop();
        shellServerFactory.removeServer(name);
        return ResponseEntity.ok("Server " + name + " stopped and removed");
    }
    
    /**
     * Server status information
     */
    public record ServerStatus(String name, String host, int port, boolean running) {}
} 