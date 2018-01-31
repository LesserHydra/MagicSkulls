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
		
		NbtFactory.NbtCompound textureCompound = NbtFactory.createCompound();
		textureCompound.put("Value", texture);
		tag.putPath("Properties.textures", NbtFactory.createList(textureCompound));
		
		return craftSkull;
	}
	
}
