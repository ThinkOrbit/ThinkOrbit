package tw.yukina.thinkorbit.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "shell")
public class ShellConfiguration {
    private boolean enabled = true;
    private String host = "127.0.0.1";
    private int port = 2222;
    private String username = "admin";
    private String password = "password";
}