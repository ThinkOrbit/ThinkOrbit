## ContextEngine Design Document

### 1. Introduction（引言）

現代任務管理系統常以清單為核心，專注於任務的輸入與排序邏輯。然而當使用者面臨高密度、多來源的訊息流與任務流時，傳統設計無法有效支援使用者的認知負荷調節與注意力轉移。

ContextEngine 是一個設計來作為「中樞神經模組」的子系統，其任務是：

> 給定一組持續流動的事件訊號與系統狀態，主動推論並維持當前最合適的注意焦點（context），成為任務模組與 UI 系統的決策依據。

ContextEngine 並不擁有任務，也不直接修改任務狀態。它的核心價值在於提供一個**認知視角的推論中介層**，將龐雜的事件流轉換為具備方向性的 context 向量，進一步支援系統決定「此刻應該做什麼」、「應該忽略什麼」、「是否需要切換工作節奏」。

ContextEngine 的存在意義，是讓第二大腦從任務列表升級為**有節奏感、有注意力控制能力的系統**，並能隨著使用者操作與環境事件持續自我調節。

---

### 2. Design Philosophy（設計哲學）

ContextEngine 並不是一個封閉的狀態機，而是一個可調節、可學習、可觀測的推論模組。為了讓其行為符合使用者預期並具備延展性，我們提出以下九項設計哲學，作為其所有策略與實作的指導原則：

---

#### 2.1 **不可有副作用（No Side Effects）**

ContextEngine 不主動控制任務模組、啟動計時器或切換頁面。它僅發出狀態變更事件（如 `ContextChangedEvent`），其他模組根據此事件決定是否採取行動。

這一原則確保 ContextEngine 的**邏輯純度與可測性**，也避免其成為過度耦合的控制中心。

---

#### 2.2 **邏輯與狀態分離（Inference and State Separation）**

ContextEngine 的推論邏輯（ContextScorer）與目前 context 狀態（ContextRepository）分屬不同模組。這樣可保證推論器可獨立測試、替換、甚至使用不同推論策略而不影響資料一致性。

---

#### 2.3 **可觀察與可追蹤（Observable and Explainable）**

每一次 context 切換都必須具備可回溯的決策紀錄，包括：

* 觸發事件序列
* 原始 context 向量
* 最終切換結果與信心分數

這讓系統具備**可解釋性與除錯能力**，也方便未來進一步訓練或調整推論模型。

---

#### 2.4 **使用者介入是合法語言（User Overrides as First-Class Citizens）**

手動切換 context 不應被當作例外狀況，而是系統的**學習樣本**。ContextEngine 應視為一種反饋行為，記錄 override 行為並考慮其對後續推論的影響。

這讓使用者能逐步調教 ContextEngine，使其更貼近個人節奏與習慣。

---

#### 2.5 **模糊向量而非離散類別（Fuzzy, Not Discrete）**

ContextEngine 輸出的是一組加權向量（如 `focus: 0.6`, `meeting: 0.3`），而非單一標籤。這能更真實反映人類處於多重情境的認知狀態，也讓其他模組根據信心值決定行為強度或優先順序。

---

#### 2.6 **策略可插拔（Pluggable Strategy）**

ContextEngine 應支援多種推論策略模組，包含：

* Rule-based scorer
* 時間敏感 scorer
* ML-based relevance estimator

系統可根據配置、上下文或學習結果組合不同策略模組，實現**彈性推論能力**。

---

#### 2.7 **切換應有慣性（Smooth Transitions）**

Context 切換不應過於敏感，以避免 jitter 或認知負擔。例如：

* 加入最低持續時間
* 加入 hysteresis 門檻（信心需低於某閾值才釋出 context）

這讓系統更接近人類決策節奏，避免過度反應。

---

#### 2.8 **預測不等於切換（Prediction ≠ Transition）**

ContextEngine 可以持續預測使用者可能的當前狀態，但是否實際轉換主 context，應由 `ContextTransitionPolicy` 根據行為模式與歷史穩定性進行判斷。這讓系統在高不確定性狀況下能**暫緩切換，避免誤導**。

---

#### 2.9 **溫和、非中心化（Gentle and Decentralized）**

ContextEngine 的目標是「支持」使用者進入正確心流，而非「指揮」使用者應該做什麼。它所發出的建議應始終可被人類 override，且系統應允許保持現狀的選項存在，避免產生過度自主或不可預測的行為。

---

非常好，我們來整理第 3 章節：**核心概念（Core Concepts）**。這章的目的是建立整個系統共享的語彙與思維模型，使得不同模組、Agent、使用者都能以相同邏輯理解 ContextEngine 的工作方式。

---

### 3. Core Concepts（核心概念）

ContextEngine 運作的基礎不是「任務導向流程」，而是一組連續變化的、具備語意層級的認知結構。這一章將說明幾個不可或缺的核心語彙與其邏輯關係。

---

#### 3.1 `Context`

**Context** 是使用者當下應聚焦的「認知節奏單元」，可以理解為一種具有語意的行為狀態，例如：

* `focus`：進行深度工作、主要任務、編寫或閱讀
* `review`：瀏覽記錄、檢視工作進度、整理 inbox
* `meeting`：正在與人互動、同步、通話
* `social`：與社群互動、看消息、處理即時對話
* `idle`：暫無活動或低效切換中

Context 是系統的「操作基調」，其他模組（如 UI、提示、通知策略）應根據目前 context 調整回饋方式。例如，在 `focus` 狀態下收斂資訊、壓抑干擾；在 `review` 狀態下開放回顧與重組。

Context 並非離散標籤，而是可以並存的 fuzzy 向量（詳見 3.5）。

---

#### 3.2 `Event`

**Event** 是一切推論與觀測的基礎，代表「時間上的某個訊號發生」。每個事件可來自多種來源，包含但不限於：

* **Internal**：系統內部操作與狀態（`task_started`, `pomodoro_finished`）
* **Peripheral**：與使用者任務節奏相關，但來自外部系統（`PR_assigned`, `calendar_event_start`）
* **Ambient**：遠端、被動接收、不強制行動的訊號（`rss_article_posted`, `weather_alert_issued`）

Event 包含以下欄位（邏輯上）：

```json
{
  "id": "...",
  "source_type": "internal" | "peripheral" | "ambient",
  "source_name": "github" | "calendar" | "rss" | ...,
  "type": "task_created" | "pomodoro_finished" | ...,
  "context_hint": "...", // optional
  "payload": {...},
  "occurred_at": "...",
  "relevance": 0.0 ~ 1.0, // 推論結果
  "decay_at": "...", // optional
  "is_archived": false
}
```

Event 是 ContextEngine 的「原料」，推論的過程就是事件流 → context 向量的轉換。

---

#### 3.3 `Relevance`

**Relevance** 是事件與目前使用者認知狀態之間的距離評分。它並不是資料本身的屬性，而是由推論器（Scorer）計算出來的結果。

重要特性如下：

* 可根據 task 相似度、時間接近度、事件強度等多維訊號組合而得
* 可隨時間 decay（relevance 應隨時間或環境改變）
* 可被 ML/Rule 混合產生（並提供解釋）

這讓事件不只是「有來就顯示」，而是具備「是否應打斷」、「何時應提醒」的決策依據。

---

#### 3.4 `Semantic Tier`

**Semantic Tier** 是對事件的**語意層級分類**，反映它與使用者的行動距離，分為三層：

1. **Internal Events**：你自己觸發、直接產生影響的事件（核心）
2. **Peripheral Events**：與你所處任務相關，但不直接產生動作（近端外部）
3. **Ambient Events**：高噪訊、低行動壓力的訊號（背景）

這三層可作為清理策略、事件權重、界面呈現的參考軸，並非資料表實體分離，而是分類語意結構。

---

#### 3.5 `Context Vector`

**Context Vector** 是 ContextEngine 推論的核心輸出：

```json
{
  "focus": 0.6,
  "meeting": 0.3,
  "review": 0.1,
  "social": 0.0
}
```

這個 fuzzy 向量代表當下推論出的多重情境機率分布。ContextEngine 並不強制只有一個主 context，而是提供模糊權重供下游模組自行決定：

* UI 可根據最大值 context 呈現主介面樣式
* Task 模組可根據子 context 微調優先順序
* 通知模組可設閾值控制是否打斷

Context Vector 應有記憶機制（可儲存、比較、追蹤），並支援 hysteresis 門檻避免頻繁切換。

---

#### 3.6 `Transition Policy`

**Transition Policy** 是從 context 向量轉化為「實際切換」的判斷策略。它考慮以下因素：

* 當前 context 的持續時間
* 新 context 的最大信心值
* 信心變化幅度
* 使用者是否近期 override

此層設計讓系統不會因為單一事件或短期干擾就做出大幅切換，而是維持某種節奏與慣性，類似生物神經系統中的抑制控制（inhibitory control）。

---

#### 3.7 `OverrideEvent`

使用者主動輸入 `context switch` 指令，會產生 `OverrideEvent`。這種事件不應只是改變當前 context，更應記錄於系統歷史中，作為未來推論的參照依據。

例如：

* 手動將 `meeting` 改為 `focus`
* 忽略了某個 high-relevance event

這些都可作為訓練資料，幫助系統理解使用者偏好或語境例外。

---

### 小結

| 概念                  | 說明                                         |
| ------------------- | ------------------------------------------ |
| `Context`           | 當前認知聚焦狀態，是行為基調而非任務項目                       |
| `Event`             | 所有內外部訊號的統一表示形式，為 context 推論基礎              |
| `Relevance`         | 表示事件與使用者認知狀態的接近程度，由推論器計算                   |
| `Semantic Tier`     | 事件的語意距離，分為 Internal / Peripheral / Ambient |
| `Context Vector`    | 推論輸出，代表多重情境的 fuzzy 分布                      |
| `Transition Policy` | 判斷是否真正切換 context 的決策機制                     |
| `OverrideEvent`     | 使用者主動調整 context 的訊號，具備學習價值                 |

---

## Use Case

### Use Case 4：使用者多次中斷任務未明確切換，系統延遲轉 context

使用者上午開始一項名為 `Draft NATS client` 的任務，ContextEngine 接收到 `task_started`，主 context 設為 `focus`。

接下來的 20 分鐘內，使用者間斷性地：

* 開啟 quick log 3 次，各留下一句記錄
* 停止 task、未重新啟動
* 輸入 `today` 指令但未選定下一個任務
* 有一次 `pomodoro_started`、中途取消

這些事件形成一組典型的**context 質變但無明確過渡操作**的狀態。

ContextScorer 計算事件序列後，更新 context vector 為：

```json
{
  "review": 0.5,
  "focus": 0.3,
  "idle": 0.2
}
```

TransitionPolicy 比對：

* 主 context `focus` 已維持超過 40 分鐘
* 過去 3 組滑動事件都偏離 `focus` 範型
* 系統未觀察到任務轉換、override 或明確切換指令

符合「可安全切換」條件，ContextEngine 更新主 context 為 `review`，並發出 `ContextChangedEvent("review")`。這項變更可供 inbox 模組調整 task 顯示排序策略，或在 CLI 命令列提示相關資訊。

---

### Use Case 5：低互動週期內 context 主動退場

使用者最近一次任務互動為 `task_completed`，時間為上午 11:23。其後無明確操作，系統僅觀察到偶發的 passive event，例如：

* `calendar_event_past`
* `weather_updated`
* `rss_article_published`（共 6 筆）

ContextScorer 根據事件密度與時間間隔調整向量：

```json
{
  "idle": 0.65,
  "review": 0.25,
  "focus": 0.10
}
```

在過去 20 分鐘內 context vector 演化趨勢穩定，TransitionPolicy 檢查：

* 主 context 已保持 `focus` 超過 40 分鐘
* idle 信心連續三輪高於 0.6
* 無 override 或反向互動事件

觸發 context 切換，主 context 改為 `idle`，寫入 repository，發出 `ContextChangedEvent("idle")`。

此 use case 展示 ContextEngine 對**長時段無互動**的推論能力，以及對「無動作也可構成語境」的處理方式。

---

### Use Case 6：複數同源事件與主 context 產生扭力

過去 10 分鐘內，ContextEngine 收到來自 GitHub 的事件：

* `pr_opened`（他人建立）
* `pr_review_requested`（針對使用者）
* `code_comment_mentioned_you`

這些事件在 semantic source profile 中歸為 peripheral，來源為同一專案。ContextScorer 根據 source clustering 與與既有 task 關聯權重，調整向量為：

```json
{
  "planning": 0.50,
  "focus": 0.30,
  "review": 0.20
}
```

TransitionPolicy 比對近一小時 context 切換歷史，發現該專案與當前 active task 沒有直接關聯，但相關度正在上升。系統選擇不立即切換，而是在 repository 中加入此上下文向量進化紀錄，標記為「扭力態 context pending」。

此 use case 展示 ContextEngine 能追蹤某一主題的壓力累積，並暫存判斷，以待下次互動進一步強化關聯再觸發切換。

---

### 小結：ContextEngine 的範疇內決策

| 場景類型       | 系統行為               | 是否切換主 context  | 行為解釋方式       |
| ---------- | ------------------ | -------------- | ------------ |
| 短暫社交干擾     | 觀察 + 向量微調          | 否              | 容忍，避免過度敏感    |
| 長時間無互動     | 向 idle 過渡 + 時間門檻檢查 | 是              | 自動退場，進入靜默模式  |
| 外部事件聚焦但未操作 | 向量扭力提升 + 延後決策      | 否（但記錄 pending） | 等待互動作為決策強化條件 |

---

## 5. Decision Flow（推論與切換流程）

ContextEngine 的任務並非即時回應每一個事件，而是在觀察一段時間內的事件趨勢、使用者行為變化後，**做出可信任的 context 認知更新**。因此它的決策流程內建多層緩衝與解釋能力。

---

### 5.1 流程總覽

```plaintext
Event Received
     ↓
ContextEngine 更新事件視窗（滑動窗口）
     ↓
ContextScorer 計算 context 向量
     ↓
ContextTransitionPolicy 決定是否切換主 context
     ↓
若切換：
  - ContextRepository 更新主 context
  - 發出 ContextChangedEvent
否則：
  - 保持向量演化狀態
  - （如有需要）標記 pending / torsion 狀態
```

---

### 5.2 事件接收與滑動視窗累積

ContextEngine 會持續訂閱來自 EventBus 的所有事件，包括：

* 使用者行為（task 開始、結束、紀錄 quick log）
* 系統訊號（pomodoro 開始、idle 檢測）
* 外部訊號（RSS、GitHub PR、日曆事件）

這些事件會被加入一個 **時間排序的滑動視窗事件列（event trace window）**，例如：

```json
[
  { "type": "task_started", "at": "09:30" },
  { "type": "quick_log_added", "at": "09:45" },
  { "type": "pomodoro_started", "at": "09:46" },
  ...
]
```

滑動視窗的寬度與策略可調，例如保留過去 30 分鐘內最多 50 筆事件。

---

### 5.3 ContextScorer 推論向量

事件序列傳入 `ContextScorer`，由一組策略（rule-based / ML / hybrid）組合輸出 fuzzy 向量：

```json
{
  "focus": 0.55,
  "review": 0.30,
  "idle": 0.15
}
```

Scorer 會考慮：

* 每個事件的類型與層級（internal/peripheral/ambient）
* 事件與任務之間的 semantic link（例如 PR 屬於某任務範疇）
* 使用者最近的行為密度與方向（例如反覆 log 或 task 停止）

向量不應立即成為 context 切換依據，而是進一步送交判斷策略處理。

---

### 5.4 ContextTransitionPolicy 決策邏輯

這個模組會根據「當前主 context」與「推論向量」間的變化來決定是否進行切換：

**條件可能包括：**

* 新 context 的信心值高於某一閾值（如 0.6）
* 主 context 的信心值低於維持門檻（如 0.3）
* 主 context 已維持超過最短持續時間（如 25 分鐘）
* 最近事件模式明顯偏離主 context
* 無使用者手動 override（避免與使用者行為衝突）

#### 延遲切換（Delay Transition）

若符合部分條件但不完全成立，系統可選擇標記為 `pending` 或 `torsion` 狀態，不立即切換：

```json
{
  "torsion": {
    "target": "review",
    "score": 0.5,
    "duration": "8m",
    "reason": ["frequent quick logs", "no task running"]
  }
}
```

這讓系統具備觀察與穩定性的能力，而不是過度反應。

---

### 5.5 切換與儲存

當 TransitionPolicy 判定應切換：

* ContextRepository 更新主 context 記錄
* 儲存向量與觸發事件序列
* 發出 `ContextChangedEvent`，內容如下：

```json
{
  "new_context": "review",
  "confidence": 0.62,
  "trigger": ["quick_log_added", "task_stopped"],
  "vector": {
    "review": 0.62,
    "focus": 0.28,
    "idle": 0.10
  }
}
```

此事件會供 CLI 顯示資訊排序用，或作為內部模組的決策參考，而非通知使用者。

---

### 5.6 切換歷史與 explainability

每次 context 切換都會記錄為一筆完整 transition log：

```json
{
  "from": "focus",
  "to": "review",
  "at": "2025-05-25T09:48:00Z",
  "reason": "drop in focus confidence; multiple review-pattern events",
  "vector": {...}
}
```

這些紀錄可供未來查詢（`context why` 指令），或訓練 ContextScorer 進行模式學習。

### 5.7 特殊情況下的立即切換（Hard Interrupt）

在極少數情況下，ContextEngine 可以在不經緩衝策略（如最低持續時間、hysteresis）的情況下，**直接切換主 context**。這類情境稱為 **Hard Interrupt**，設計時應慎用，僅限以下條件成立：

#### ✅ 條件一：事件具備強制語意

例如：

* `meeting_started`（來自日曆或通話模組）
* `system_shutdown_initiated`
* `emergency_log_triggered`（用戶輸入特殊 override 或斷電記錄）

這些事件本身不容許延遲，應視為語意上的「斷點」，意味著上下文已強制結束。

#### ✅ 條件二：來源被標記為高信任 + 高即時性

某些來源預先在 source profile 中被標示為：

```json
{
  "source": "calendar",
  "priority": "high",
  "interrupt_capable": true
}
```

這意味著一旦來自此來源的某類事件發生（如 event 開始時間過了），ContextEngine 可以無視當前 context 狀態強制切換至 `meeting` 或其他標定 context。

#### ✅ 條件三：使用者手動下達指令

例如使用者直接下達 `context switch focus`，則可立即觸發 context 切換，此屬於 `OverrideEvent` 類型，但其執行結果也等同於 Hard Interrupt，**不需進入判斷流程**。

---

### 實作補充

在程式邏輯上，ContextEngine 可於事件進入後執行以下邏輯分支：

```go
if event.isInterrupt() {
    repository.setMainContext(event.getDesignatedContext())
    publish(ContextChangedEvent{...})
    return
}
```

---

### ⚠ 設計警語

不應輕易開放外部事件成為 Hard Interrupt，除非：

* 它具備明確的上下文終結語意
* 其觸發不可被誤解或濫用
* 用戶知情並可覆寫結果（例如 undo）

例如 RSS 訊息、GitHub PR 通知、聊天訊息等，應一律**禁止搶占 context**，即使其 relevance 再高，也只能透過常規流程進行轉換判斷。

---

### 小結

| 流程階段  | 說明                                     |
| ----- | -------------------------------------- |
| 事件輸入  | 觀察所有事件進入（EventBus）                     |
| 向量推論  | Scorer 計算 fuzzy context vector         |
| 切換判定  | Policy 根據信心值與模式變化做判斷                   |
| 切換或延遲 | 決定是否立即切換主 context，或保留 pending 狀態       |
| 記錄與通知 | 更新 repository，發出 `ContextChangedEvent` |

---

## 7. ContextScorer Internals（推論器內部設計）

ContextScorer 是 ContextEngine 的認知核心，它的任務是：

> 根據一段時間內的事件序列與當前狀態，計算出多個 context 的相對可能性（fuzzy vector）。

它不負責做決策、不主動切換主 context，只提供一個「此刻系統應傾向聚焦在哪些語境」的中性向量。這章將說明 Scorer 的**結構、語意邏輯、設計哲學與可擴展性**。

---

### 7.1 Scorer 的核心模型

Scorer 可視為一個函數：

```plaintext
score: EventWindow × SystemState → ContextVector
```

輸入為：

* **EventWindow**：滑動視窗內所有已觀察事件（含來源、時間、語意層級）
* **SystemState**：目前任務狀態、上次切換時間、是否 idle、最近 override 等

輸出為：

```json
{
  "focus": 0.65,
  "review": 0.20,
  "idle": 0.15
}
```

向量總和不需為 1，但通常會正規化。

---

### 7.2 認知推論的來源模型

Scorer 所用的邏輯並非單純分類，而是基於「**語意原型**（semantic prototype）」建構出的分數疊加模型。具體來說，每個 context 都有一組典型事件圖譜（event signature set）：

#### 例如：

```json
"context: focus" {
  "positive_events": ["task_started", "pomodoro_started", "quick_log_added"],
  "negative_events": ["rss_fetched", "calendar_event_past", "task_idle_detected"]
}
```

每當事件序列中出現正相關事件，會為該 context 增加分數，出現負相關事件則扣分。

> 這就像你正在閱讀（`quick_log_added`）時，有人發了三篇 PR 提醒（`github_mentioned_you`），Scorer 不會立即切 context，而是記下這種「結構性偏移」。

---

### 7.3 推論結構：加權疊加 + 時間衰減

每一事件會對各個 context 造成一定程度的加權貢獻：

```plaintext
score[event][context] × decay_factor(t)
```

* **score\[event]\[context]** 是靜態定義或可訓練的權重表
* **decay\_factor(t)** 是事件距離現在的時間衰減係數（例如半衰期為 10 分鐘）

這讓系統具備「遺忘機制」：遠古事件不應與當前 context 推論產生等量貢獻。

---

### 7.4 可組合策略架構（Pluggable Strategies）

Scorer 並非單一實作，而是一個策略集合（Ensemble），支援多種組合方式：

| 策略類型                | 描述                                      |
| ------------------- | --------------------------------------- |
| RuleBasedScorer     | 使用明確的事件對 context 關聯表，具備高可解釋性            |
| MLBasedScorer       | 使用語意編碼、事件序列轉向量、transformer 或 HMM 等      |
| TimeSensitiveScorer | 根據每日節奏建模（如早上傾向 `planning`、下午傾向 `focus`） |
| OverrideLearner     | 學習使用者手動修正時的偏好模式，形成補正模型                  |

每一策略可對 context vector 貢獻一組權重，最後做 weighted ensemble。

---

### 7.5 抽象接口設計

若以 Java 為例，Scorer 接口可設計為：

```java
interface ContextScorer {
    Map<ContextType, Double> score(List<Event> window, SystemState state);
}
```

其中：

* `ContextType` 是已註冊的 context 名稱（如 focus, review）
* `score(...)` 為純函數：同一組輸入應輸出相同結果，便於測試與快取

---

### 7.6 可觀察性與解釋能力

Scorer 的每次執行，都必須保留一份完整解釋紀錄：

```json
{
  "input_events": [...],
  "per_context": {
    "focus": {
      "raw": 0.52,
      "contribution": [
        { "event": "task_started", "score": +0.3 },
        { "event": "quick_log_added", "score": +0.2 },
        { "event": "task_stopped", "score": -0.1 }
      ]
    },
    ...
  }
}
```

這樣使用者執行 `context why` 指令時，能夠獲得真實理由，而非黑箱模糊結果。

---

### 7.7 錯誤修正與學習

* 使用者的 `OverrideEvent` 可視為一組修正範例（counterexample）
* Scorer 可建模使用者偏好，例如「我在早上 10 點切 `planning` 比 `focus` 還常見」
* 若啟用 ML 模組，則需保證訓練資料可溯源、可清除（支援 GDPR 式 forget）

---

### 小結

Scorer 並非分類器，而是「**語意認知模組**」，其行為設計必須同時滿足：

* **可插拔**：支援多策略混合
* **可觀察**：可產生詳細貢獻解釋
* **可調節**：支援語意偏好學習與衰減控制
* **可測試**：為純推論模組，不具副作用

它是 ContextEngine 的心智模型來源，也是整體系統是否「看得懂使用者在做什麼」的關鍵。

---

## 8. Design Constraints（限制與預設）

ContextEngine 雖具備推論與推送訊號的能力，但其角色始終是一個**觀察者與建議者**，而非控制器或任務執行單元。以下列出其設計中的關鍵限制與預設值，任何擴充或整合模組都應遵守這些邊界。

---

### 8.1 邊界限制（Hard Constraints）

#### 不應直接操作任務資料

ContextEngine 不得：

* 建立、啟動、結束任何任務
* 修改任務屬性（如優先級、分類、標籤）
* 調整任務的展示順序或歸屬關係

若需根據 context 調整任務行為，應由任務模組訂閱 `ContextChangedEvent`，自行做出對應調整。

---

#### 不應同時持有多個主 context

ContextEngine 輸出的 fuzzy 向量可以同時具備多個 context，但**主 context 僅能為一個**。這是為了維持：

* 使用者認知的單一焦點模型
* 下游模組簡化邏輯（例如依主 context 顯示任務）

若出現模糊情境，應透過 `ContextTransitionPolicy` 延後切換，而非同時激活多個主 context。

---

#### 不應主動發出通知或打斷式回饋

ContextEngine 的職責是維持 context 狀態與流動，不應：

* 主動觸發提示文字
* 彈出通知、聲音、提醒等形式
* 要求使用者立即回應某事件

所有反饋應由 CLI 或使用者互動模組依主 context 判斷後自願性呈現。

---

#### 不應擁有內部副作用

所有 scorer、策略模組皆應為**純推論函數**。不得：

* 改動事件、資料庫、任務狀態
* 執行系統指令
* 引入非決定性行為（除非明確標示為 experimental）

這可保證推論過程可重演、可測試、可追蹤。

---

#### 不得根據單一事件立即切換 context（除非明示 interrupt）

除非事件被明確標記為 `hard_interrupt`，否則 ContextEngine 不得在僅收到一筆 peripheral 或 ambient 事件後立刻切換 context。Context 必須源自事件**模式與趨勢**，非瞬時反應。

---

### 8.2 預設行為與參數（Defaults）

#### `relevance` 預設為 0.5

所有事件若未特別標註 relevance，應預設為中性值 0.5，並由 Scorer 根據上下文進行加權與調整。

---

#### context 切換最低維持時間為 10 分鐘

主 context 切換後，若非 override 或 interrupt，應維持至少 10 分鐘再允許切換。此參數可透過配置調整。

---

#### 滑動事件視窗預設觀察 30 分鐘內最多 50 筆事件

Scorer 須根據此範圍進行推論，超出者視為失效資料。

---

#### 向量變化閾值為 ±0.3 才能考慮切換

如果新向量與目前向量差異未達 0.3，則視為 context 穩定，不進行切換（除非其他策略條件成立）。

---

#### fuzzy 向量總和不限制為 1.0，但預設正規化

Scorer 可依實作需求決定是否正規化，但下游模組預期接收相對分布，因此建議所有向量結果皆經標準化處理。

---

#### context type 必須先註冊才可使用

任意新增 context type（如 `deep_research`, `errand`, `conversation`）必須先透過註冊程序導入系統，並提供：

* context 名稱與語意描述
* 其典型事件範型
* relevance 特性與影響維度

這可避免誤用 context 命名造成系統錯判。

---

