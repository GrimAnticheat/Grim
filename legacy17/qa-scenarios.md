# GrimLegacyAC QA Scenarios（legacy17）

以下场景用于稳定复现/回归验证 `legacy17` 的典型误判高风险路径，并与 `/glac dump` 中的 `scenario` 字段对齐归档。

## 场景清单（10+）

1. **斜跳加速（scenario=diagonal_jump）**  
   玩家以 45° 输入持续起跳，观察 Speed/Prediction 是否被异常拉高。

2. **格挡移动（scenario=block_sprint_mix）**  
   旧版本剑格挡 + 冲刺切换，验证 NoSlow 与 Speed 的边界。

3. **高处落地（scenario=high_fall_landing）**  
   从 4 格以上落地并立刻平移，关注 NoFall、Prediction 容差。

4. **鱼竿双连勾（scenario=rod_double_pull）**  
   连续两次钩中玩家并拉扯，关注 Velocity/Prediction 的 buffer 激增。

5. **鱼竿单次拉扯（scenario=rod_pull）**  
   单次钩中后立刻变向，检验 Speed 与 Reach 后续链路影响。

6. **液体受击（scenario=liquid_hit）**  
   玩家站在水中/水边被击退，验证 Velocity、Jesus、Prediction。

7. **液体移动（scenario=liquid_movement）**  
   纯水中移动/跳水转折，检查 Speed/Jesus/Prediction 的正常收敛。

8. **珍珠位移（scenario=pearl_displacement）**  
   末影珍珠落地后立即操作（攻击/冲刺），验证 teleport 对齐窗口。

9. **边缘卡脚（scenario=edge_stuck）**  
   方块边缘小幅抖动位移，检测 stuck-edge 相关误判。

10. **受击窗口（scenario=velocity_window）**  
    常规击退后 10 tick 内轨迹，验证 Velocity tx-window 对齐。

11. **传送后首包（scenario=post_teleport_first_move）**  
    TP 后第一/第二个移动包，检查 SKIPPED 与恢复时机。

12. **库存移动（scenario=inventory_move_open）**  
    开背包状态下持续平移跳跃，关注 InventoryMove 与 NoSlow 协同。

## 固定回归流程（每次改动后执行）

1. 收集带 `[GLAC-DEBUG]` 的日志（建议测试服固定脚本跑上述场景）。
2. 运行：
   ```bash
   tools/log-analyzer <log_file>
   ```
3. 记录并比较以下三项合并门槛指标：
   - 误判数量
   - 漏判数量
   - 平均触发延迟

> 建议将每次回归输出附在变更说明中，作为是否合并依据。
