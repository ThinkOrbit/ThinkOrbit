package tw.yukina.thinkorbit.shell;

/**
 * Factory 介面用於創建 ShellServer 實例
 */
public interface ShellServerFactory {
    
    /**
     * 使用預設配置創建 ShellServer
     * @param name Server 的名稱標識
     * @return ShellServer 實例
     */
    ShellServer createServer(String name);
    
    /**
     * 使用自定義配置創建 ShellServer
     * @param name Server 的名稱標識
     * @param config 自定義配置
     * @return ShellServer 實例
     */
    ShellServer createServer(String name, ShellServerProperties config);
} 