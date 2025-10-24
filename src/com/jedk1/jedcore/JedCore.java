package com.jedk1.jedcore;


import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import com.google.common.reflect.ClassPath;
import com.jedk1.jedcore.util.*;
import com.jedk1.jedcore.command.Commands;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.listener.AbilityListener;
import com.jedk1.jedcore.listener.CommandListener;
import com.jedk1.jedcore.listener.JCListener;
import com.jedk1.jedcore.util.ChiRestrictor;
import com.jedk1.jedcore.util.versionadapter.ParticleAdapter;
import com.jedk1.jedcore.util.versionadapter.ParticleAdapterFactory;
import com.jedk1.jedcore.util.versionadapter.PotionEffectAdapter;
import com.jedk1.jedcore.util.versionadapter.PotionEffectAdapterFactory;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Logger;

public class JedCore extends JavaPlugin {

	public static JedCore plugin;
	public static Logger log;
	public static String dev;
	public static String version;
	public static boolean logDebug;
	public static boolean isFolia;
	public static boolean luminol;
	public static boolean paper;
    public static boolean spigot;
    public static ServerImplementation scheduler;

	private ParticleAdapter particleAdapter;
	private PotionEffectAdapter potionEffectAdapter;

	@Override
	public void onEnable() {
        scheduler = new FoliaCompatibility(this).getServerImplementation();
		plugin = this;
		log = this.getLogger();
		new JedCoreConfig(this);

		logDebug = JedCoreConfig.getConfig((World) null).getBoolean("Properties.LogDebug");

		dev = this.getDescription().getAuthors().toString().replace("[", "").replace("]", "");
		version = this.getDescription().getVersion();

		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
			isFolia = true;
		} catch (ClassNotFoundException ignored) {}

		try {
			Class.forName("com.destroystokyo.paper.PaperConfig");
			paper = true;
		} catch (ClassNotFoundException ignored) {}

		try {
			Class.forName("me.earthme.luminol.api.ThreadedRegion");
			luminol = true;
		} catch (ClassNotFoundException ignored) {}

		if (isFolia && !luminol) {
				getLogger().info("Server is running on Folia");
		}

		if (paper && !luminol) {
				getLogger().info("Server is running on Paper");
		}

		if (luminol) {
				getLogger().info("Server is running on Luminol");
		}

        if (!luminol && !isFolia && !paper) {
            spigot = true;
            getLogger().info("Server is running on Spigot");
        }

		JCMethods.registerDisabledWorlds();
		CoreAbility.registerPluginAbilities(this, "com.jedk1.jedcore.ability");

		getServer().getPluginManager().registerEvents(new AbilityListener(this), this);
		getServer().getPluginManager().registerEvents(new CommandListener(this), this);
		getServer().getPluginManager().registerEvents(new JCListener(this), this);
		getServer().getPluginManager().registerEvents(new ChiRestrictor(), this);

		// Repeating logic task - uses ThreadUtil
		ThreadUtil.runGlobalTimer(new JCManager(this), 0L, 1L);

		new Commands();
		FireTick.loadMethod();

		particleAdapter = new ParticleAdapterFactory().getAdapter();
		potionEffectAdapter = new PotionEffectAdapterFactory().getAdapter();

		// Delayed combo/collision init
		ThreadUtil.runGlobalLater(() -> {
			JCMethods.registerCombos();
			initializeCollisions();
		}, 1L);


        try {
            MetricsLite metrics = new MetricsLite(this);
            metrics.start();
            log.info("Initialized Metrics.");
        } catch (IOException e) {
            log.info("Failed to submit statistics for MetricsLite.");
        }
	}

	public void initializeCollisions() {
		boolean enabled = getConfig().getBoolean("Properties.AbilityCollisions.Enabled");
		if (!enabled) {
			getLogger().info("Collisions disabled.");
			return;
		}

		try {
			ClassPath cp = ClassPath.from(this.getClassLoader());

			for (ClassPath.ClassInfo info : cp.getTopLevelClassesRecursive("com.jedk1.jedcore.ability")) {
				try {
					@SuppressWarnings("unchecked")
					Class<? extends CoreAbility> abilityClass = (Class<? extends CoreAbility>) Class.forName(info.getName());
					if (abilityClass == null) continue;

					CollisionInitializer initializer = new CollisionInitializer<>(abilityClass);
					initializer.initialize();
				} catch (Exception ignored) {}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		RegenTempBlock.revertAll();
	}

	public static void logDebug(String message) {
		if (logDebug) {
			plugin.getLogger().info(message);
		}
	}

	public ParticleAdapter getParticleAdapter() {
		return particleAdapter;
	}

	public PotionEffectAdapter getPotionEffectAdapter() {
		return potionEffectAdapter;
	}

	public static boolean isFolia() {
		return isFolia;
	}

	public static boolean isPaper() {
		return paper;
	}

	public static boolean isLuminol() {
		return luminol;
	}

    public static boolean isSpigot() {
        return spigot;
    }
}
