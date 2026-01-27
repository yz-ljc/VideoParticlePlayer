##### AI代码随便看看得了，没啥技术玩意，我纯整活玩的
- - -
## /badapple 指令用法

| 指令                       | 权限节点        | 说明                                    |
|---------------------------|----------------|-----------------------------------------|
| `/badapple load`          | badapple.use   | 加载视频文件（请先放置 video.mp4 到插件目录）。加载成功后会缓存帧数据。|
| `/badapple play`          | badapple.use   | 在玩家当前位置上方(竖版)开始播放已加载的视频。|
| `/badapple stop`          | badapple.use   | 停止当前正在播放的视频。                |

### 使用流程

1. 将你要播放的 `video.mp4` 放到插件生成的目录（通常为 `/plugins/BadApple/`）下，并确保文件名为 `video.mp4`。
2. 输入 `/badapple load` 加载并缓存视频。**等待“视频加载完成，总计帧数: xxx”提示。**
3. 在游戏内你想播放的位置输入 `/badapple play`，会在你头顶上播放该视频。
4. 如需强制终止播放，可输入 `/badapple stop`。

> **注意：**  
> 所有 /badapple 子命令都要求权限节点 `badapple.use` 才可用。
