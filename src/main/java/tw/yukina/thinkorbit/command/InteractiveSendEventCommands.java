package tw.yukina.thinkorbit.command;

import org.jline.terminal.Terminal;
import org.springframework.stereotype.Component;
import tw.yukina.thinkorbit.service.command.Command;
import tw.yukina.thinkorbit.service.command.InteractiveCommandHandler;
import tw.yukina.thinkorbit.service.event.EventBus;
import tw.yukina.thinkorbit.service.intent.Intent;
import tw.yukina.thinkorbit.service.intent.IntentRouteRegistry;
import tw.yukina.thinkorbit.service.task.intent.CreateTask;

/**
 * Interactive debug command for sending events
 */
@Component
@Command(value = "send-event", interactive = true)
public class InteractiveSendEventCommands implements InteractiveCommandHandler {

    private final EventBus eventBus;
    private final IntentRouteRegistry intentRouteRegistry;

    private boolean shouldExit = false;

    public InteractiveSendEventCommands(EventBus eventBus, IntentRouteRegistry intentRouteRegistry) {
        this.eventBus = eventBus;
        this.intentRouteRegistry = intentRouteRegistry;
    }

    @Override
    public boolean execute(String[] args, Terminal terminal) {
        // Initialize calculator
        shouldExit = false;
        return true;
    }

    @Override
    public void handleInput(String input, Terminal terminal) {
        if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
            shouldExit = true;
            terminal.writer().println("Exiting calculator mode.");
            return;
        }

        if (input.startsWith("add task ")) {
            CreateTask createTask = new CreateTask(input.substring(9).trim());
            Intent<CreateTask> intent = new Intent<>(createTask);
            intentRouteRegistry.executeIntent(intent);
        } else {
            terminal.writer().println("Unknown command: " + input);
        }
    }

    @Override
    public boolean shouldExit() {
        return shouldExit;
    }

    @Override
    public void onEnterInteractive(Terminal terminal) {
        terminal.writer().println("Entering interactive mode.");
    }

    @Override
    public void onExitInteractive(Terminal terminal) {
        terminal.writer().println("Exiting calculator mode.");
    }
} 