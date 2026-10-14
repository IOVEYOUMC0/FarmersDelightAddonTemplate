# 模板当前接口核对

日期：2026-10-15。

- 双语 README 改用当前调试语法、/fd stats addon，以及 supportsPlacement / supportsActivation / activateAt 按位置调用。all 仅覆盖支持当前操作的目标，并受玩家调试批次约束；不再推荐旧 activate(player) 或不存在的 debugtools status。
- 配方扩展示例说明完整原料、液体输出和无配方功能激活；经验入口说明原版/CE Leveler，由 FD 配置决定。
- CE 编译26.9.1，运行核对26.8.2～26.10；类名相同不等于API兼容。示例删除未使用插件字段和过时长注释。
- clean build：3 suites / 4项 JUnit，零失败、错误、跳过。FD API 边界检查通过，模板产物纳入 CE 七组符号检查，零缺失类/成员/抽象缺口。
- 原有5项用户修改逐文件SHA256与开始基线一致，不纳入此次提交。

构建使用本地同级 FD composite 的 API 制品；未测试无同级源码时的远程 Gradle source dependency，也未将示例模板部署到测试服。
