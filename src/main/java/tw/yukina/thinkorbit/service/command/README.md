# Command System

This package provides an automatic command registration system for the shell service.

## Features

- Automatic command discovery and registration using annotations
- Support for both class-level and method-level commands
- Command aliases support
- Built-in help system
- Auto-completion for registered commands

## Package Structure

```
command/
├── Command.java            # Command annotation
├── CommandHandler.java     # Interface for class-level commands
├── CommandRegistry.java    # Command registration and execution
├── CommandScanner.java     # Automatic command discovery
└── impl/                   # Command implementations (put your commands here)
    ├── HelpCommand.java
    └── SystemCommands.java
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

## Command Method Signature

Method-level commands must have the following signature:
```java
public boolean methodName(String[] args, Terminal terminal)
```

- `args`: Command arguments (excluding the command name itself)
- `terminal`: JLine terminal for input/output operations
- Return value: `true` if command executed successfully, `false` otherwise

## Built-in Commands

- `help` (aliases: `?`, `h`) - Display available commands
- `date` - Display current date and time
- `memory` (alias: `mem`) - Display memory usage information
- `clear` (alias: `cls`) - Clear the terminal screen
- `echo` - Echo input text
- `exit` - Exit the shell session

## Auto-completion

All registered commands are automatically added to the shell's auto-completion system. Press TAB to complete command names.

## Command Registration Process

1. During application startup, `CommandScanner` scans for:
   - Classes annotated with `@Command` that implement `CommandHandler` in the `impl` subpackage
   - Methods annotated with `@Command` in Spring beans (excluding command system components)
   
2. Found commands are registered in `CommandRegistry`

3. `ShellService` uses `CommandRegistry` to execute commands

## Adding New Commands

1. Create a new class in the `command/impl` package or create your own subpackage under `command`
2. Add `@Command` annotation with command name and description
3. For class-level commands: implement `CommandHandler` interface
4. For method-level commands: add method with correct signature in a Spring bean
5. The command will be automatically discovered and registered on application startup 