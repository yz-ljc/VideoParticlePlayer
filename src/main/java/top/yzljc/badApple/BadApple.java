package top.yzljc.badApple;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.yzljc.badApple.video.VideoCache;
import top.yzljc.badApple.video.VideoPlayerTask;

import java.io.File;

public class BadApple extends JavaPlugin implements CommandExecutor {

    private VideoCache videoCache;
    private VideoPlayerTask currentTask;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        getCommand("badapple").setExecutor(this);
        getLogger().info("插件已加载，请将视频文件命名为 'video.mp4' 并放入插件目录，然后使用 /badapple load 加载视频！");
    }

    @Override
    public void onDisable() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("badapple.use")) {
            sender.sendMessage("§c你没有权限这样做！"); // 毫无意义的鉴权，但是还是写吧
            return true;
        }

        if (args.length == 0) return false;

        if (args[0].equalsIgnoreCase("load")) {
            File videoFile = new File(getDataFolder(), "video.mp4");
            if (!videoFile.exists()) {
                sender.sendMessage("§c没有找到video.mp4文件！请将视频文件放入插件目录并重命名为video.mp4");
                return true;
            }

            if (videoCache != null) {
                sender.sendMessage("§e正在清理先前的视频缓存...");
            }

            sender.sendMessage("§aStart loading video... This may take a while.");
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    videoCache = new VideoCache(videoFile, 100);
                    sender.sendMessage("§a视频加载完成，总计帧数: " + videoCache.getTotalFrames());
                    sender.sendMessage("§a使用 /badapple play 在你当前位置上方播放视频(竖版)");
                } catch (Exception e) {
                    sender.sendMessage("§c在加载视频时出现问题: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("play")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c我寻思着你总不能在控制台放视频吧");
                return true;
            }

            if (videoCache == null) {
                sender.sendMessage("§c视频尚未缓存，请使用/badapple load 加载视频！");
                return true;
            }

            if (currentTask != null && !currentTask.isCancelled()) {
                currentTask.cancel();
            }

            double spacing = 0.3;
            currentTask = new VideoPlayerTask(videoCache, player.getLocation().add(0, 2, 0), spacing);
            currentTask.runTaskTimer(this, 0L, 1L);

            sender.sendMessage("§a开始播放！");
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            if (currentTask != null) {
                currentTask.cancel();
                currentTask = null;
                sender.sendMessage("§a已停止播放！");
            } else {
                sender.sendMessage("§c没有正在播放中的视频！");
            }
            return true;
        }

        return false;
    }
}