package com.gmail.mrphpfan;

public class UpdateTask implements Runnable {
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
