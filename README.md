# Feng Chain Miner / 枫之连锁挖矿

一个默认自动触发、无需按住 Shift 的 Minecraft 连锁挖矿模组，同时维护 Fabric 与 Forge 版本。

## 功能

- 普通挖掘方块时自动触发，无需潜行或按快捷键。
- 支持煤、铜、铁、青金石、红石、绿宝石、金、钻石矿石及其深层变种。
- 支持全部原木；不同树种不会混挖。
- 搜索相邻 26 个方向，因此斜角相连也会被识别。
- 默认每次最多处理 128 个方块，避免超大结构造成卡顿。
- 使用玩家正常挖掘流程，保留工具耐久、附魔、掉落和权限检查。
- 连锁挖掘逻辑仅在服务端执行；单人游戏直接安装，服务器使用时服务端必须安装。

### Fabric 1.20.1 游戏内设置

- 默认按 `V` 打开设置界面，也可以在“选项 → 控制”中重新绑定快捷键。
- 可设置每次最多连锁的方块数量，范围为 1–4096。
- 所有可选择方块以游戏内图标网格显示；点击一次加入连锁并显示绿色边框，再点一次取消。
- 支持按方块名称或 ID 搜索、翻页、全部取消和恢复默认。
- 旧的方块标签配置会自动展开成对应的单个方块，保存后使用明确的方块 ID 列表。
- 单人游戏保存后立即生效；配置文件位于实例的 `config/feng-chain-miner.json`。

## 支持矩阵

| Minecraft | Fabric | Forge | Java |
| --- | --- | --- | --- |
| 1.20.1 | ✅ | ✅ | 17 |
| 1.21.1 | ✅ | ✅ | 21 |

Fabric 版需要安装 Fabric API；Forge 版没有额外运行依赖。每个 Minecraft 版本需要下载对应的 jar，不能跨版本混用。

后续计划适配 1.21.11 和 26.x；这些版本的 Minecraft 与加载器接口有明显变化，会作为独立目标发布，不会用一个 jar 强行兼容。

## 安装

1. 在 Releases 中下载与你的 Minecraft 版本及加载器对应的 jar。
2. Fabric 玩家同时安装 Fabric API。
3. 把 jar 放入游戏实例的 `mods` 文件夹，然后启动游戏。

HMCL 中请先确认实例的游戏版本和加载器，再把对应文件拖入“模组管理”。

## 本地构建

仓库包含 Gradle Wrapper。按目标版本选择命令：

```bash
./gradlew -p versions/1.20.1/fabric build
./gradlew -p versions/1.20.1/forge build
./gradlew -p versions/1.21.1/fabric build
./gradlew -p versions/1.21.1/forge build
```

Windows 中使用 `gradlew.bat`。生成的 jar 位于相应项目的 `build/libs` 文件夹。

## 项目结构

- `core`：与加载器无关的连通方块搜索算法。
- `versions/<mc版本>/fabric`：Fabric 事件及 Minecraft API 适配。
- `versions/<mc版本>/forge`：Forge 事件及 Minecraft API 适配。
- `.github/workflows`：四套版本的自动构建与发布。

## 参与开发

欢迎提交 Issue 和 Pull Request。新增 Minecraft 版本时，请保留现有行为，并为对应加载器添加独立构建项目。

版本记录见 [CHANGELOG.md](CHANGELOG.md)。本项目使用 [MIT License](LICENSE)。


