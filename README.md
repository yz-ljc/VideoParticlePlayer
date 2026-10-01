##### AI代码随便看看得了，没啥技术玩意，我纯整活玩的
##### 推荐的视频模板（BadApple）：https://www.bilibili.com/video/BV1xx411c79H
- - -
# PlayVideo 插件

这是一个适用于 Paper 26.3（Java 25）服务器的 Minecraft 插件，允许在游戏内用粒子或坐垫播放视频帧。

## 特性

- 支持多视频加载与缓存，切换播放更高效
- 灵活选择播放帧率 (FPS)
- 坐垫模式：在玩家面前的平坦地面生成低分辨率画面，结束或停止时自动清理

## 安装方法

1. 下载编译好的 jar 文件
2. 放入你的服务器 `/plugins/` 目录
3. 重启服务器自动生成插件文件夹

## 开发运行

使用 Java 25，在项目目录执行 `./gradlew runDevBundleServer`（Windows 使用 `./gradlew.bat runDevBundleServer`），即可从项目的 `run/` 目录启动 Paper 26.3 开发服务器并加载当前插件。启动前先关闭仍在使用这个目录的服务器。

## 视频放置

将视频文件放到插件生成的数据文件夹 (`plugins/VideoParticlePlayer/`) 下。解码由 JCodec 完成，建议使用兼容的 MP4 文件。

## 指令用法

```
/pv load <文件名>
```  
- 说明：加载指定视频文件到内存，加快后续播放速度
- 示例：`/pv load badapple.mp4`

```
/pv play <文件名> [FPS]
```  
- 说明：用 100 像素宽的粒子画面播放指定视频；可选参数 FPS 默认为 30
- 示例：`/pv play badapple.mp4 24`

```
/pv play <文件名> cushion [FPS] [宽度]
```
- 说明：坐垫模式默认使用视频原始分辨率，默认 20 FPS；可选的宽度参数会按比例缩放画面
- 坐垫按批生成，不设置人为数量上限；视频帧在后台读取，停止播放时会清理已生成的坐垫
- Minecraft 每秒最多显示 20 次画面更新，高于 20 FPS 时会跳过部分源帧
- 站在一块足够大的平坦完整方块地面旁，面向希望画面延伸的方向执行命令
- 示例：`/pv play badapple.mp4 cushion 15`（原画）或 `/pv play badapple.mp4 cushion 15 16`（缩到 16 格宽）
- 本项目示例 `badapple.mp4` 为 512×384；原画播放需 196,608 个坐垫和同样大小的平坦地面，会对服务器与客户端造成很大压力

```
/pv stop
```  
- 说明：停止当前正在播放的视频
- 坐垫模式会同时移除本次播放生成的坐垫

## 权限

- `playvideo.use` —— 允许玩家加载/播放/停止视频

## 示例流程

1. 上传视频至 `plugins/VideoParticlePlayer/` 目录
2. 在游戏内输入 `/pv load badapple.mp4`
3. 等待提示“加载完成”
4. 输入 `/pv play badapple.mp4 30` （或省略 FPS，默认30fps）
5. 输入 `/pv stop` 停止播放

## 常见问题

- **Q：支持什么格式？**  
  A：使用 JCodec 解码，建议使用兼容的 MP4 文件
- **Q：支持多视频吗？**  
  A：支持！可加载多个视频切换播放，不用重复加载
- **Q：播放会卡服吗？**  
  A：视频在后台加载；坐垫生成和每帧颜色更新仍在服务器主线程执行。原画尺寸越大，负载越高

## 开发 & 贡献

欢迎 PR 和 Issue 如果你有想法或发现 bug！

## 声明

本插件为开源学习项目，与 Mojang 或 Microsoft 官方无关
