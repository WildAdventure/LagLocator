/*
 * Copyright (c) 2020, Wild Adventure
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 4. Redistribution of this software in source or binary forms shall be free
 *    of all charges or fees to the recipient of this software.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gmail.filoghost.laglocator;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import wild.api.WildCommons;
import wild.api.command.CommandFramework;
import wild.api.command.CommandFramework.Permission;

@Permission("laglocator.use")
public class CommandHandler extends CommandFramework {

	private Plugin plugin;
	
	public CommandHandler(JavaPlugin plugin, String label) {
		super(plugin, label);
		this.plugin = plugin;
	}

	@Override
	public void execute(final CommandSender sender, String label, String[] args) {
		
		// Non elencato, si usa solo tramite click
		if (args.length > 0 && args[0].equalsIgnoreCase("teleport")) {
			CommandValidate.minLength(args, 4, "Utilizzo: /laglocator teleport <mondo> <chunkX> <chunkZ>");
			
			Player player = CommandValidate.getPlayerSender(sender);
			World world = Bukkit.getWorld(args[1]);
			CommandValidate.notNull(world, "Mondo " + args[1] + " non trovato!");
			int chunkX = CommandValidate.getInteger(args[2]);
			int chunkZ = CommandValidate.getInteger(args[3]);
			
			CommandValidate.isTrue(player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR, "Devi essere in modalità creativa o spettatore, o potresti morire!");
			int chunkCenterX = chunkX * 16 + 8;
			int chunkCenterZ = chunkZ * 16 + 8;
			int highestY = 0;
			
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					int highestRelative = world.getHighestBlockAt(chunkCenterX + x, chunkCenterZ + z).getY();
					highestY = Math.max(highestY, highestRelative);
				}
			}

			Location loc = new Location(world, chunkCenterX, highestY + 10, chunkCenterZ, 0f, 90f);
			player.teleport(loc);
			sender.sendMessage(ChatColor.GRAY + "Teletrasportato al chunk (" + chunkX + ", " + chunkZ + ").");
			return;
		}
		
		CommandValidate.isTrue(HandlerList.getRegisteredListeners(plugin).isEmpty(), "C'è già un test in esecuzione, per favore attendi.");

		final LagListener listener = new LagListener();
		listener.registerEvents(plugin);
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				listener.unregisterEvents();

				sendChunkDataBatch(sender, "Chunk più attivi (per redstone):", "Nessun cambiamento redstone registrato.", listener, chunkData -> chunkData.redstoneEvents);
				sendChunkDataBatch(sender, "Chunk più intensivi (per liquidi):", "Nessun movimento di liquidi registrato.", listener, chunkData -> chunkData.liquidEvents);
				sendChunkDataBatch(sender, "Chunk più attivi (per hopper):", "Nessun hopper registrato.", listener, chunkData -> chunkData.hoppersAmount);
				
			}
		}.runTaskLater(plugin, LagLocator.SAMPLING_SECONDS * 20);
		
		sender.sendMessage(ChatColor.GRAY + "Test avviato, riceverai i risultati fra " + LagLocator.SAMPLING_SECONDS + " secondi...");
	}

	
	private void sendChunkDataBatch(CommandSender sender, String header, String failureMessage, LagListener lagListener, ChunkDataField field) {
		List<ChunkData> chunkDataSortedByField = lagListener.getAllChunkData(field);
		sender.sendMessage(ChatColor.YELLOW + header);
		int sent = 0;
		for (int i = 0; i < Math.min(chunkDataSortedByField.size(), 5); i++) { // Al massimo 5 elementi
			ChunkData chunkData = chunkDataSortedByField.get(i);
			if (sendChunkData(sender, chunkData, field.get(chunkData))) {
				sent++;
			}
		}
		
		if (sent == 0) {
			sender.sendMessage(ChatColor.GRAY + failureMessage);
		}
	}

	
	private boolean sendChunkData(CommandSender sender, ChunkData data, int stat) {
		if (stat == 0) {
			return false;
		}
		String formattedStat = String.format("%,d", stat);
		if (sender instanceof Player) {
			WildCommons.fancyMessage("- Chunk (" + data.coords.x + ", " + data.coords.z + "): ").color(ChatColor.GRAY)
								.then(formattedStat + " eventi  ").color(ChatColor.GOLD)
								.then("[Teletrasporto]").color(ChatColor.DARK_GRAY).tooltip(ChatColor.GRAY + "Clicca per teletrasportarti in questo chunk.").command("/laglocator teleport " + data.worldName + " " + data.coords.x + " " + data.coords.z)
								.send(sender);
		} else {
			sender.sendMessage(ChatColor.GRAY + "- Chunk (" + data.coords.x + ", " + data.coords.z + "): " + ChatColor.GOLD + formattedStat + " eventi");
		}
		return true;
	}
}
