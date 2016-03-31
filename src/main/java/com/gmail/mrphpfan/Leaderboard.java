package com.gmail.mrphpfan;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.block.SignChangeEvent;

public class Leaderboard {
	private Location loc;
	private int rank;
	private String name;
	private double balance;
	
	public Leaderboard(Location loc, int rank){
		this.loc = loc;
		this.rank = rank;
	}
	
	public Location getLocation(){
		return loc;
	}
	
	public int getRank(){
		return rank;
	}
	
	public boolean update(String newName, double newBal){
		//return true if update successful, sign will stay updated
		//return false if update not successful, sign will not stay updated.
		name = newName;
		balance = newBal;
		
		Block signBlock = loc.getBlock();
		Material type = signBlock.getType();
		if(type == Material.SIGN || type == Material.SIGN_POST || type == Material.WALL_SIGN){
			Sign s = (Sign)signBlock.getState();
			//check 1 final time that this is actually a rank sign
			if(s.getLine(0).startsWith("#" + rank)){
				s.setLine(0, "#" + rank);
				s.setLine(1, ChatColor.DARK_RED + name);
				
				//balances displayed as a double over 10 million will display in scientific notation. Convert to integer if over 10mil
				if(balance < 10000000){
					s.setLine(2, ChatColor.DARK_PURPLE + "$" + balance);
				}else{
					s.setLine(2, ChatColor.DARK_PURPLE + "$" + (int)balance);
				}
				s.update();
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	public void update(SignChangeEvent event, String newName, double newBal){
		name = newName;
		balance = newBal;
		
		event.setLine(0, "#" + rank);
		event.setLine(1, ChatColor.DARK_RED + name);
		
		//balances displayed as a double over 10 million will display in scientific notation. Convert to integer if over 10mil
		if(balance < 10000000){
			event.setLine(2, ChatColor.DARK_PURPLE + "$" + balance);
		}else{
			event.setLine(2, ChatColor.DARK_PURPLE + "$" + (int)balance);
		}
	}
}
