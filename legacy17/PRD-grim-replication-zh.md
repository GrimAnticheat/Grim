# PRD：legacy17 进一步复刻 Grim（面向另一个 AI 的实施说明）

## 1. 文档目标

本 PRD 的目标不是给“可直接执行的命令清单”，而是给出**可交付的产品需求 + 设计方法 + 落地策略**，便于你把这份文档转交给另一个 AI，让它按统一目标继续推进 legacy17 对 Grim 的复刻。

---

## 2. 背景与现状解析

### 2.1 Grim（主项目）核心特征（现状）

从仓库可见，Grim 当前是一个多模块、跨平台的反作弊系统（`common`/`bukkit`/`fabric`），并且在 `common` 层构建了平台无关的核心能力，包括：

- 全局 API 与初始化生命周期管理（`GrimAPI` + `InitManager`）
- 以玩家为中心的大型状态对象（`GrimPlayer`）
- 高密度包事件驱动的检查流水线（`CheckManager` + `CheckManagerListener`）
- 预测、延迟补偿、世界/实体补偿等深度组件化机制

### 2.2 legacy17（1.7.10 子模块）核心特征（现状）

legacy17 已经具备“类 Grim”的基础骨架，但复杂度明显更低：

- 单插件入口（`LegacyAntiCheatPlugin`）
- 检查管理器集中调度（`CheckManager`）
- 协议层优先 + Bukkit 回退（pipeline）
- 交易包 RTT、Prediction、Backtrack hitbox、基础自适应延迟阈值

其定位是“1.7.10 兼容下的实验性独立实现”，并已提供 QA 场景基线。

### 2.3 二者差距（复刻关键）

legacy17 与 Grim 的主要差距不在“有没有检查项”，而在于：

1. **状态建模粒度**：Grim 以 `GrimPlayer` 聚合大量跨子系统状态；legacy17 的 `PlayerData` 仍偏轻量。  
2. **事件/数据总线化能力**：Grim 的检查分型与调用时机高度结构化；legacy17 调用链相对直连。  
3. **补偿系统完整性**：Grim 有更完整的 world/entity/inventory/latency compensation 组合；legacy17 仍是关键点实现。  
4. **可扩展性与可观测性**：Grim 的模块化与可插拔程度更高；legacy17 在“调试证据标准化”和“回归治理”上还可继续深化。  

---

## 3. 产品定位（Product Positioning）

### 3.1 产品愿景

把 legacy17 从“有 Grim 思路的 1.7.10 反作弊实现”升级为“**在 1.7.10 约束下，最大程度对齐 Grim 设计哲学**的高可靠反作弊内核”。

### 3.2 目标用户

- 1.7.10 服务器服主/技术服主
- 需要在老版本保留较强反作弊能力的运维团队
- 关注低误判、可调参、可解释证据链的技术玩家社区

### 3.3 成功标准（业务层）

- 在现有 QA 场景中，误判率下降（或不升高）
- 核心检查在高延迟/低 TPS 下稳定性提高
- 新增功能在调试日志中可解释（可定位触发原因、补偿状态、阈值来源）

---

## 4. 范围定义（Scope）

### 4.1 本轮“进一步复刻 Grim”应包含

1. 状态层重构：把 `PlayerData` 进化为“分域状态聚合器”。
2. 检查流水线分层：preprocess → prediction → post-prediction → fallback 的可声明化。
3. 延迟补偿增强：tx/ping/jitter/tps 四维输入统一驱动容差预算。
4. Combat 证据链升级：Reach/Velocity/KillAura 的上下文证据标准化。
5. 可观测性治理：统一 debug 字段模型 + QA 回归输出基线。

### 4.2 本轮不建议纳入（避免失焦）

- 一次性引入过多新检查类型（先把已有检查“做深做稳”）
- 跨平台抽象（legacy17 先在 Spigot 1.7.10 做强）
- UI 化配置后台（先保证算法与证据链）

---

## 5. 功能需求（Functional Requirements）

## FR-1：分域玩家状态模型

**目标**：把当前 `PlayerData` 扩展成按域组织的状态模型，减少检查间隐式耦合。

**怎么做（给另一个 AI）**：

- 将状态拆为至少 6 个域：
  - `MovementState`
  - `CombatState`
  - `NetworkState`
  - `CompensationState`
  - `EnvironmentState`
  - `EnforcementState`
- 每个域只暴露“读接口 + 有界写接口”，避免检查随意改全局字段。
- 定义状态更新顺序（严格时序）：
  1) 包输入归一化
  2) 网络指标更新
  3) 预测计算
  4) 检查执行
  5) 处罚/回滚
  6) 证据落盘

**验收要点**：

- 任意检查都能明确声明自己读写哪个域。
- 调试输出能定位“哪个域导致本次触发”。

## FR-2：检查流水线声明化与编排

**目标**：对齐 Grim 的“检查分型 + 固定调用阶段”思路，而非散点触发。

**怎么做**：

- 给每个检查增加元信息：
  - 执行阶段（PRE / PREDICTION / POST / FALLBACK）
  - 依赖前置（如必须有预测结果）
  - 是否允许在 teleport 未对齐时运行
- 由统一编排器驱动，不再由检查互相调用。
- 输出每帧“执行图谱摘要”（执行了哪些检查、被跳过原因、耗时）。

**验收要点**：

- 可以生成单帧 pipeline trace。
- 当 prediction unavailable 时，fallback 行为一致且可解释。

## FR-3：统一容差预算（Tolerance Budget）

**目标**：把当前分散在配置与检查内部的容差逻辑，合并成统一预算器。

**怎么做**：

- 统一输入维度：
  - RTT（交易包）
  - Jitter
  - TPS
  - 最近受击/传送/液体/边缘状态
- 预算输出至少包含：
  - movement allowance
  - combat reach margin
  - velocity response slack
- 检查只消费预算结果，不自行重复推导。

**验收要点**：

- 同一玩家同一帧，所有检查看到的是同一份预算快照。
- Debug 中能还原预算来源与分解项。

## FR-4：Combat 证据链标准化

**目标**：把 Reach/Velocity/KillAura 的判定与证据格式统一。

**怎么做**：

- 定义 `CombatEvidence` 统一结构：
  - actor/target 基础信息
  - attack 时间轴（本地时间、transaction 对齐时间）
  - 历史 hitbox 命中窗口
  - 输入旋转与轨迹关键点
  - 最终判定得分与阈值
- Reach 与 KillAura 共用“视线-命中盒-时间窗”中间层。
- Velocity 与 Prediction 共用“受击响应窗口”中间层。

**验收要点**：

- 三类 Combat 检查输出可拼接成同一条事件报告。
- 人工复盘不需要翻多类日志格式。

## FR-5：配置分层与安全默认值

**目标**：降低调参复杂度，避免错误配置放大误判。

**怎么做**：

- 配置拆层：
  - `global`（全局策略）
  - `pipeline`（执行拓扑）
  - `budget`（容差预算）
  - `checks.<name>`（检查个性化参数）
- 给每个检查参数增加：
  - 推荐范围
  - 极端值风险说明
  - 是否影响误判/漏判
- 新增“保守模式 profile”（高延迟服默认建议）。

**验收要点**：

- 修改配置后，日志能打印关键参数摘要与风险提示。
- 关键参数越界时有保护（钳制或警告）。

---

## 6. 非功能需求（NFR）

1. **稳定性**：单玩家异常数据不得阻塞全局检查线程。  
2. **性能**：主要检测链路避免高频对象分配，重点结构复用。  
3. **可调试性**：每次 flag 都可追溯“输入→预算→判定→处罚”。  
4. **兼容性**：保持 Spigot 1.7.10 与 ProtocolLib 可选路径。  
5. **可维护性**：新增检查必须接入统一阶段模型与证据模型。

---

## 7. 技术方案（Solution Design）

### 7.1 架构蓝图（建议）

- 输入层：ProtocolLib / Netty / Bukkit fallback
- 归一化层：MovementFrame + ActionFrame（统一事件模型）
- 状态层：PlayerStateDomain Aggregates
- 预算层：ToleranceBudgetEngine
- 检查层：Stage-based Check Executors
- 证据层：EvidenceBus（MovementEvidence / CombatEvidence / WorldEvidence）
- 决策层：Violation & Enforcement Engine

### 7.2 关键设计原则

- **单向数据流**：输入 → 状态更新 → 预算 → 检查 → 决策，禁止逆向写回。
- **时序优先**：所有判定都基于“可对齐时间点”的快照。
- **中间结果复用**：Prediction/Hitbox/Transaction 窗口尽量共享，避免重复计算。
- **降级可解释**：任何 fallback 必须带 reason code。

---

## 8. 交付计划（给另一个 AI 的推进顺序）

> 这里给的是“实施顺序与方法”，不是 shell 命令。

### 阶段 A：模型固化

- 先产出状态域数据结构与字段字典。
- 再定义每种包/事件如何更新状态域。
- 最后补充状态快照序列化（用于 debug）。

### 阶段 B：流水线重排

- 把现有检查按阶段归类。
- 引入统一编排入口，接管触发逻辑。
- 保留旧逻辑作为灰度开关，先双轨比对。

### 阶段 C：预算器接管

- 抽取散落阈值逻辑，统一放入 Budget Engine。
- 每帧生成 budget snapshot 并注入检查上下文。
- 双日志比较：旧阈值 vs 新预算输出。

### 阶段 D：Combat 中间层

- Reach/KillAura 共享射线与回溯时间窗。
- Velocity 与 Prediction 对齐窗口共享。
- 输出统一 CombatEvidence。

### 阶段 E：回归治理

- 用已有 QA 场景跑回归。
- 建立“误判/漏判/触发延迟”固定报表。
- 设定上线门槛：未达标则不切换默认路径。

---

## 9. 风险与缓解

1. **风险：1.7.10 协议差异导致复刻上限**  
   - 缓解：明确“行为等价优先于实现等价”，用等效中间模型替代新版本特性。

2. **风险：过度追求严格导致误判升高**  
   - 缓解：预算器必须纳入 jitter/tps，保守模式作为默认上线策略。

3. **风险：调试日志过多影响性能**  
   - 缓解：日志分级 + 采样 + 按玩家开关。

4. **风险：检查耦合重构期间引入回归**  
   - 缓解：双轨灰度 + QA 场景基线对比。

---

## 10. 验收标准（Definition of Done）

- 结构验收：
  - 状态域、流水线、预算器、证据模型都已接入主链路。
- 质量验收：
  - QA 基线场景中误判不高于当前版本。
  - 关键检查触发延迟稳定或改善。
- 运营验收：
  - 管理员可从日志直接读出触发原因、补偿状态、处罚依据。

---

## 11. 给另一个 AI 的执行提示词模板（可直接转发）

你可以把下面文本原样发给另一个 AI：

> 你现在是 legacy17 的核心开发 AI。目标是在 Spigot 1.7.10 约束下进一步复刻 Grim 设计哲学。请按以下顺序推进：
> 1) 先完成 PlayerData 的分域建模与字段字典；
> 2) 再做阶段化检查编排（PRE/PREDICTION/POST/FALLBACK）；
> 3) 接入统一容差预算器（RTT/Jitter/TPS/事件上下文）；
> 4) 统一 Reach/KillAura/Velocity 的 CombatEvidence 输出；
> 5) 用 QA 场景做双轨对比，确保误判不升高。
> 输出要求：每一步都给出“设计说明、影响面、回归风险、验收标准”，不要只给代码。

---

## 12. 附：本 PRD 依据的仓库事实

- Grim 主项目是多模块工程，包含 `common`、`bukkit`、`fabric`、`legacy17`。  
- Grim 的核心能力集中在 `common`，如 `GrimAPI`、`GrimPlayer`、`CheckManager`、`CheckManagerListener`。  
- legacy17 已包含 packet-first/fallback、transaction、prediction、combat backtrack、QA 场景文档等基础能力。  

