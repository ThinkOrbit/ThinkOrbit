package tw.yukina.thinkorbit.service.shell;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jline.builtins.ssh.Ssh;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * SSH Shell Server Instance
 */
@Slf4j
@Getter
public class ShellServer {
    private final String name;
    private final ShellServerProperties properties;
    private final Consumer<Ssh.ShellParams> shellConsumer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private SshServer sshServer;
    private Thread serverThread;

    public ShellServer(String name, ShellServerProperties properties, Consumer<Ssh.ShellParams> shellConsumer) {
        this.name = name;
        this.properties = properties;
        this.shellConsumer = shellConsumer;
    }

    public synchronized void start() {
        if (running.get()) {
            log.warn("Shell server [{}] is already running", name);
            return;
        }

        try {
            log.info("Starting SSH server [{}] on {}:{}", name, properties.getHost(), properties.getPort());
            
            serverThread = new Thread(() -> {
                try {
                    sshServer = this.createSshServer();

                    Ssh ssh = new Ssh(
                            shellConsumer,
                            null,
                            () -> sshServer,
                            null);

                    ssh.sshd(System.out, System.err, new String[]{
                            "--ip=" + properties.getHost(),
                            "--port=" + properties.getPort(),
                            "start"
                    });

                    new CountDownLatch(1).await();

                } catch (Exception e) {
                    log.error("Failed to run SSH server [{}]", name, e);
                }
            }, "shell-server-" + name);

            serverThread.setDaemon(false);
            serverThread.start();
            running.set(true);

        } catch (Exception e) {
            log.error("Failed to start SSH server [{}]", name, e);
            throw new RuntimeException("Failed to start SSH server", e);
        }
    }

    public synchronized void stop() {
        if (!running.get()) {
            log.warn("Shell server [{}] is not running", name);
            return;
        }

        try {
            log.info("Stopping SSH server [{}]", name);
            
            if (sshServer != null) {
                sshServer.stop();
            }
            
            if (serverThread != null) {
                serverThread.interrupt();
                serverThread.join(5000);
            }
            
            running.set(false);
            log.info("SSH server [{}] stopped successfully", name);
            
        } catch (IOException | InterruptedException e) {
            log.error("Error stopping SSH server [{}]", name, e);
        }
    }

    /**
     * Check if the server is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }

    private SshServer createSshServer() {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPasswordAuthenticator(properties.getAuthenticator());
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        return sshServer;
    }
} 