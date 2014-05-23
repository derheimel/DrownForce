package io.github.oaschi.drownforce;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;


public class DFLogger{
	
	private DrownForce plugin;
	private Logger logger;
	
	public DFLogger(DrownForce plugin){
		this.plugin = plugin;
		this.logger = plugin.getLogger();
	}
	
	private String getFormattedMessage(String msg){
		PluginDescriptionFile pdf = plugin.getDescription();
		return ChatColor.BLUE + "[" + ChatColor.WHITE + pdf.getName() + ChatColor.BLUE + "]: " + ChatColor.WHITE + msg;
	}
	
	public void info(CommandSender reciever, String msg){
		if(reciever instanceof Player)
			reciever.sendMessage(getFormattedMessage(msg));
		else
			reciever.sendMessage(msg);
	}
	
	public void info(String msg){
		this.logger.info(msg);
	}
	
}
