<div align="center">
 <h1>GrimAC</h1>
 
 <div>
  <a href="https://github.com/GrimAnticheat/Grim/actions/workflows/gradle-publish.yml">
   <img alt="工作流状态" src="https://github.com/GrimAnticheat/Grim/actions/workflows/gradle-publish.yml/badge.svg" />
  </a>
  <a href="https://discord.grim.ac">
   <img alt="Discord社区" src="https://img.shields.io/discord/811396969670901800?style=flat&label=Discord&logo=discord">
  </a>
 </div>
 <br>
</div>

GrimAC 是一款开源 Minecraft 反作弊系统，专为最新版 Minecraft 设计，同时支持 1.8 及更高版本。为避免误判，基岩版玩家（Geyser）将完全免于反作弊检测。当前项目免费开源，但未来版本可能转为付费模式或包含订阅制高级检测功能。如需修复漏洞或功能增强但无法提供资金支持，欢迎提交 Pull Request。

## 下载渠道
- [Modrinth](https://modrinth.com/plugin/grimac)
- [Hangar](https://hangar.papermc.io/GrimAnticheat/GrimAnticheat)
- [SpigotMC](https://www.spigotmc.org/resources/grim-anticheat.99923/)
- *前沿构建版请使用 GitHub 制品*：[Bukkit版](https://nightly.link/GrimAnticheat/Grim/workflows/gradle-publish/2.0/grimac-bukkit.zip) | [Fabric版](https://nightly.link/GrimAnticheat/Grim/workflows/gradle-publish/2.0/grimac-fabric.zip)

## 资源
- 文档与示例请参阅 [Wiki](https://github.com/GrimAnticheat/Grim/wiki)
- 常见问题解答请访问 [FAQ](https://github.com/GrimAnticheat/Grim/wiki/FAQ)
- 社区支持与项目讨论请加入 [Discord](https://discord.grim.ac)

## 要求与安装
- Java 17 或更高版本（详见 [升级至Java17指南](https://github.com/GrimAnticheat/Grim/wiki/Updating-to-Java-17)）
- 需 Spigot/Paper/Folia/Fabric 服务端环境（详见 [支持的环境](https://github.com/GrimAnticheat/Grim/wiki/Supported-environments)）
- 若使用 Geyser，请在后端服务器部署 Floodgate 以便 Grim 识别基岩版玩家（代理层无法访问 Floodgate API）
- 若使用 ViaVersion，应部署于后端服务器（移动检测高度依赖客户端版本）

## 开发者插件 API
通过 [Grim API 仓库](https://github.com/GrimAnticheat/GrimAPI) 可将 Grim 集成至您的插件，获取源代码与详细文档。

## 源码编译
1. `git clone https://github.com/GrimAnticheat/Grim.git`
2. `cd Grim`
3. `./gradlew build`
4. 成品 jar 文件将生成于 `<平台>/build/libs` 目录

## 提交 Pull Request
贡献指南详见 [CONTRIBUTING.md](CONTRIBUTING.md)

## Grim 的核心优势
为何 Grim 能脱颖而出？

### 运动模拟引擎
* 1:1 精准复刻玩家所有可能移动
  * 覆盖行走、游泳、击退、蛛网、气泡柱等场景
  * 支持乘骑实体（船只/猪/炽足兽等）
* 极端案例覆盖确保准确性
* 全面支持跨版本场景：
  * 1.13+ 客户端 → 1.13+ 服务端
  * 1.12- 客户端 → 1.13+ 服务端
  * 1.13+ 客户端 → 1.12- 服务端
  * 1.12- 客户端 → 1.12- 服务端
* 碰撞顺序按客户端版本精确还原
* 版本间碰撞箱差异精准处理：
  * 单格玻璃板：1.7-1.8玩家显示"+"形，1.9+玩家显示"*"形
  * 1.13+客户端在1.8服务端通过ViaVersion看到"+"形碰撞箱
  * 水logged方块对1.12-玩家无效
  * 客户端不存在的方块使用ViaVersion替代方案
* 完整实现所有原版碰撞箱

### 全异步多线程架构
* 所有移动检测及绝大多数监听器运行于Netty线程
* 可扩展至数百玩家规模
* 严格保证线程安全
* 核心架构支撑高性能设计

### 完整世界复刻
* 为每位玩家创建独立世界副本
* 通过区块数据包/方块变更事件构建副本
* 使用调色盘技术压缩区块（16-64KB/区块）
* 通过缓存安全访问世界状态
* 玩家级缓存支持多线程操作
* 假方块数据包不会触发误判
* 独立世界副本支持延迟补偿
* 客户端方块与数据包方块互不冲突

### 延迟补偿机制
* 世界变更操作延迟至玩家数据到达
* 玩家脚下方块破坏不会触发误判
* 飞行状态/移动速度等全面延迟补偿

### 背包状态同步
* 实时追踪玩家背包状态
* 避免高延迟下的幽灵方块等问题

### 安全设计（非混淆式）
* 所有系统通过数学模型实现防绕过
* 例如运动预测引擎覆盖所有可能路径，理论不可破解
