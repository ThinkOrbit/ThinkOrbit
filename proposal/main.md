## 1. Introduction｜專案引言

本系統是一個以事件為基礎的**第二大腦系統核心**，設計目標是：協助使用者在多任務切換與頻繁中斷的情境中，減輕認知負擔，維持思緒連貫，並在需要時提供精確回饋。

第二大腦不應是一份更大的清單，而是一個**能觀察、理解、記憶你正在做什麼的中介層**，讓你能將更多心力專注於真正重要的行動上。它不取代人的意志，而是在注意力稀缺的情境下，幫助你不遺漏、不遺忘、不中斷。

系統的運作方式建立於事件流之上：來自使用者操作、系統變化與外部平台（如 GitHub PR、日曆、RSS）的訊號皆被轉換為語意化事件，並驅動推論、決策與提示流程。

初期版本實作四個核心模組：

1. **任務生命週期管理**：支援啟動、完成、與狀態追蹤，並可從事件還原歷程。
2. **時間追蹤與番茄鐘**：支援 start/stop 計時，並根據 context 調整提醒風格。
3. **外部訊號轉任務候選**：將 PR、Jira 等訊號轉為 Inbox 項目以待評估。
4. **ContextEngine**：推論當前認知狀態（如專注、整理、互動），引導下游模組調整資訊優先順序與回饋方式。

整體架構強調：

* 以事件為核心資料來源（event-capsule model）
* 各模組不共享狀態，只透過事件互動
* 系統不強制主導，而是根據語境給出恰當支持

這樣的設計讓本系統不只是一組工具，而是**一層協助你維持注意與記憶的認知結構**——始終在場，但從不干擾。

## 2. Cognitive Model｜核心認知模型

本系統以中樞神經結構為比擬模型，劃分為四層：

| 層級                 | 對應角色          | 系統行為範疇                              |
| ------------------ | ------------- | ----------------------------------- |
| 感知（Perception）     | 事件流           | 接收 CLI 指令、外部訊號（PR、日曆）、內部操作          |
| 解釋（Interpretation） | 語意推論器         | 推論 context，計算 relevance，建立 fuzzy 向量 |
| 行動（Action）         | 行為策略          | 啟動計時、變更任務狀態、產生提示與快照                 |
| 調節（Modulation）     | meta-state 設定 | 依時間、目標、疲勞等調整 context 切換閾值與輸出風格（可選）  |

---

### 一切皆事件，Task 與 Context 皆由其驅動

**Event** 是本系統的最小事實單位，所有行為（task 操作、RSS 更新、PR 指派、quick log 等）皆以事件形式進入系統。事件具備以下欄位：

* `timestamp`：事件發生時間
* `type`：如 `task_started`, `rss_article_posted`
* `source`：如 `TaskService`, `GitHubService`
* `payload`：包含 task ID、文章標題等參數
* `semantic_tier`：事件語意層級，如 `internal`, `peripheral`, `ambient`

所有模組之間只透過事件流通訊，不共享 mutable state，模組責任也劃分為「觀察」與「決策」兩層。

---

### Context 是語意鏡頭，不是控制器

**Context** 是對事件流的語意推論結果，用於決定當前系統應採取的行為策略，例如應優先顯示哪些 task、是否壓制提醒、是否進行回顧等。

context 為 fuzzy 向量（如 `focus: 0.6`, `review: 0.3`），不保證唯一主體。系統透過 `ContextScorer` 根據事件趨勢生成 context vector，再由 `TransitionPolicy` 判斷是否進行切換。

---

### Inbox 是雜亂入口，不是任務池

Inbox 並不是任務的子集，而是系統對**認知模糊區**的模擬。它接收所有 relevance 尚未明確的事件候選（特別是 peripheral 與 ambient tier 事件），例如：

* GitHub PR 被指派
* RSS 發布新文章
* Jira 產生待處理項目
* 使用者 quick log 留下一句訊息

這些項目不一定會變成 task，也不一定重要，但它們**有潛在的注意力價值**。因此：

* Inbox **理應雜亂**，這是系統對不確定訊號的保留
* 清理 Inbox 是使用者在 `review` context 中的認知工作
* 系統應依 context 與 relevance 自動排序 / 摺疊 / 聚合 Inbox 項目

Inbox 是工作記憶，也是任務形成之前的過渡區。

---

### Inbox 決策與資料分層

為實現 decoupling，系統中會存在兩個不同職責的元件：

| 元件               | 職責                                    |
| ---------------- | ------------------------------------- |
| `RSSObserver`    | 產生 `rss_article_posted` 事件，不進行判斷      |
| `InboxEvaluator` | 訂閱事件流，決定是否產生 `inbox_entry_created` 事件 |

這種設計保留：

* 單純觀察者（只產生事件）
* 決策模組（基於 context / relevance 判斷是否轉入 Inbox）
* 日後可插入 LLM 或 rule engine 決定是否接收某類事件

---

### context 影響所有模組，但不直接下命令

Context 變化只產生 `ContextChangedEvent`，所有模組應以此作為觀察依據調整策略，而不是由 context 主動控制系統。

例如：

* Inbox 模組可依 context 調整排序與顯示樣式（`focus` 隱藏 ambient tier，`review` 全展開）
* Timer 模組在 `focus` 中啟用番茄鐘結束提醒，在 `meeting` 中則靜音處理
* CLI 可根據 context 提示常用指令（`review` 顯示 inbox 清理指令）

這樣設計確保系統有節奏感但不強制，保留使用者 override 與自訂空間。

## 3. Module Architecture｜模組與責任劃分

本系統的模組架構建立於事件流基礎之上。每個模組皆應：

* 透過 `EventBus` 接收事件
* 發送事件作為輸出
* 保持邏輯封閉，不操作他人狀態

模組不應共享 mutable state，唯一可觀察實體為 append-only 事件流。以下列出核心模組與其責任範圍。

---

### 3.1 `TaskService`：任務狀態快照管理

* 管理任務的目前狀態（started, completed, scheduled 等）
* 接收 `task_created`, `task_started`, `task_completed` 等事件
* 發送狀態轉移事件（如 `task_started`）
* 提供查詢與快照更新（但不主動推論）

任務不為事件主體，而是事件驅動後產生的快照。

---

### 3.2 `ContextEngine`：語意推論中介層

* 接收所有事件（無篩選）
* 保留滑動視窗事件序列（如過去 30 分鐘）
* 推論 fuzzy context vector
* 依切換政策判斷是否更新主 context，發出 `ContextChangedEvent`

此模組不可發出通知、執行操作、或主動控制任務，只能影響其他模組的解讀與策略。

---

### 3.3 `EventBus`：統一事件交換中心

* 所有模組事件皆經由此處廣播
* 保留完整事件紀錄以供重播與分析（可選）
* 提供 event hook 給外部觀察模組（如觀測器、摘要引擎、通知系統）

事件流為系統唯一事實來源，資料庫僅為快照存儲層。

---

### 3.4 `InboxService`：尚未轉化任務的候選記憶體

* 訂閱 event stream（尤其是 peripheral / ambient tier）
* 根據事件 relevance、context、來源等條件判斷是否轉入 Inbox
* 發送 `inbox_entry_created`, `inbox_entry_archived` 等事件
* 提供批次清理、分類、轉 Task 等指令支援

Inbox 是 transition layer，所有還不明確是否要成為任務的資訊都進入此區等待清理。

---

### 3.5 `InboxEvaluator`：模糊事件的意圖判斷器

* 專責評估非明確任務型事件（如 RSS、mention、pull request）
* 提供策略層：是否進 Inbox？如何加註初始 relevance？
* 支援多策略切換（規則、context-aware、模型推薦）
* 未持久化狀態，僅提供事件轉事件的中介邏輯

此模組可選擇性實作，允許外部註冊評估器擴充。

---

### 3.6 `TimerService`：時間追蹤與番茄鐘

* 接收 `task_started`、`timer_started` 等事件
* 發出 `pomodoro_finished`, `timer_interrupted` 等事件
* 可依 context 改變行為：如在 `meeting` 停用通知、在 `focus` 顯示預計結束時間
* 提供時間統計資料給 UI 模組或報表模組

本模組為 context-aware 的追蹤模組，應避免與任務狀態耦合。

---

### 3.7 `CLIInterface`：語意輸入與回饋組合器

* 將指令轉為 `Intent` → 映射為事件並發送（例如 `task start abc` → `task_started`）
* 訂閱 `FeedbackEvent` 類事件並組合文字回饋
* 根據 context 提供 command 建議與提示（autocomplete、上下文警示）

CLI 並不執行實際邏輯，只作為語意轉換與觀察輸出，並提供回饋表達層。

---

### 3.8 `ViewComposer`（可選）：組合可視化輸出（TUI/Web）

* 訂閱 `ContextChangedEvent`, `InboxEntryCreated`, `TimerUpdated` 等事件
* 組合出不同 context 下的界面（如聚焦模式、清理模式）
* 與 CLI 並存，屬於 passive view 組合模組

可後期實作，不屬於 CLI-only 最小可用核心。

---

### 3.9 `Logger / Observer` 模組（外掛式）

* 訂閱所有事件與 context 切換
* 可做記錄、簡報、生產力分析用
* 可封裝為 Plugin Interface，由使用者自行添加

非必要模組，但對於觀察使用者節奏與行為變化極有價值。

---

### 3.10 模組互動示意圖（簡述）

```plaintext
           ┌─────────────┐
           │ CLI / View  │
           └────┬────────┘
                ↓
          ┌───────────┐
          │ EventBus  │◄────────────┐
          └────┬──────┘             │
     ┌─────────▼──────────┐        │
     │   ContextEngine     │        │
     └────────┬────────────┘        │
              │                     │
     ┌────────▼────────┐     ┌──────▼──────┐
     │   TaskService    │     │ TimerService│
     └────────┬────────┘     └──────┬──────┘
              │                     │
     ┌────────▼────────┐     ┌──────▼────────┐
     │ InboxEvaluator  │────► InboxService   │
     └─────────────────┘     └───────────────┘
```

---

## 4. Interaction Model｜互動模型與 CLI 設計哲學

本系統採取 **Intent → Event → Feedback** 的語意導向互動流程。CLI 並不是命令解譯器，而是語意輸入與認知回饋的界面，承擔「語言轉事件」與「事件轉認知提示」的責任。

---

### 4.1 CLI 不是控制器，而是語意輸入層

CLI 的任務是將使用者輸入的字串轉為語意明確的 `Intent`：

```plaintext
> task start abc123
→ Intent("start_task", { task_id: "abc123" })
```

Intent 是不具語意狀態的「意圖描述」，不會直接改變系統狀態。其處理交由對應模組（如 TaskService）完成。

這種設計可保證：

* CLI 輕量、不擁有邏輯
* 同一 intent 可被自動化模組、script、schedule 觸發
* 系統可對 Intent 實施驗證、解模糊、路由等中介層控制

---

### 4.2 Event 為語意封包，CLI 為回饋訂閱者

模組接收 Intent → 發送語意事件（如 `task_started`）→ CLI 訂閱 EventBus 觀察這些事件，產生語意回饋：

```plaintext
Event: task_started
→ CLI 渲染：
✓ 你已開始任務「撰寫提案書」
✓ 番茄鐘開始，預計 25 分鐘後結束
```

這些語意模板應獨立於模組邏輯，並可依 context 調整語氣與資訊密度。

---

### 4.3 回饋事件（FeedbackEvent）與分層顯示

某些模組（如 ContextEngine、TimerService）會發送 `FeedbackEvent` 類事件，僅供 CLI 呈現，不進入核心狀態機。例如：

* `PomodoroAlmostDoneFeedback`（剩餘 3 分鐘）
* `TaskSuggestedFeedback`（來自 quick log 推論的建議任務）

這些回饋應可分級顯示，或依 context 決定是否略過、延遲或高亮。

---

### 4.4 語意 autocomplete 與 context-aware 指令建議

CLI 不應僅為字串比對 autocomplete，而應支援以下語意導引：

| 類型            | 說明                                                        |
| ------------- | --------------------------------------------------------- |
| context-aware | 在 `review` 顯示 `inbox clean`，在 `focus` 顯示 `timer stop`     |
| data-driven   | 顯示最近三筆任務、active PR、未轉入的 quick log                         |
| intent-aware  | 根據 partial input 推測可能意圖並補全參數：`task sta...` → `task start` |

---

### 4.5 CLI 為語意入口，非狀態查詢器

CLI 不應主動查詢模組內部狀態（例如任務快照、context 分數），而是：

* 發送指令 → 等待事件 → 根據事件決定要不要呈現資訊
* 所有狀態皆應由事件間接反映，例如：

  * `ContextChangedEvent`
  * `InboxEntryCreated`
  * `TimerTicked`

如果需要查詢最新 context / active task，應由 TaskService 或 ContextEngine 以事件形式主動 broadcast，CLI 僅維護最後一筆快照。

---

### 4.6 CLI 應具備 replay 與實驗能力

為配合事件封包架構，CLI 應支援：

* `intent replay`：重放過去一次指令的 Intent
* `event watch`：以 raw 模式顯示所有接收到的事件（供除錯）
* `context why`：顯示最近 context 切換的觸發事件與推論紀錄

這讓使用者可觀察系統行為，並強化信任與除錯能力。

---

### 小結

| 元件            | 角色                        |
| ------------- | ------------------------- |
| CLI           | 語意輸入轉換器 + 回饋觀察者           |
| Intent        | 抽象意圖，不含資料、無副作用            |
| TaskService 等 | 根據 Intent 發送封包式事件         |
| EventBus      | 唯一觀察層，CLI 訂閱回饋            |
| FeedbackEvent | 額外提示層，支援 context-aware 呈現 |

這樣的 CLI 架構將允許高度擴充（支援 TUI / Web CLI）、可腳本化（replay）、可觀察（event trace），並與整體語意驅動設計一致。

---

## 5. Event-Oriented State｜事件為中心的狀態模型

---

### 5.1 快照不是真實，而是推論的暫存

在本系統中，「狀態」不再是主系統資料結構，而是從事件流中推論出來的「暫存快照（snapshot）」。例如：

* 任務當前狀態為 `started`
* context 為 `focus`，信心值為 0.6
* timer 剩餘 18 分鐘

這些狀態是根據一連串事件（如 `task_started`, `context_changed`, `timer_ticked`）計算得來，不是永久真實資料。**真實資料僅存在於事件序列中**。

---

### 5.2 每個模組保有自己的快照，但不共享

為保持模組邏輯的高內聚性，快照應由**各 Service 自行管理**，例如：

| 模組              | 負責的快照資料                    |
| --------------- | -------------------------- |
| `TaskService`   | 任務清單與當前狀態                  |
| `ContextEngine` | 當前主 context、context vector |
| `TimerService`  | 當前計時器啟動狀態與開始時間             |
| `InboxService`  | 所有 active inbox entries    |

這些快照只能作為本模組內部推論、回饋或查詢用途，**不得暴露給其他模組或被動查詢**。他模組若需更新自身狀態，應訂閱事件流，自行演算。

---

### 5.3 所有狀態更新皆由事件推動

快照不得由模組自行寫入，只能透過事件來推進。例如：

```plaintext
Event: task_started → TaskService 記錄 task 進入進行中
Event: context_changed → ContextEngine 更新主 context 向量
Event: inbox_entry_archived → InboxService 將該項目移出 active list
```

這保證所有狀態更新具備以下特性：

* 可重播（replayable）：任何狀態皆可從事件重新構造
* 可觀察：狀態變化都有對應事件
* 無副作用：不依賴不可觀察邏輯或查詢

---

### 5.4 快照可用於 UI、CLI、緩存與回饋，但不得用來做邏輯判斷

任何模組都可使用內部快照：

* 快速查詢目前活躍任務
* 顯示當前 context 名稱與信心值
* CLI 呈現任務標題與倒數時間

但不得用快照作為「唯一資訊來源」，亦不得作為決策依據。例如：

```plaintext
❌ TimerService 不得說「這任務是 started」→ 所以我要啟動計時器
✅ TimerService 收到 task_started 事件 → 啟動計時器
```

因此，模組間不得以快照資料為參照發起邏輯判斷，只能對事件反應。


### 5.5 設計原則小結：Event-Capsule Architecture

本系統採用 **封包驅動架構（Event-Capsule Architecture）**，其核心原則如下：

---

#### 原則 1：事件是自足語意單位，不是 CRUD 紀錄

每一個事件（Event）不是資料變動的記錄，而是語意的承載體。它不只是「發生了什麼」，而是「如何讓其他模組理解這個行為、足以做出對應反應」。

事件的設計應**封裝所有行為與推論所需語意**，例如：

```json
{
  "type": "task_started",
  "payload": {
    "task_id": "abc123",
    "title": "撰寫提案書",
    "estimated_minutes": 25
  }
}
```

這讓其他模組（如 Timer、ContextEngine、CLI）皆能**無需額外查詢**地依據此事件行動或渲染。

---

#### 原則 2：事件不是由 CLI 組裝，而是由「語意擁有者」生成

事件應由具備語意上下文的模組產生。例如：

* CLI 發送 Intent：`Intent("start_task", { task_id: "abc123" })`
* TaskService 擁有任務資料 → 根據 intent 組裝 `task_started` 事件並發送

這樣能確保語意一致性、避免重複查詢、提升模組自治性。

---

#### 原則 3：模組不查快照、不 pull 狀態，只根據事件行動

以 `TimerService` 為例：

* 它不查詢任務名稱、估時
* 它只監聽 `task_started` 事件，從中取得所需語意資訊
* 若事件未帶估時欄位，它會選擇 fallback 或無動作，而**不向 TaskService 查資料**

這確保了所有模組行為可觀察、可 replay、可測試，**系統核心邏輯可封裝為純函數（stateless reaction to event stream）**。

---

#### 原則 4：事件不是某模組的內部日誌，而是整個系統的資料交換層

事件不是為了「紀錄」，而是為了讓其他模組理解並行動。例如：

* `task_started` 不代表 TaskService 做了什麼，而是代表這個任務已進入進行中狀態，**其他模組可據此行動**
* 即使 TaskService 本身沒有副作用，它也需負責「發布語意」的任務

---

#### 原則 5：冗餘、延遲、封裝都可接受，唯獨模組邊界模糊不可接受

你可能會發現：

* 某些事件的 payload 重複
* 某些動作需兩步（Intent → Event）
* 某些模組做了發事件以外什麼都不做的「多餘工」

這是設計允許的「語意冗餘」。只要能換取以下結果，就是值得的：

* 邏輯中心分離（模組不互查狀態）
* 減少模組耦合（改一個不影響其他）
* 系統行為可回放（replayable）
* LLM 可觀察（全部語意由事件封裝）

---

### 補充說明：釐清常見誤解

| 誤解                                            | 澄清與對應實作                                       |
| --------------------------------------------- | --------------------------------------------- |
| CLI 要組好所有事件內容                                 | ❌ CLI 發 Intent，由具語意模組（如 TaskService）組事件       |
| Timer 需要去查 Task 快照                            | ❌ 應由事件 payload 提供足夠資訊，如估計時間、title             |
| TaskService 發事件卻沒做事感覺沒意義                      | ✅ 它的責任是描述行為發生與語意封裝，不是執行副作用                    |
| 想讓模組共用資料避免重複計算或多組裝一次事件                        | ❌ 模組可重複處理，但不應違反封包封閉原則；可以建立 helper 計算，不可跨模組拉資料 |
| Inbox、Context、Timer 都需要 task title，但重複欄位好像浪費？ | ✅ 這就是封包式系統設計的代價，用語意一致性換取模組獨立性                 |

---

## 8. Implementation Plan｜實作計畫與優先順序

本章旨在定義實作階段的最小骨架、依賴順序與並行可能性，協助 Agent 具備一致的語意預期與行為合約。

---

### 8.1 MVP Core：最小可用閉環

建立事件導向的任務管理核心，需達成以下閉環：

```plaintext
Intent（task start）→ Event（task_started）→
→ Timer 啟動 → CLI 顯示 → Context 推論 → CLI 提示（ContextChanged）
```

因此，以下為初期必要模組：

| 模組                 | 優先 | 備註                                          |
| ------------------ | -- | ------------------------------------------- |
| `EventBus`         | 1  | 全系統基礎；需支援發送、訂閱、多模組註冊                        |
| `TaskService`      | 2  | 實作 Intent handler → 發出 `task_started` 事件    |
| `CLIInterface`     | 2  | 輸入 Intent、訂閱事件、輸出回饋                         |
| `TimerService`     | 3  | 訂閱 `task_started` → 記錄時間、發送 `timer_tick` 事件 |
| `ContextEngine`    | 4  | 接收事件流 → 推論向量 → 發送 `ContextChangedEvent`     |
| `FeedbackRenderer` | 5  | 組合提示回饋內容，根據 context 動態格式化                   |

---

### 8.2 模組間合約與資料流

各模組之間只透過事件互動。以下為主要資料流摘要：

```plaintext
[CLI]
  ↓ Intent("start_task", task_id)
[TaskService]
  → emit Event("task_started", {task_id, title, estimated_minutes})
[TimerService]
  → emit Event("timer_started")
  → emit Event("timer_tick")
[ContextEngine]
  → emit Event("ContextChangedEvent", {new_context, vector})
[CLI / Feedback]
  ← render response
```

Intent → Event 是一次性轉換，事件應為語意封裝後的 immutable 資料。

---

### 8.3 實作順序建議（含測試）

| 步驟 | 模組          | 重點實作                             | 測試條件                                 |
| -- | ----------- | -------------------------------- | ------------------------------------ |
| 1  | EventBus    | Pub/Sub 機制；同步實作；可記錄事件序列          | 單元測試：事件廣播、重播順序                       |
| 2  | CLI         | Intent Parser；stdin 輸入、事件回饋      | 指令送出後事件正確出現                          |
| 3  | TaskService | 接收 Intent，發送封裝良好的 `task_started` | 含 payload: task\_id, title, estimate |
| 4  | Timer       | 記錄開始時間、定時 tick、發送 `timer_tick`   | 可模擬計時、驗證 tick 間隔與事件內容                |
| 5  | Context     | 基於事件滑動視窗輸出向量、推論切換                | 測試向量與事件關聯是否一致                        |
| 6  | Feedback    | 顯示提示、根據 context 調整語氣/輸出格式        | 顯示包含 context 與 timer 結果              |

---

### 8.4 平行開發策略與介面穩定性

為利多 Agent 並行實作，需定義以下穩定合約：

| 合約項目            | 說明                                                   |
| --------------- | ---------------------------------------------------- |
| `Event` 結構      | type / source / timestamp / payload 為最小欄位            |
| `Intent` 結構     | name + param；不攜帶語意，僅供 service 處理                     |
| `TaskSnapshot`  | task\_id, title, estimated\_minutes；由 TaskService 管理 |
| `ContextVector` | fuzzy map，如 `{focus: 0.6, review: 0.3}`              |

建議每個模組暴露：`subscribe(Event)`, `onIntent(Intent)` 兩組函式，以統一對接方式。

---

### 8.5 延伸模組與未來階段（可 async 開發）

以下模組可等 MVP 閉環完成後擴展，並彼此解耦，可由不同 Agent 同時實作：

| 模組                | 功能                                      |
| ----------------- | --------------------------------------- |
| `InboxService`    | 根據事件 relevance 決定是否進 Inbox              |
| `QuickLogService` | 使用者快速輸入、轉為事件                            |
| `CalendarAdapter` | 對接 Google Calendar，發送 `meeting_started` |
| `PeopleMemory`    | 記錄與人互動後的事件，累積 profile                   |

這些模組亦僅透過 EventBus 溝通，無需修改既有模組內部。


