# GrimLegacyAC (legacy17)

> **一句话 / One sentence:**
> GrimLegacyAC（legacy17）是一个面向 Spigot 1.7.10 的模块化反作弊插件，基于数据驱动的玩家状态建模与延迟补偿来检测并拦截移动、战斗与交互作弊。  
> GrimLegacyAC (legacy17) is a modular anti-cheat plugin for Spigot 1.7.10 that uses data-driven player-state modeling and latency compensation to detect and block movement, combat, and interaction cheats.

## 简介 | Overview

GrimLegacyAC 是 Grim 思路在 1.7.10 环境下的独立实现版本，重点是：

- 多检查协同（Movement / Combat / World / Misc）
- 交易包与 KeepAlive 延迟感知
- 回溯命中盒（Backtrack hitbox）与射线判定
- 缓冲（Buffer）与违规值（VL）累计/衰减
- 调试证据输出与自动处罚命令

---

## 功能列表 | Features

### Movement
- Speed
- Fly
- Phase
- Timer
- Jesus
- InventoryMove
- Prediction

### Combat
- Reach
- KillAura
- AutoClicker
- Velocity

### World / Interact
- FastPlace
- FastBreak
- FastUse

### Misc
- NoFall

---

## 架构特性 | Architecture Highlights

- ProtocolLib 包监听（POSITION / LOOK / FLYING / TRANSACTION / KEEP_ALIVE）
- Netty pipeline 注入（ProtocolLib 不可用时回退）
- Transaction RTT 同步（发出并确认交易包，估算往返延迟）
- Shadow Prediction（ExpectedPos = LastPos + Motion × Friction）
- 自适应延迟门控（高 jitter / 低 TPS 时动态放宽阈值）
- Teleport Sync 冻结机制（防止传送阶段误报）
- Reach 射线 + 历史 hitbox 回溯判定（默认 400ms）

---

## 构建 | Build

```bash
./gradlew :legacy17:build
```

构建产物位于：

- `legacy17/build/libs/`

---

## 命令 | Commands

- `/glac info`
- `/glac alerts`
- `/glac reload`
- `/glac profile <player>`
- `/glac debug <player>`

---

## 权限 | Permissions

- `grimlegacy.command`
- `grimlegacy.alerts`
- `grimlegacy.bypass`

---

## 配置提示 | Config Tips

重点配置路径（`legacy17/src/main/resources/config.yml`）：

- `pipeline.packet-first`：优先使用包级事件作为数据源
- `pipeline.bukkit-fallback`：包级数据不可用时允许回退到 Bukkit 事件
- `pipeline.bukkit-fallback-stale-nanos`：包级数据超过该时间窗口视为过期并触发回退
- `transaction.*`：交易包同步频率与 ACK 时效
- `combat.backtrack-window-ms`：回溯命中盒时间窗口
- `adaptive-lag.*`：高延迟自适应阈值
- `checks.<CheckName>.*`：每个检测的开关、buffer、setback、处罚

建议先开启 `/glac debug <player>` 在测试服观察证据日志，再调整阈值上线。

---

## 兼容说明 | Compatibility

- 目标平台：**Spigot 1.7.10**
- 属于实验性模块，建议在测试环境先验证参数后再用于生产服。
