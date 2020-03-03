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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.Plugin;

public class LagListener implements Listener {
	
	private Map<String, Map<ChunkCoords, ChunkData>> data = new HashMap<>();
	
	public void registerEvents(Plugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		for (World world : Bukkit.getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				for (BlockState tileEntity : chunk.getTileEntities()) {
					if (tileEntity instanceof Hopper) {
						getChunkData(tileEntity.getBlock()).hoppersAmount++;
					}
				}
			}
		}
	}
	
	public void unregisterEvents() {
		HandlerList.unregisterAll(this);
	}
	
	private ChunkData getChunkData(Block block) {
		String worldName = block.getWorld().getName();
		ChunkCoords coords = new ChunkCoords(block.getX() >> 4, block.getZ() >> 4);
		
		Map<ChunkCoords, ChunkData> worldData = data.get(worldName);
		if (worldData == null) {
			worldData = new HashMap<>();
			data.put(worldName, worldData);
		}
		
		ChunkData chunkData = worldData.get(coords);
		if (chunkData == null) {
			chunkData = new ChunkData(worldName, coords);
			worldData.put(coords, chunkData);
		}
		
		return chunkData;
	}

	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onRedstone(BlockRedstoneEvent event) {
		getChunkData(event.getBlock()).redstoneEvents++;
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onLiquidFlow(BlockFromToEvent event) {
		if (event.getBlock().isLiquid()) {
			getChunkData(event.getBlock()).liquidEvents++;
		}
	}
	
	public List<ChunkData> getAllChunkData(ChunkDataField field) {
		List<ChunkData> allChunkData = new ArrayList<>();
		
		for (Map<ChunkCoords, ChunkData> worldData : data.values()) {
			allChunkData.addAll(worldData.values());
		}
		
		Collections.sort(allChunkData, (o1, o2) ->  {
			int c = field.get(o2) - field.get(o1);
			if (c == 0) {
				c = o2.hashCode() - o1.hashCode(); // Altrimenti con 0 si eliminano
			}
			return c;
		});
		return allChunkData;
	}
		
}
