package com.gmail.mrphpfan;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Logger;

public class EcoLeaderboards extends JavaPlugin implements Listener {
	private static final Logger log = Bukkit.getLogger();
	private static Economy econ = null;
	private Bal[] top10 = new Bal[11];

	private ArrayList<Leaderboard> allBoards = new ArrayList<>();

	private int tickUpdateInterval = 1200;
	private ArrayList<String> excludedPlayers = new ArrayList<>();

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);

		if(!setupEconomy()) {
			log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		loadConfiguration();
		loadSigns();

		//initially do a balance ranking
		rankBalances();

		//update the signs every so often
		Bukkit.getScheduler().runTaskTimer(this, new UpdateTask(this), tickUpdateInterval, tickUpdateInterval);

		getLogger().info("EcoLeaderboards Enabled.");
	}

	@Override
	public void onDisable() {
		saveSigns();
		getLogger().info("EcoLeaderboards Disabled.");
	}

	public boolean setupEconomy() {
		if(getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if(rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public void loadConfiguration() {
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();

		if(this.getConfig().get("update_interval_seconds") == null) {
			this.getConfig().addDefault("update_interval_seconds", 60);
			this.saveConfig();
		}

		if(this.getConfig().get("excluded_players") == null) {
			ArrayList<String> exclPlayerDefault = new ArrayList<>();
			this.getConfig().addDefault("excluded_players", exclPlayerDefault);
			this.saveConfig();
		}

		excludedPlayers = (ArrayList<String>) this.getConfig().getStringList("excluded_players");
		if(excludedPlayers == null) {
			excludedPlayers = new ArrayList<>();
			getLogger().warning("Invalid excluded_players config section or not found.");
		}

		int secondsInterval = this.getConfig().getInt("update_interval_seconds");
		if(secondsInterval <= 0) {
			tickUpdateInterval = 60 * 20;
			getLogger().warning("Invalid update_interval_seconds config section or not found. Defaulting to 60.");
		}
		else {
			//convert seconds to server ticks (1 second = 20 ticks)
			tickUpdateInterval = secondsInterval * 20;
		}
	}

	private void loadSigns() {
		File signFile = new File("plugins/EcoLeaderboards/signs.txt");
		if(signFile.exists()) {
			try {
				Scanner scanner1 = new Scanner(signFile);

				while(scanner1.hasNextLine()) {
					String locString = scanner1.nextLine();

					//check if invalid line / empty line (no way a valid line can be below 5 characters
					if(locString.length() <= 5) {
						continue;
					}

					String[] parts = locString.split(":");
					String worldName = parts[0];
					World world = Bukkit.getWorld(worldName);
					double posX = Double.parseDouble(parts[1]);
					double posY = Double.parseDouble(parts[2]);
					double posZ = Double.parseDouble(parts[3]);
					Location loc = new Location(world, posX, posY, posZ);

					int rank = Integer.parseInt(parts[4]);
					Leaderboard newBoard = new Leaderboard(loc, rank);
					allBoards.add(newBoard);
				}
			}
			catch(FileNotFoundException e) {
				e.printStackTrace();
			}

		}
	}

	public void saveSigns() {
		File signFile = new File("plugins/EcoLeaderboards/signs.txt");



		try {
			if(!signFile.exists() && !signFile.createNewFile()) {
				log.severe("Failed to create signs file");
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		try {
			PrintWriter out = new PrintWriter(signFile);
			for(Leaderboard l : allBoards) {
				Location loc = l.getLocation();
				//format: world:x:y:z:rank
				String ln = loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + l.getRank();
				out.println(ln);
			}
			out.close();
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		Player player = event.getPlayer();
		if(event.isCancelled()) {
			return;
		}

		if(player.hasPermission("EcoLeaderboards.place")) {
			String line0 = event.getLine(0);
			if(line0.startsWith("[el")) {
				try {
					int number = Integer.parseInt(line0.substring(3, line0.indexOf("]")));
					Bal balRank = atPosition(number);

					if(balRank == null) {
						return;
					}

					Location loc = event.getBlock().getLocation();
					Leaderboard leaderboard = new Leaderboard(loc, number);
					leaderboard.update(event, balRank.getName(), balRank.getBalance());

					//check if there's already a board at this location, remove that board if so
					int removeIndex = -1;
					for(int i = 0; i < allBoards.size(); i++) {
						if(allBoards.get(i) != null && allBoards.get(i).getLocation().equals(loc)) {
							//the locations are the same, remove that board (mark it for removal)
							removeIndex = i;
							break;
						}
					}
					if(removeIndex != -1) {
						allBoards.remove(removeIndex);
					}

					allBoards.add(leaderboard);
				}
				catch(Exception e) {
					player.sendMessage(ChatColor.GOLD + "First line must be [el#], replace # with a number 1-10");
				}
			}
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		String cmdName = cmd.getName();
		if(cmdName.equalsIgnoreCase("el")) {
			if(args.length == 1) {
				if(args[0].equalsIgnoreCase("reload")) {
					if(sender.hasPermission("EcoLeaderboards.reload")) {
						rankBalances();
						sender.sendMessage(ChatColor.GOLD + "Balances being ranked.");
					}
					else {
						sender.sendMessage(ChatColor.RED + "No permission.");
					}
					return true;
				}
				else if(args[0].equalsIgnoreCase("position")) {
					//they forgot a position
					sender.sendMessage(ChatColor.GOLD + "Usage: /el position [number(1-10)].");
					return true;
				}
				else if(args[0].equalsIgnoreCase("top")) {
					//print all the top balances
					if(sender.hasPermission("EcoLeaderboards.position")) {
						sender.sendMessage(ChatColor.GOLD + "Top 10 balances:");
						for(int i = 1; i <= 10; i++) {
							Bal thisBal = atPosition(i);
							if(thisBal != null) {
								sender.sendMessage(ChatColor.GOLD + "" + i + ": " + ChatColor.GREEN + thisBal.getName() + ": " + ChatColor.RED + "$" + thisBal.getBalance());
							}
						}
					}
					return true;
				}
			}
			else if(args.length == 2) {
				if(args[0].equalsIgnoreCase("position")) {
					if(sender.hasPermission("EcoLeaderboards.position")) {
						Bal thisBal = atPosition(Integer.parseInt(args[1]));
						if(thisBal != null) {
							sender.sendMessage(ChatColor.GOLD + args[1] + ": " + ChatColor.GREEN + thisBal.getName() + ": " + ChatColor.RED + "$" + thisBal.getBalance());
						}
						else {
							sender.sendMessage(ChatColor.GOLD + "Position specified must be 1-10");
						}
					}
					else {
						sender.sendMessage(ChatColor.RED + "No permission.");
					}
					return true;
				}
			}
			else {
				return false;
			}
		}
		else {
			sender.sendMessage(ChatColor.GOLD + "Invalid EcoLeaderboards command.");
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	public void rankBalances() {
		//strategy - maintain a top 10 list as sorting progresses, add a new balance to the 11th spot, sort the array, etc.

		//first get every user that has ever logged into the server
		OfflinePlayer[] players = getServer().getOfflinePlayers();

		for(int i = 0; i < top10.length; i++) {
			top10[i] = new Bal("", 0.0);
		}

		playerloop:
		for(OfflinePlayer player : players) {
			String name = player.getName();

			//make sure it's not in the excluded players list
			for(String excludedPlayer : excludedPlayers) {
				if(name.equalsIgnoreCase(excludedPlayer)) {
					//dont use this name in the ranking
					continue playerloop;
				}
			}

			double balance = econ.getBalance(name);
			top10[10] = new Bal(name, balance);
			Arrays.sort(top10);
		}

		//update all existing boards
		updateBoards();
	}

	private Bal atPosition(int position) {
		//specify 1-10
		if(position >= 1 && position <= 10) {
			return top10[position - 1];
		}
		else {
			return null;
		}
	}

	private void updateBoards() {
		//update signs with balances from top10
		ArrayList<Leaderboard> newBoards = new ArrayList<>();

		for(Leaderboard thisBoard : allBoards) {
			boolean updateSuccess = thisBoard.update(top10[thisBoard.getRank() - 1].getName(), top10[thisBoard.getRank() - 1].getBalance());
			if(updateSuccess) {
				newBoards.add(thisBoard);
			}
		}
		allBoards = newBoards;
	}
}
