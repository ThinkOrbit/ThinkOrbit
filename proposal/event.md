# Event

## 1. 定位與目標（Positioning and Purpose）

EventBus 是整個系統中唯一可信任的語意資料交換層。它並非 log 系統，也非純粹的訊息佇列（queue），而是一個具備語意封包能力的廣播式事件總線。其主要任務是為所有模組提供一個中立、結構化、可觀察的資訊通道，並確保模組間僅透過事件進行通訊與協作。

EventBus 的設計目標如下：

1. **觀察性與重放能力（Observability and Replayability）**
   所有事件皆為不可變（immutable）封包，需具備結構化格式與儲存能力，支持後續分析、除錯與行為重現（replay）。

2. **模組解耦（Loose Coupling）**
   模組不得查詢彼此狀態。所有互動應僅透過事件進行，EventBus 應能提供足夠語意支持，讓模組能獨立推論與反應。

3. **語意傳遞（Semantic Exchange）**
   每筆事件皆需明確表示其來源、語意層級與意圖，避免低階技術訊號（如 CRUD 操作）混淆高階語意（如「使用者切換任務」、「番茄鐘結束」）。

4. **行為封裝與責任清晰（Encapsulation and Clarity）**
   每筆事件必須代表某個模組對外的語意主張。模組不得為他人發言，亦不得產生模糊不明的訊號（如「檢查一下」）。

5. **支援語意層級與來源區分（Tiered Semantics and Provenance）**
   事件需標示 semantic tier（internal / peripheral / ambient）與來源模組（source），以支援推論模組如 ContextEngine 區分行為強度與關聯性。

## 2. 設計原則（Design Principles）

EventBus 並非僅為模組通訊之用，它構成整個系統中語意資料流的唯一骨幹。為了支撐一個可觀察、可重建、具語意一致性的系統架構，其設計必須從封包原則、推論模型、模組邊界等層面建立強約束。以下為 EventBus 所遵循的設計原則與其背後理念。

---

### 2.1 不可變封包（Immutable Capsule）

事件是系統的最小語意單元。一旦發送，事件應視為不可變封包，**不得修改、延遲注入或覆寫語意**。此原則確保事件能夠安全地：

* 作為 **行為發生的唯一證據**
* 被觀察、記錄與重播（replayable）
* 作為行為推論與除錯的可信依據

不可變設計也讓所有事件邏輯具有可重演性與可測性，避免邊界模糊的動態資料查詢與修改。

---

### 2.2 單向資料流（Unidirectional Flow）

EventBus 僅支援單向廣播：**模組發送事件 → EventBus 廣播 → 訂閱模組接收並反應**。此模式使得資料流具有高度可觀察性與可追蹤性，並帶來三項效益：

* **解耦**：模組間不互相查詢，邏輯邊界明確
* **可測試**：模組僅依事件輸入決定行為，易於 isolation 測試
* **可重播**：系統可於任何時刻重演事件流並驗證行為一致性

資料流方向不可逆轉，是避免互 call、維持事件封包純度的關鍵設計。

---

### 2.3 語意為核心（Semantics First）

事件格式與內容設計應以語意為中心，而非資料結構為中心。也就是說，事件應該傳達「這件事發生了，為什麼重要」，而不只是「某欄位變成了 X」。

例如，`task_started` 應同時包含任務名稱、來源、預估時間，而非僅記錄 ID。如此一來：

* 任何模組皆可從單一事件獲得完整語意，不需查詢他人狀態
* 可支援後續通知策略、推論模型、CLI 呈現與快照生成
* 有利於未來 replay、摘要、語意壓縮（semantic compaction）

此原則讓事件成為**語意封裝單位**，而不只是同步狀態變更的機制。

---

### 2.4 語意層級與來源可見（Semantic Tier and Source Transparency）

每筆事件需附帶其語意層級（如 internal / peripheral / ambient）與來源模組（如 `TaskService`, `GitHubAdapter`）。這些資訊雖不影響邏輯處理，卻為推論模組提供必要上下文，達成下列目標：

* **ContextEngine 可根據層級與來源強度判斷干擾性**
* **InboxEvaluator 可依語意遠近決定是否轉入待辦清單**
* **Observer 模組可基於來源與語意構建使用者行為摘要**

這些欄位屬於系統級語意，是事件可理解性與語意行為策略形成的基礎。

---

### 2.5 可觀察與可重播（Observable and Replayable）

EventBus 並非即發即忘的觸發器，而是一個**語意封包的資料管線**。系統中所有模組皆應能透過 EventBus：

* **觀察事件流進行推論與狀態建構**
* **在系統初始化階段重播過去事件序列（Replay）以重建內部快照**
* **支援除錯與問題重演，理解「當初為何做出這個決策」**

這種設計不僅提供系統觀察能力，也為未來引入 LLM Agent、學習模型或語意統計機制提供資料基礎。

---

### 2.6 執行階段分明（Phased Execution）

模組在 replay、dry-run、或 bootstrap 階段應禁止產生任何副作用（例如發送通知、更新 CLI 輸出）。為此，EventBus 必須提供：

* **事件上下文（EventContext）**，明確標示當前執行階段
* **模組需區分純邏輯更新與副作用觸發兩種階段行為**

這樣的階段設計可防止 replay 時誤發提醒、污染觀察記錄、或干擾使用者操作，確保 replay 與 live 行為完全分離。

收到，我已根據你的需求整合以下內容，並撰寫為第 3 章的正式草稿。內容涵蓋結構定義、語意策略、欄位設計理由，語氣符合提案書格式：

---

## 3. 事件格式（Event Envelope Specification）

EventBus 的每一筆事件皆視為語意封包（capsule），需具備明確的結構與語意邊界。事件格式應同時支援模組推論、重播、觀察分析與除錯用途，並維持語意一致性與可序列化性。以下為統一事件封包格式之欄位定義與設計原則。

---

### 3.1 結構定義（Java）

事件封包採不變資料物件設計（immutable record），以 Java 表示如下：

```java
public record EventEnvelope(
    long id,
    Instant occurredAt,
    String type,
    String source,
    SemanticTier tier,
    String traceId,
    String causeId,
    Map<String, Object> payload
) {}
```

---

### 3.2 欄位說明

| 欄位名稱         | 類型                    | 說明                                                       |
| ------------ | --------------------- | -------------------------------------------------------- |
| `id`         | `long`                | 全域唯一事件序號，由 EventStore 生成，自增不可變                           |
| `occurredAt` | `Instant`             | 事件實際發生時間，用於排序與時間分析。Replay 時此欄位不變                         |
| `type`       | `String`              | 事件類型名稱（如 `task_started`, `pomodoro_finished`），為判斷語意與訂閱依據 |
| `source`     | `String`              | 發出事件的模組識別名稱（如 `TaskService`, `GitHubAdapter`）            |
| `tier`       | `SemanticTier`（enum）  | 語意層級，定義事件與使用者行為的語意距離（詳見 3.3）                             |
| `traceId`    | `String`              | 所屬行為鏈的追蹤識別碼，用於觀察整體使用者互動流程（如來自同一 Intent）                  |
| `causeId`    | `String`              | 觸發此事件的前一筆事件 ID，作為因果鏈索引依據（nullable）                       |
| `payload`    | `Map<String, Object>` | 語意資料封包，應具備足夠內容供他模組理解與反應。所有物件皆必須可序列化（例如 JSON 序列化）         |

---

### 3.3 語意層級：`SemanticTier`

`SemanticTier` 用於幫助模組（尤其是 ContextEngine）判斷事件的認知距離與反應強度。此欄位由事件來源模組於事件產生時給定，不可變更，並限定於以下三種語意層級：

| 層級名稱         | 意義說明                                     |
| ------------ | ---------------------------------------- |
| `INTERNAL`   | 使用者主動觸發的直接行為（如開始任務、快速記錄、計時開始）            |
| `PERIPHERAL` | 與使用者當前任務上下文有關，但未必需要立即反應（如 PR 被分派、日曆事件開始） |
| `AMBIENT`    | 系統背景訊號，非即時導向任務，但可作為語意脈絡（如 RSS 更新、氣象提醒）   |

> 層級定義由 EventBus 規格控管，模組不得自定其他層級；此欄位不應用於衰減推論計算。

---

### 3.4 語意推論資料不應嵌入事件封包

為保持事件封包的穩定性與語意純度，以下資料**不應**直接包含於事件中：

* `relevance`（與當前 context 之關聯強度）
* `decayAt`（推論用時間衰減閾值）
* `importanceScore`（使用者偏好分數）

這些值應由接收模組（如 ContextEngine）根據事件序列與系統狀態推論產生，不可由來源模組硬編碼。此設計確保：

* 事件封包不隱含推論意圖
* 推論策略可獨立測試與替換
* Replay 與分析不受當初來源判斷所綁定

---

### 3.5 Payload 設計原則

事件 `payload` 應視為語意封裝資料，其設計應符合下列原則：

1. **資料充分**：應包含所有讓訂閱模組能判斷行為的必要資訊，避免後續查詢他人狀態（如任務標題、預估時間、來源人員等）
2. **語意導向**：欄位命名與結構應反映人類行為語意，而非低階技術欄位（避免使用 `status = 1`、`flag = true` 類結構）
3. **可序列化**：所有物件皆應可轉為 JSON / BSON / FlatBuffer 等格式，支援儲存、傳輸與分析

> 模組不得將不可序列化的物件（如 database entity、Spring bean）嵌入 payload 中。

---

### 3.6 實例

範例事件（`task_started`）封包如下：

```json
{
  "id": 1024,
  "occurredAt": "2025-05-26T09:30:12Z",
  "type": "task_started",
  "source": "TaskService",
  "tier": "INTERNAL",
  "traceId": "t-91f2",
  "causeId": "1023",
  "payload": {
    "taskId": "abc123",
    "title": "撰寫提案書",
    "estimatedMinutes": 25,
    "tags": ["writing", "draft"]
  }
}
```

---


## 4. API 設計（Java Interface）

EventBus 作為系統內部唯一的語意封包交換通道，其 API 設計需同時滿足三項核心要求：

1. **語意清晰**：每筆事件皆代表發送模組的語意主張，不應為資料同步手段。
2. **角色明確**：模組之間僅能觀察事件，不得互相查詢或操控狀態。
3. **可觀察與可重播**：事件為不可變封包，需支援紀錄、查詢與重播，以實現行為還原與除錯能力。

---

### 4.1 EventBus 介面定義

```java
public interface EventBus {
    /**
     * Publishes an event to the event bus.
     *
     * @param event the event to publish
     */
    void publish(EventEnvelope event);

    /**
     * Subscribes a listener to events matching the specified filter.
     *
     * @param filter   the filter to match events
     * @param listener the listener to notify when events match
     */
    void subscribe(EventFilter filter, EventListener listener);

    /**
     * Replays events within a specified range to a listener.
     *
     * @param range    the range of events to replay
     * @param listener the listener to receive the replayed events
     */
    void replay(ReplayRange range, EventListener listener);

    /**
     * Closes the event bus and releases resources.
     */
    void close();
}
```

---

### 4.2 事件處理相關介面

```java
public interface EventListener {
    void onEvent(EventContext context, EventEnvelope event);
}

public interface EventFilter {
    /**
     * Checks if the event matches the filter criteria.
     *
     * @param event the event to check
     * @return true if the event matches, false otherwise
     */
    boolean matches(EventEnvelope event);
}
```

---

### 4.3 重播邊界與層級控制

```java
public record ReplayRange(
    Optional<Long> fromEventId,
    Optional<Instant> fromTime,
    ReplayLevel level
) {}
```

```java
public enum ReplayLevel {
    SNAPSHOT_ONLY,
    SEMANTIC_REPLAY,
    FULL_SIMULATION
}
```

#### Replay 行為區分：

| 層級                | 行為描述                 |
| ----------------- | -------------------- |
| `SNAPSHOT_ONLY`   | 僅更新模組快照，不執行語意推論與副作用  |
| `SEMANTIC_REPLAY` | 執行模組邏輯，產生語意推論，但禁止副作用 |
| `FULL_SIMULATION` | 全面重播語意與副作用，僅用於測試與分析  |

---

### 4.4 EventContext：語境辨識器

```java
public interface EventContext {
    boolean isReplay();         // 是否來自 replay 操作
    boolean isDryRun();         // 是否為 dry-run 模式
    ReplayLevel replayLevel();  // 為 null 時代表非 replay 模式
}
```

此接口為模組判斷當前執行語境的標準方式。模組可據此調整邏輯，例如 suppress 副作用、選擇性記錄、跳過 CLI 輸出等。應避免以非結構性標記（如 phase 字串）實作語境判斷。

---

### 4.5 使用範式示意

```java
eventBus.subscribe(
    e -> e.type().equals("task_started"),
    (ctx, evt) -> {
        taskService.apply(evt);
        if (!ctx.isReplay()) {
            taskService.react(evt);
        }
    }
);

eventBus.replay(
    new ReplayRange(
        Optional.of(lastHandledId),
        Optional.empty(),
        ReplayLevel.SEMANTIC_REPLAY
    ),
    (ctx, evt) -> taskService.apply(evt)
);
```

---

### 4.6 注意事項與約束

| 條目                           | 原則說明                             |
| ---------------------------- | -------------------------------- |
| 每筆事件處理邏輯應具決定性（deterministic） | 同一事件輸入應對應唯一行為輸出                  |
| `EventContext` 必須明確控制副作用執行   | 所有 CLI / 通知等副作用僅允許在 live mode 執行 |
| 所有 replay 皆應避免產生新事件          | 重播期間不得改變事件序列或發布衍生事件              |
| 模組不得跨越事件封包以存取其他模組狀態          | 禁止在事件處理中進行任何查詢或 service 呼叫       |

---
