package tw.yukina.thinkorbit.service.command;

import java.lang.annotation.*;

/**
 * Command annotation for marking shell commands.
 * Can be applied to classes or methods.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Command {
    /**
     * Command name
     * @return the name of the command
     */
    String value();
    
    /**
     * Command description
     * @return the description of the command
     */
    String description() default "";
    
    /**
     * Command aliases
     * @return array of command aliases
     */
    String[] aliases() default {};
    
    /**
     * Whether this command is interactive
     * @return true if the command handles its own input loop
     */
    boolean interactive() default false;
} 