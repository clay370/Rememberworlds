我将按照用户要求修改 app/build.gradle.kts 文件，具体修改内容如下：

1. **修改 plugins 块**：

   * 将 Google 服务插件版本从 4.4.2 改为 4.4.0

2. **修改 dependencies 块**：

   * 删除 LeanCloud 相关依赖

   * 添加 Firebase 相关依赖，使用 BOM 管理版本

3. **根目录 build.gradle.kts**：不需要修改，因为使用的是现代 Gradle

