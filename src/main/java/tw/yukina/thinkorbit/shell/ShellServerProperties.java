package tw.yukina.thinkorbit.shell;

import lombok.Builder;
import lombok.Data;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;

/**
 * ShellServer Properties
 */
@Data
@Builder
public class ShellServerProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    
    @Builder.Default
    private PasswordAuthenticator authenticator = (username, password, session) -> false;
    
    /**
     * From Spring Shell Configuration
     */
    public static ShellServerProperties fromConfiguration(String host, int port, String username, String password) {
        return ShellServerProperties.builder()
                .host(host)
                .port(port)
                .username(username)
                .password(password)
                .authenticator((user, pass, _) ->
                    username.equals(user) && password.equals(pass))
                .build();
    }
} 