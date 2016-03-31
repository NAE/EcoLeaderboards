package com.gmail.mrphpfan;

import org.bukkit.scheduler.BukkitRunnable;

public class UpdateTask extends BukkitRunnable {

	private final EcoLeaderboards plugin;
	 
    public UpdateTask(EcoLeaderboards plugin) {
        this.plugin = plugin;
    }
	
	@Override
	public void run() {
		plugin.rankBalances();
		plugin.saveSigns();
	}

}
