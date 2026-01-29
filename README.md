##### AI代码随便看看得了，没啥技术玩意，我纯整活玩的
##### 推荐的视频模板（BadApple）：https://www.bilibili.com/video/BV1xx411c79H
- - -
# PlayVideo 插件

这是一个适用于Paper 1.21.11服务器的 Minecraft 插件，允许在游戏内加载和播放视频帧！

## 特性

- 支持多视频加载与缓存，切换播放更高效
- 灵活选择播放帧率 (FPS)

## 安装方法

1. 下载编译好的 jar 文件
2. 放入你的服务器 `/plugins/` 目录
3. 重启服务器自动生成插件文件夹

## 视频放置

将你想要播放的 mp4 （或支持的格式）文件放到插件生成的数据文件夹 (`plugins/PlayVideo/`) 下

## 指令用法

```
/pv load <文件名>
```  
- 说明：加载指定视频文件到内存，加快后续播放速度
- 示例：`/pv load badapple.mp4`

```
/pv play <文件名> [FPS]
```  
- 说明：播放指定已加载视频可选参数 FPS（默认为 30）
- 示例：`/pv play badapple.mp4 24`

```
/pv stop
```  
- 说明：停止当前正在播放的视频

## 权限

- `playvideo.use` —— 允许玩家加载/播放/停止视频

## 示例流程

1. 上传视频至 `plugins/PlayVideo/` 目录
2. 在游戏内输入 `/pv load badapple.mp4`
3. 等待提示“加载完成”
4. 输入 `/pv play badapple.mp4 30` （或省略 FPS，默认30fps）
5. 输入 `/pv stop` 停止播放

## 常见问题

- **Q：支持什么格式？**  
  A：理论支持 ffmpeg 可识别的视频或图片序列
- **Q：支持多视频吗？**  
  A：支持！可加载多个视频切换播放，不用重复加载
- **Q：播放会卡服吗？**  
  A：异步加载不卡主线程，播放任务为独立调度

## 开发 & 贡献

欢迎 PR 和 Issue 如果你有想法或发现 bug！

## 声明

本插件为开源学习项目，与 Mojang 或 Microsoft 官方无关
