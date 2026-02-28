# GrimLegacyAC (experimental)

这是一个为 **Spigot 1.7.10** 设计的独立反作弊插件模块，借鉴 Grim 的「玩家状态建模 + 多检查协同 + 缓冲/衰减」思路，使用事件层实现。

## 当前实现的检测

- Movement: Speed / Fly / Phase / Timer / Jesus / InventoryMove / Prediction
- Combat: Reach / KillAura / AutoClicker / Velocity
- World/Interact: FastPlace / FastBreak / FastUse
- Misc: NoFall

> 说明：由于 1.7.10 环境与协议差异，无法 1:1 复刻 Grim 全部现代检测，但已按 Grim 的模块化思路扩展到多类别检测，并可继续迭代。

## 架构特性（相对上一版加强）

- Netty pipeline 注入（玩家进服后注入 ChannelDuplexHandler，优先捕获原始移动包）
- 每玩家状态缓存（位移、旋转变化、空中/落地 tick、CPS 窗口、移动频率窗口、速度响应窗口）
- 每检测独立 `buffer` 与 `VL`，减少瞬时误报
- 全局 `violation-decay-per-second` 衰减机制（每秒任务）
- 加入 join/teleport/velocity 保护窗口，减少回弹与受击后误报
- 支持检测级自动处罚命令（`punish-vl` + `punish-commands`）
- `/glac profile <player>` 快速查看各检测 VL

## 构建

```bash
./gradlew :legacy17:build
```

产物在 `legacy17/build/libs/` 下。

## 命令

- `/glac info`
- `/glac alerts`
- `/glac reload`
- `/glac profile <player>`

## 权限

- `grimlegacy.command`
- `grimlegacy.alerts`
- `grimlegacy.bypass`
