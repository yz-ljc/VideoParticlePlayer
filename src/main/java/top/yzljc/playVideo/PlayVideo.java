package top.yzljc.playVideo;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.yzljc.playVideo.video.VideoCache;
import top.yzljc.playVideo.video.VideoPlayerTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PlayVideo extends JavaPlugin implements CommandExecutor {

    private final Map<String, VideoCache> loadedVideos = new HashMap<>();
    private VideoPlayerTask currentTask;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        if (getCommand("playvideo") != null) {
            getCommand("playvideo").setExecutor(this);
        }

        getLogger().info("插件已加载！支持多视频播放。");
        getLogger().info("使用方法: /pv load <文件名> | /pv play <文件名> [FPS]");
    }

    @Override
    public void onDisable() {
        stopCurrentTask();
        loadedVideos.clear();
    }

    private void stopCurrentTask() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("playvideo.use")) {
            sender.sendMessage("§c你没有权限这样做！");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§c用法: /pv <load|play|stop> [文件名] [FPS]");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "load" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c请指定要加载的文件名！例如: /pv load badapple.mp4");
                    return true;
                }
                String fileName = args[1];
                File videoFile = new File(getDataFolder(), fileName);

                if (!videoFile.exists()) {
                    sender.sendMessage("§c未在插件目录找到文件: " + fileName);
                    return true;
                }

                if (loadedVideos.containsKey(fileName)) {
                    sender.sendMessage("§e视频 " + fileName + " 已经在内存中了，正在重新加载...");
                }

                sender.sendMessage("§a开始加载视频: " + fileName + " (异步处理中...)");

                // 异步加载
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    try {
                        VideoCache cache = new VideoCache(this, videoFile, 100);

                        getServer().getScheduler().runTask(this, () -> {
                            loadedVideos.put(fileName, cache);
                            sender.sendMessage("§a视频 " + fileName + " 加载完成！");
                            sender.sendMessage("§a总帧数: " + cache.getTotalFrames() + " | 分辨率: " + cache.getWidth() + "x" + cache.getHeight());
                            sender.sendMessage("§a输入 /pv play " + fileName + " 即可播放");
                        });
                    } catch (Exception e) {
                        sender.sendMessage("§c加载失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                return true;
            }


            case "play" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c控制台无法播放视频。");
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("§c请指定要播放的视频名！已加载: " + loadedVideos.keySet());
                    return true;
                }

                String fileName = args[1];
                VideoCache cache = loadedVideos.get(fileName);

                if (cache == null) {
                    sender.sendMessage("§c视频尚未加载！请先使用 /pv load " + fileName);
                    return true;
                }

                double fps = 30.0;
                if (args.length >= 3) {
                    try {
                        fps = Double.parseDouble(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cFPS 参数错误，使用默认值 30");
                    }
                }

                stopCurrentTask();

                double spacing = 0.25;

                currentTask = new VideoPlayerTask(cache, player.getLocation().add(0, 3, 0), spacing, fps);
                currentTask.runTaskTimer(this, 0L, 1L);

                sender.sendMessage("§a开始播放: " + fileName + " (FPS: " + fps + ")");
                return true;
            }
            case "stop" -> {
                if (currentTask != null) {
                    stopCurrentTask();
                    sender.sendMessage("§a已停止播放。");
                } else {
                    sender.sendMessage("§c当前没有正在播放的视频。");
                }
                return true;
            }
        }

        return false;
    }
}