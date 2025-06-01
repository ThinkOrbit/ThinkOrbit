# Command System

This package provides an automatic command registration system for the shell service.

## Features

- Automatic command discovery and registration using annotations
- Support for both class-level and method-level commands
- Command aliases support
- Built-in help system
- Auto-completion for registered commands
- **Interactive command mode support** (NEW)

## Package Structure

```
command/
├── Command.java                  # Command annotation
├── CommandHandler.java           # Interface for class-level commands
├── InteractiveCommandHandler.java # Interface for interactive commands (NEW)
├── CommandRegistry.java          # Command registration and execution
├── CommandScanner.java           # Automatic command discovery
└── impl/                         # Command implementations (put your commands here)
    ├── HelpCommand.java
    ├── SystemCommands.java
    └── InteractiveCalculatorCommand.java # Example interactive command
```

**Important**: All command implementations should be placed in the `impl` subpackage or other subpackages to avoid circular dependencies with the command system components.

## Usage

### Creating a Class-Level Command

```java
package tw.yukina.thinkorbit.command.impl;  // Note: Use impl subpackage

@Component
@Command(value = "mycommand", description = "My custom command", aliases = {"mc"})
public class MyCommand implements CommandHandler {
    
    @Override
    public boolean execute(String[] args, Terminal terminal) {
        terminal.writer().println("Executing my command with args: " + Arrays.toString(args));
        return true;
    }
}
```

### Creating Method-Level Commands

```java
@Component
public class MyService {
    
    @Command(value = "status", description = "Show service status")
    public boolean showStatus(String[] args, Terminal terminal) {
        terminal.writer().println("Service is running");
        return true;
    }
    
    @Command(value = "restart", description = "Restart service", aliases = {"r"})
    public boolean restartService(String[] args, Terminal terminal) {
        terminal.writer().println("Service restarted");
        return true;
    }
}
```

### Creating Interactive Commands (NEW)

Interactive commands allow for continuous interaction with the user until explicitly exited. To create an interactive command:

1. Implement the `InteractiveCommandHandler` interface
2. Set `interactive = true` in the `@Command` annotation
3. Implement the required methods for handling interactive input

Example:

```java
@Component
@Command(value = "calc", description = "Interactive calculator", aliases = {"calculator"}, interactive = true)
public class InteractiveCalculatorCommand implements InteractiveCommandHandler {
    
    private boolean shouldExit = false;
    
    @Override
    public boolean execute(String[] args, Terminal terminal) {
        // Initialize the interactive session
        shouldExit = false;
        return true;
    }
    
    @Override
    public void handleInput(String input, Terminal terminal) {
        // Handle each line of user input
        if (input.equalsIgnoreCase("exit")) {
            shouldExit = true;
            return;
        }
        // Process the input...
        terminal.writer().println("You entered: " + input);
    }
    
    @Override
    public boolean shouldExit() {
        return shouldExit;
    }
    
    @Override
    public void onEnterInteractive(Terminal terminal) {
        terminal.writer().println("Entering interactive mode. Type 'exit' to quit.");
    }
    
    @Override
    public void onExitInteractive(Terminal terminal) {
        terminal.writer().println("Exiting interactive mode.");
    }
}
```

## Command Method Signature

### Standard Commands
Method-level commands must have the following signature:
```java
public boolean methodName(String[] args, Terminal terminal)
```

### Interactive Commands
Interactive commands must implement the `InteractiveCommandHandler` interface, which includes:
- `execute(String[] args, Terminal terminal)` - Initialize the interactive session
- `handleInput(String input, Terminal terminal)` - Process each line of user input
- `shouldExit()` - Determine when to exit interactive mode
- `onEnterInteractive(Terminal terminal)` - Called when entering interactive mode (optional)
- `onExitInteractive(Terminal terminal)` - Called when exiting interactive mode (optional)

## Built-in Commands

- `help` (aliases: `?`, `h`) - Display available commands
- `date` - Display current date and time
- `memory` (alias: `mem`) - Display memory usage information
- `clear` (alias: `cls`) - Clear the terminal screen
- `echo` - Echo input text
- `exit` - Exit the shell session
- `calc` (alias: `calculator`) - Interactive calculator (example of interactive command)

## Interactive Mode Features

When a command is marked as interactive:
1. The command's `execute()` method is called first with any initial arguments
2. If it returns `true`, the system enters interactive mode
3. A dedicated input loop is created for the command
4. All user input is passed to the command's `handleInput()` method
5. The command remains in control until `shouldExit()` returns `true`
6. Users can exit using Ctrl+C or Ctrl+D as well

## Auto-completion

All registered commands are automatically added to the shell's auto-completion system. Press TAB to complete command names.

## Command Registration Process

1. During application startup, `CommandScanner` scans for:
   - Classes annotated with `@Command` that implement `CommandHandler` or `InteractiveCommandHandler` in the `impl` subpackage
   - Methods annotated with `@Command` in Spring beans (excluding command system components)
   
2. Found commands are registered in `CommandRegistry` with their interactive flag

3. `ShellService` uses `CommandRegistry` to execute commands, handling interactive mode when needed

## Adding New Commands

1. Create a new class in the `command/impl` package or create your own subpackage under `command`
2. Add `@Command` annotation with command name and description
3. For standard commands: implement `CommandHandler` interface
4. For interactive commands: implement `InteractiveCommandHandler` interface and set `interactive = true`
5. For method-level commands: add method with correct signature in a Spring bean
6. The command will be automatically discovered and registered on application startup 