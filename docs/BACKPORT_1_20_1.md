# 主分支剧情功能回迁到 Forge 1.20.1

本次变更由 Agent 辅助生成。仅修改剧情模组，不改 SAO 服务器的经济、NPC 注册表或 FTB 任务。

## 对比基线与开发过程

- 目标分支：`1.20.1`，起点 `e20d5d1`，开始时工作区干净。
- 功能来源：`origin/master` 的 `955c457`。
- 配套主工程：ViScriptLib `1.20.1` 的 `afcb958`。
- 先检查两分支差异，再区分业务功能、平台替换和开发工具，避免直接合并 NeoForge 源码。
- 初始差异涉及 92 个文件，绝大多数为 Forge/NeoForge、Java 17/21、NBT/组件、编解码及 import 差异。
- 保留 Forge 事件、Java 17、NBT 物品匹配、NeoBackports StreamCodec 和 KubeJS 6 插件接口。
- 构建环境使用 mise；主工程在独立目录验证，通过 Gradle `projectDir` 直接引用此工作区，避免复制源码后验证了旧文件。

## 实际回迁

| 功能 | 1.20.1 实现 |
| --- | --- |
| 食物/饮品目标按手部筛选 | 恢复蓝图 `use_hand`、编译字段、持久化字段、运行时筛选及提示；旧数据默认 ANY |
| 完成使用事件的手部 | Forge 事件边界读取 `player.getUsedItemHand()`；不照搬 NeoForge 的 `event.getHand()` |
| 旧 API 兼容 | 保留两参数 `matches`、`recordFoodEaten`，委托给新实现 |
| 计分板持有者 | 读取和修改均遵循配置的持有者；空值使用当前玩家；读取不存在的分数返回 0 且不创建记录 |
| 非经典主题状态标签 | 使用当前主题的正常、悬停、按下材质与文字颜色；经典书签保留原布局 |
| 客户端配置入口 | Forge 模组列表配置按钮打开本地配置页；支持布尔、枚举、数值和分页，先验证所有值再保存；取消不写入 |
| 公共状态初始化 | TaskStatus 直接构造资源 ID，避免为状态图标初始化模组入口类 |
| 无显示信息的进度目标 | 先判断 Advancement 的显示信息，再读取图标；空图标回退知识之书，保持主分支行为 |
| AI 编辑回归场景 | 回迁 `quest_ai_adapter`，覆盖主图/子图读取、编辑、编译、预览、放弃、撤销与重做 |
| AI 场景测试底座 | 主工程 1.20.1 缺少测试基类，按主工程 master 的 `4ddbf6c` 引入模组内开发夹具；虚拟线程替换为 Java 17 线程池，结束时关闭 |
| UI 测试 Gradle 桥 | 改为 `legacyForge`，测试使用隔离游戏目录，失败或缺失报告使任务失败 |

分支原有的任务流程、子任务、分支/汇合、变量、目标后动作、任务书、HUD、KubeJS 事件和上传权限校验继续沿用。
主分支的 NeoForge 原生配置屏幕使用 Forge 对应实现，界面布局不要求逐像素相同。
不迁移无业务变化的 import 整理、Java 21 集合便捷 API 和 1.21 数据组件 API。

## 准备与构建

从本模组根目录执行。需要 mise，先检查配置，再运行 `mise install`。
本仓库仍是子模块，不能脱离主工程单独构建。
Java 17 用于模组源码与游戏；当前 ModDev/NeoForm 构建工具另需 Java 21，mise 同时安装两者，构建脚本显式提供 toolchain 路径。

1. 获取 ViScriptLib 的 `1.20.1` 分支并初始化匹配的子模块。
2. 确认主工程有配套的 LDLib2 Forge 2.2.45 开发包及 NeoBackports 1.4.2。
3. 主工程子项目名称为 `ViScriptQuest`（单数），将它的 `projectDir` 指向当前仓库：

   ```groovy
   project(':ViScriptQuest').projectDir = file('/absolute/path/to/ViScriptQuests')
   ```

4. 主工程 `build.gradle` 中本地 LDLib2 路径应使用 `rootProject.file('libs/ldlib2-forge-1.20.1-2.2.45-dev.jar')`。
   原来的相对 `files('libs/...')` 按子项目解析，外置工作区会找不到该包。
5. 在本模组根目录创建被忽略的 `mise.local.toml`：

   ```toml
   [env]
   VISCRIPT_WORKSPACE = "D:/work/ViScriptLib"
   ```

6. 执行：

   ```powershell
   mise run build
   mise run verify
   mise run verify:ui
   ```

`verify` 在真实项目编译类路径上检查指定/空白持有者、读操作不创建分数和手部筛选。
`verify:ui` 启动图形客户端，在隔离世界运行 AI 测试，使用本机确定性协议夹具，不需要真实模型密钥。
不要在玩家世界里执行开发场景。

产物沿用主工程目录：`build/ViScriptQuest/libs/`；UI 报告在 `build/ViScriptQuest/ldlib2-uitest/`。
主工程及依赖变更只在本地验证目录进行，本次未提交到主工程远端。

## 游戏内验收与服务器边界

- 旧工程/旧存档的食用目标仍接受两只手；新目标分别验证主手、副手、食物和饮品。
- 使用不同玩家分数与剧情持有者分数验证条件分支和修改节点。
- 检查经典与非经典主题的状态过滤按钮、切换和文字显示。
- 在模组列表打开配置，验证数值边界、取消、保存、分页和重启后的保留。
- AI 场景完成后检查 `report.json`，不能用成功编译替代 UI 通过。
- 专用服务器启动时确认不会加载客户端配置页或开发场景。
- 1.20.1 模组端已具备新剧情接入所需的原生数据类、目标类型和 KubeJS 6 边界；
  SAO 服务器侧的 NPC、钱包和旧 FTB 兼容改动在独立仓库开发，不能随本模组提交自动部署。

正式部署仍需测试 Forge 47.4.10、完整客户端模组集、重登与重启保存行为；不自动提交、推送或部署。

## 实际验证结果（2026-11-11）

- `mise run verify`：通过，真实 1.20.1 计分板与手部筛选回归运行成功。
- `mise run build`：通过，包含全部源码编译、资源处理、回归检查、JAR 与重混淆。
- `git diff --check`：通过；语言 JSON 解析及新启用提示键完整性检查通过。
- 本地个人配置 `mise.local.toml` 已确认被 Git 忽略；SAO 服务器仓库保持无改动。
- 构建使用主工程 Forge 47.1.30 基线；目标整合包 Forge 47.4.10 的运行兼容性尚未实测。
- `verify:ui` 的代码已编译通过，但未启动图形客户端执行，不能视为 UI 场景通过。
- 模组列表配置界面、实际使用物品事件、专用服务器启动与完整剧情闭环尚待游戏内验收。

2026-11-11 复跑 `mise run verify` 仍通过。SAO 隔离 Forge GameTest 随后使用本分支 JAR
验证了 44 个原生剧情任务、203 个阶段和 207 个目标，包括新增食物/饮品使用目标的压缩 NBT 往返；
真实 KubeJS 6 服务端加载、重复安装和外部分类保留也通过。测试使用 Forge 47.1.30；
Forge 47.4.10 完整整合包、客户端图形界面和全部剧情玩法绑定仍未验收。
低内存 Windows 环境可从 SAO 仓库以 `-TestHeap 1024m` 限制隔离 GameTest 堆并启用 Serial GC；
该选项只由服务器源仓库的测试 init script 注入，不改变模组生产启动参数。

排障过程：Gradle wrapper 下载超时后改用 mise 固定 Gradle 8.12.1；依赖直连失败后在本地验证环境使用已有代理；
NeoForm 要求 Java 21，补充工具链并显式传入安装路径；测试源码默认缺少 Minecraft 类路径，补齐与主源码的编译及运行类路径后重跑成功。
上述失败尝试没有被记为通过。主工程本地路径及代理值未写入公共配置。


## SAO 剧情接入时的服务端实测修复

隔离 Forge 47.1.30 GameTest 发现公共模组入口内声明的配置界面 lambda 会令专用服务端加载客户端 Screen。
现已把客户端配置与界面注册提取到 `ViScriptQuestsClient`，公共入口仅在客户端分支调用其无参方法。
修复后真实 Forge 服务端成功启动，SAO 的 44 个原生任务、203 个阶段、207 个目标通过原生校验与压缩 NBT 往返验收。
测试由服务器源仓库的 `mise run verify:story-forge` 驱动，使用 build 下的隔离世界；不代表完整整合包、客户端 UI 或全部剧情玩法已验收。
