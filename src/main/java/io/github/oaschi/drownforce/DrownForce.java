package io.github.oaschi.drownforce;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class DrownForce extends JavaPlugin implements CommandExecutor, Listener{
	
	private DFLogger logger;
	private Map<UUID, Drown> drowningPlayers = new HashMap<>();
	private boolean usePerms;
	
	public static Permission perms = null;
	
	public static final String NO_PERMISSION = ChatColor.YELLOW + "You don't have the permission to do that!";
	public static final String PLAYER_NOT_FOUND = ChatColor.YELLOW + "Player not found!";
	public static final String PLAYER_DROWNING = ChatColor.GREEN + "is drowning!";
	public static final String PLAYER_UNDROWNED = ChatColor.GREEN + "Player undrowned!";
	public static final String PLAYER_CANT_DROWN = ChatColor.YELLOW + "This player can't drown!";
	public static final String PLAYER_NOT_DROWING = ChatColor.YELLOW + "This player isn't drowning!";
	public static final String PLAYER_ALREADY_DROWNING = ChatColor.YELLOW + "This player is already drowning!";
	
	@Override
	public void onEnable(){
		logger = new DFLogger(this);
		logger.info("Enabled!");
		usePerms = setupVault();
		setupMetrics();
		getServer().getPluginManager().registerEvents(this, this);
		getCommand("drown").setExecutor(this);
		getCommand("undrown").setExecutor(this);
	}
	
	@Override
	public void onDisable(){
		for(UUID id : drowningPlayers.keySet()){
			Player p = Bukkit.getPlayer(id);
			removeCurrentWaterBlock(p);
		}
		logger.info("Disabled!");
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(args.length != 1) return false;
		
		Player victim = getServer().getPlayer(args[0]);
		if(victim == null){
			logger.info(sender, PLAYER_NOT_FOUND);
			return true;
		}
		
		boolean canDrownVictim = !usePerms || perms.has(sender, "drownforce.drown");
		boolean isVictimImmune = false;
		if(usePerms){
			isVictimImmune = perms.has(victim, "drownforce.immune");
		}
		boolean isVictimDrowning = drowningPlayers.containsKey(victim.getUniqueId());
		
		if(label.equalsIgnoreCase("drown")){
			if(canDrownVictim){
				if(isVictimImmune){
					logger.info(sender, PLAYER_CANT_DROWN);
				}
				else if(isVictimDrowning){
					logger.info(sender, PLAYER_ALREADY_DROWNING);
				}
				else{
					drownPlayer(victim);
					logger.info(sender, ChatColor.BLUE + victim.getName() + PLAYER_DROWNING);
				}
			}
			else{
				logger.info(sender, NO_PERMISSION);
			}
		}
		else if(label.equalsIgnoreCase("drownforever") || label.equalsIgnoreCase("drownfe")){
			if(canDrownVictim){
				if(isVictimImmune){
					logger.info(sender, PLAYER_CANT_DROWN);
				}
				else if(isVictimDrowning){
					logger.info(sender, PLAYER_ALREADY_DROWNING);
				}
				else{
					drownPlayerForever(victim);
					logger.info(sender, ChatColor.BLUE + victim.getName() + PLAYER_DROWNING);
				}
			}
		}
		else{
			if(!usePerms || perms.has(sender, "drownforce.undrown")){
				if(!isVictimDrowning){
					logger.info(sender, PLAYER_NOT_DROWING);
				}
				else{
					undrownPlayer(victim);
					logger.info(sender, PLAYER_UNDROWNED);
				}
			}
			else{
				logger.info(sender, NO_PERMISSION);
			}
		}
		
		return true;
	}
	
	/**
	 * Stops drowning the player.
	 */
	private void undrownPlayer(Player player){
		removeCurrentWaterBlock(player);
		drowningPlayers.remove(player.getUniqueId());
	}
	
	/**
	 * Removes the water block that is currently sitting on the players head.
	 */
	private void removeCurrentWaterBlock(Player player){
		Location loc = getHeadLocation(player);
		Drown dro = drowningPlayers.get(player.getUniqueId());
		if(dro == null) return;
		Block previous = dro.getBlock();
		Block current = loc.getBlock();
		if(current.equals(previous)){
			loc.getBlock().setType(Material.AIR);
		}
	}
	
	/**
	 * Starts drowning a player and doesn't stop even if he respawns.
	 */
	private void drownPlayerForever(Player player){
		Drown dro = new Drown(true);
		dro.setRemainingAir(player.getRemainingAir());
		drowningPlayers.put(player.getUniqueId(), dro);
		coverHeadInWater(player);
	}
	
	/**
	 * Starts drowning a player.
	 */
	private void drownPlayer(Player player){
		Drown dro = new Drown();
		dro.setRemainingAir(player.getRemainingAir());
		drowningPlayers.put(player.getUniqueId(), dro);
		coverHeadInWater(player);
	}
	
	/**
	 * Removes previous water blocks if the position of the block
	 * has to be updated because the victim is moving.
	 */
	private void removePreviousWaterBlock(Player player){
		Location loc = getHeadLocation(player);
		Drown dro = drowningPlayers.get(player.getUniqueId());
		if(dro == null) return;
		Block previous = dro.getBlock();
		if(previous != null && !loc.getBlock().equals(previous)){
			previous.setType(Material.AIR);
		}
	}
	
	/**
	 * Returns the location of the players head.
	 */
	private Location getHeadLocation(Player player){
		if(player == null) return null;
		Location loc = player.getLocation();
		loc.setY(loc.getY() + 1.5);
		return loc;
	}
	
	/**
	 * Covers a players head in stationary water.
	 */
	private void coverHeadInWater(Player player){
		Location loc = getHeadLocation(player);
		if(loc.getBlock().getType() != Material.AIR) return;
		
		Block block = player.getWorld().getBlockAt(loc);	
		block.setType(Material.STATIONARY_WATER);
		Drown dro = drowningPlayers.get(player.getUniqueId());
		dro.setBlock(block);
	}
	
	@EventHandler
	private void onPlayerMove(PlayerMoveEvent event){
		if(event.isCancelled()) return;
		Player p = event.getPlayer();
		if(drowningPlayers.containsKey(p.getUniqueId())){
			Drown dro = drowningPlayers.get(p.getUniqueId());
			int lastRemainingAir = dro.getRemainingAir();
			if(p.getRemainingAir() > lastRemainingAir){
				p.setRemainingAir(lastRemainingAir);
			}
			else{
				dro.setRemainingAir(p.getRemainingAir());
			}
			removePreviousWaterBlock(p);
			coverHeadInWater(p);
			if(event.getTo().getY() > event.getFrom().getY()){
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler
	private void onPlayerLeave(PlayerQuitEvent event){
		Player p = event.getPlayer();
		removeCurrentWaterBlock(p);
	}
	
	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent event){
		Player p = event.getPlayer();
		if(drowningPlayers.containsKey(p.getUniqueId())){
			coverHeadInWater(p);
		}
	}
	
	@EventHandler
	private void onPlayerDeath(PlayerDeathEvent event){
		Player p = event.getEntity();
		Drown dro = drowningPlayers.get(p.getUniqueId());
		if(!dro.isForever()){
			undrownPlayer(p);
		}
	}
	
	public boolean setupVault(){
		if(getServer().getPluginManager().getPlugin("Vault") == null){
			logger.info("Vault not found, " + getDescription().getName() + " won't support permissions!");
			return false;
		}
		return setupPermissions();
	}
	
	public void setupMetrics(){
		try{
			Metrics metrics = new Metrics(this);
			metrics.start();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private boolean setupPermissions(){
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) {
            perms = permissionProvider.getProvider();
        }
        return (perms != null);
    }

}
