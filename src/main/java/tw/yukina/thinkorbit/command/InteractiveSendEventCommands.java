package tw.yukina.thinkorbit.command;

import org.jline.terminal.Terminal;
import org.springframework.stereotype.Component;
import tw.yukina.thinkorbit.service.command.Command;
import tw.yukina.thinkorbit.service.command.InteractiveCommandHandler;
import tw.yukina.thinkorbit.service.event.EventBus;
import tw.yukina.thinkorbit.service.intent.Intent;
import tw.yukina.thinkorbit.service.intent.IntentRouteRegistry;
import tw.yukina.thinkorbit.service.task.Task;
import tw.yukina.thinkorbit.service.task.TaskService;
import tw.yukina.thinkorbit.service.task.intent.CreateTask;

import java.util.List;

/**
 * Interactive debug command for sending events
 */
@Component
@Command(value = "focus", interactive = true)
public class InteractiveSendEventCommands implements InteractiveCommandHandler {

    private final IntentRouteRegistry intentRouteRegistry;

    private final TaskService taskService;

    private boolean shouldExit = false;

    private List<Task> tasks = List.of();

    public InteractiveSendEventCommands(EventBus eventBus, IntentRouteRegistry intentRouteRegistry, TaskService taskService) {
        this.intentRouteRegistry = intentRouteRegistry;
        this.taskService = taskService;
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
            return;
        }

        if (input.startsWith("task add ")) {
            String taskName = input.substring("task add ".length()).trim();
            if (taskName.isEmpty()) {
                terminal.writer().println("Usage: task add <task_name>");
                return;
            }

            // Create a new task intent
            CreateTask createTask = new CreateTask(input.substring(9).trim());
            Intent<CreateTask> intent = new Intent<>(createTask);
            intentRouteRegistry.executeIntent(intent);
            terminal.writer().println("Task added: " + taskName);
        } else if (input.startsWith("task ls")) {
            // List all tasks
            terminal.writer().println("Current tasks:");
            tasks = taskService.getTasks();

            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                terminal.writer().println((i + 1) + ". " + task.getName() + " (ID: " + task.getId() + ")");
            }
        } else if (input.startsWith("start ")) {
            String taskNumber = input.substring("start ".length()).trim();
            if (taskNumber.isEmpty() || !taskNumber.matches("\\d+")) {
                terminal.writer().println("Usage: start <task_number>");
                return;
            }

            int index = Integer.parseInt(taskNumber) - 1;
            if (index < 0 || index >= tasks.size()) {
                terminal.writer().println("Invalid task number: " + taskNumber);
                return;
            }

            Task taskToStart = tasks.get(index);
            terminal.writer().println("Starting task: " + taskToStart.getName() + " (ID: " + taskToStart.getId() + ")");
        } else if (input.startsWith("task del ")) {
            String taskNumber = input.substring("task delete ".length()).trim();
            if (taskNumber.isEmpty() || !taskNumber.matches("\\d+")) {
                terminal.writer().println("Usage: task delete <task_number>");
            }

            int index = Integer.parseInt(taskNumber) - 1;
            if (index < 0 || index >= tasks.size()) {
                terminal.writer().println("Invalid task number: " + taskNumber);
                return;
            }

            Task taskToDelete = tasks.get(index);
            terminal.writer().println("Deleted task: " + taskToDelete.getName() + " (ID: " + taskToDelete.getId() + ")");
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
        terminal.writer().println("Exiting interactive mode.");
    }
}
