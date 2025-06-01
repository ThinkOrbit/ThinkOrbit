# Shell Server 架構說明

## 概述
此模組已重構為符合 Spring 慣例的 Factory 模式設計，支援動態創建和管理多個 SSH Shell Server 實例。

## 主要元件

### 1. ShellConfiguration
- Spring Boot 配置類別，使用 `@ConfigurationProperties`
- 定義預設的 Shell Server 配置
- 支援通過 `application.yml` 或 `application.properties` 進行配置

### 2. ShellServerProperties
- 封裝單個 Shell Server 實例的配置
- 使用 Lombok `@Builder` 模式
- 包含主機、端口、認證等設定

### 3. ShellServerFactory / ShellServerFactoryImpl
- Factory 介面和實現，用於創建 ShellServer 實例
- 管理所有 Server 實例的生命週期
- 支援使用預設配置或自定義配置創建 Server

### 4. ShellServer
- 代表單個 SSH Server 實例
- 專注於 Server 本身的功能（啟動、停止、狀態管理）
- 支援非同步運行和優雅關閉

### 5. ShellService
- 處理 Shell 會話的業務邏輯
- 與 Server 創建邏輯分離，專注於命令處理

### 6. ShellServerBootstrap
- Spring 啟動時自動創建預設 Server
- 可通過 `shell.enabled=false` 禁用自動啟動

### 7. ShellServerController（範例）
- 展示如何使用 Factory 動態管理多個 Server
- 提供 REST API 創建、查詢、停止 Server

## 使用範例

### 配置檔案 (application.yml)
```yaml
shell:
  enabled: true
  host: 127.0.0.1
  port: 2222
  username: admin
  password: password
```

### 程式碼使用
```java
// 注入 Factory
@Autowired
private ShellServerFactory factory;

// 創建自定義 Server
ShellServerProperties props = ShellServerProperties.builder()
    .host("0.0.0.0")
    .port(2223)
    .username("user")
    .password("pass")
    .build();

ShellServer server = factory.createServer("custom-server", props);
server.start();
```

## 優點
1. **符合 Spring 慣例**：使用依賴注入、配置類別、生命週期管理
2. **擴展性**：輕鬆創建多個 Server 實例而無需修改核心程式碼
3. **關注點分離**：Server 管理、會話處理、配置管理各司其職
4. **易於測試**：每個元件都可以獨立測試
5. **動態管理**：支援運行時創建和停止 Server 