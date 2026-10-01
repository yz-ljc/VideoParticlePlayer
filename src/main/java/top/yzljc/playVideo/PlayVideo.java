package top.yzljc.playVideo;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import top.yzljc.playVideo.video.CushionVideoPlayerTask;
import top.yzljc.playVideo.video.VideoCache;
import top.yzljc.playVideo.video.VideoPlayerTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PlayVideo extends JavaPlugin implements CommandExecutor {

    private final Map<String, VideoCache> loadedVideos = new HashMap<>();
    private BukkitRunnable currentTask;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        if (getCommand("playvideo") != null) {
            getCommand("playvideo").setExecutor(this);
        }

        getLogger().info("插件已加载！支持多视频播放。");
        getLogger().info("使用方法: /pv load <文件名> | /pv play <文件名> [FPS] | /pv play <文件名> cushion [FPS] [宽度]");
    }

    @Override
    public void onDisable() {
        stopCurrentTask();
        loadedVideos.clear();
    }

    private void stopCurrentTask() {
        if (currentTask != null) {
            if (currentTask instanceof CushionVideoPlayerTask cushionTask) {
                cushionTask.cleanup();
            }
            if (!currentTask.isCancelled()) {
                currentTask.cancel();
            }
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
            sender.sendMessage("§c用法: /pv load <文件名> | /pv play <文件名> [FPS] | /pv play <文件名> cushion [FPS] [宽度] | /pv stop");
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
                            sender.sendMessage("§a总帧数: " + cache.getTotalFrames() + " | 原画: " + cache.getSourceWidth() + "x" + cache.getSourceHeight() + " | 粒子: " + cache.getWidth() + "x" + cache.getHeight());
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

                boolean cushionMode = args.length >= 3 && args[2].equalsIgnoreCase("cushion");
                int fpsArg = cushionMode ? 3 : 2;
                double fps = cushionMode ? 20.0 : 30.0;
                if (args.length > fpsArg) {
                    try {
                        fps = Double.parseDouble(args[fpsArg]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cFPS 必须是数字。");
                        return true;
                    }
                }
                if (!Double.isFinite(fps) || fps <= 0) {
                    sender.sendMessage("§cFPS 必须大于 0。");
                    return true;
                }

                int cushionWidth = cache.getSourceWidth();
                if (cushionMode && args.length >= 5) {
                    try {
                        cushionWidth = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§c坐垫画面宽度必须是整数。");
                        return true;
                    }
                }

                stopCurrentTask();

                if (cushionMode) {
                    CushionVideoPlayerTask task = null;
                    try {
                        task = new CushionVideoPlayerTask(cache, player, fps, cushionWidth);
                        task.prepare();
                        task.runTaskTimer(this, 0L, 1L);
                        currentTask = task;
                        sender.sendMessage("§a正在准备坐垫画面: " + fileName + " (" + task.getWidth() + "x" + task.getHeight() + ", " + fps + " FPS)");
                    } catch (RuntimeException e) {
                        if (task != null) {
                            task.cleanup();
                        }
                        sender.sendMessage("§c坐垫播放失败: " + e.getMessage());
                        getLogger().warning("坐垫播放失败: " + e.getMessage());
                    }
                    return true;
                }

                double spacing = 0.25;
                VideoPlayerTask task = new VideoPlayerTask(cache, player.getLocation().add(0, 3, 0), spacing, fps);
                task.runTaskTimer(this, 0L, 1L);
                currentTask = task;

                sender.sendMessage("§a开始播放: " + fileName + " (FPS: " + fps + ")");
                return true;
            }
            case "stop" -> {
                if (currentTask != null && !currentTask.isCancelled()) {
                    stopCurrentTask();
                    sender.sendMessage("§a已停止播放。");
                } else {
                    stopCurrentTask();
                    sender.sendMessage("§c当前没有正在播放的视频。");
                }
                return true;
            }
        }

        return false;
    }
}
