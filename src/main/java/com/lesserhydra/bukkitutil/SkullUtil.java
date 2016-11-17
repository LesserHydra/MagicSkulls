package com.lesserhydra.bukkitutil;

import com.comphenix.nbt.NbtFactory;
import org.bukkit.inventory.ItemStack;

public class SkullUtil {
	
	public static boolean skullIsEmpty(ItemStack item) {
		NbtFactory.NbtCompound tag = NbtFactory.fromItemTag(NbtFactory.getCraftItemStack(item), false);
		return tag == null || !tag.containsKey("SkullOwner");
	}
	
	public static ItemStack setTexture(ItemStack skull, String id, String texture) {
		ItemStack craftSkull = NbtFactory.getCraftItemStack(skull);
		NbtFactory.NbtCompound tag = NbtFactory.fromItemTag(craftSkull, true);
		
		NbtFactory.NbtCompound skullOwner = tag.getMap("SkullOwner", true);
		skullOwner.put("Id", id);
		
		NbtFactory.NbtCompound properties = tag.getMap("Properties", true);
		
		NbtFactory.NbtList textures = properties.getList("textures", true);
		
		NbtFactory.NbtCompound textureCompound = NbtFactory.createCompound();
		textureCompound.put("Value", texture);
		textures.add(textureCompound);
		
		return craftSkull;
	}
	
}
