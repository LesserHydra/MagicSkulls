package com.lesserhydra.magicskulls.volitilecode;

import net.minecraft.server.v1_10_R1.ItemStack;
import net.minecraft.server.v1_10_R1.NBTTagCompound;
import net.minecraft.server.v1_10_R1.NBTTagList;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;

public class NMSSkullUtil {
	
	public static boolean skullIsEmpty(org.bukkit.inventory.ItemStack item) {
		ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = nmsItem.getTag();
		return tag == null || !tag.hasKey("SkullOwner");
	}
	
	public static org.bukkit.inventory.ItemStack setTexture(org.bukkit.inventory.ItemStack skull, String id, String texture) {
		ItemStack nmsSkull = CraftItemStack.asNMSCopy(skull);
		NBTTagCompound tag = new NBTTagCompound();
		
		NBTTagCompound skullOwner = new NBTTagCompound();
		skullOwner.setString("Id", id);
		tag.set("SkullOwner", skullOwner);
		
		NBTTagCompound properties = new NBTTagCompound();
		skullOwner.set("Properties", properties);
		
		NBTTagList textures = new NBTTagList();
		properties.set("textures", textures);
		
		NBTTagCompound textureCompound = new NBTTagCompound();
		textureCompound.setString("Value", texture);
		textures.add(textureCompound);
		
		nmsSkull.setTag(tag);
		return CraftItemStack.asBukkitCopy(nmsSkull);
	}
	
}
