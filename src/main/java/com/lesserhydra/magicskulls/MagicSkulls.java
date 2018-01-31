package com.lesserhydra.magicskulls;

import com.lesserhydra.bukkitutil.SkullUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MagicSkulls extends JavaPlugin implements Listener {
	
	private static final Pattern usernamePattern = Pattern.compile("[a-zA-Z0-9_]{3,16}");
	private static final Pattern textureIdPattern = Pattern.compile("Id:\"([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})\"");
	private static final Pattern textureValuePattern = Pattern.compile("Value:\"([a-zA-Z0-9+/]+={0,2})\"");
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	private static class SkullCombine {
		final ItemStack skull;
		final ItemStack other;
		final int skullSlot;
		final int otherSlot;
		
		SkullCombine(ItemStack skull, ItemStack other, int skullSlot, int otherSlot) {
			this.skull = skull;
			this.other = other;
			this.skullSlot = skullSlot;
			this.otherSlot = otherSlot;
		}
	}
	
	private static SkullCombine decideCombination(ItemStack first, ItemStack second) {
		if (isPlayerSkull(first)) return new SkullCombine(first, second, 0, 1);
		else if (isPlayerSkull(second)) return new SkullCombine(second, first, 1, 0);
		else return null;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPrepareAnvilSkull(PrepareAnvilEvent event) {
		AnvilInventory inv = event.getInventory();
		
		//Must be empty skull
		SkullCombine combine = decideCombination(inv.getItem(0), inv.getItem(1));
		if (combine == null) return;
		
		//Second item must be written book
		if (!isBook(combine.other)) return;
		
		BookMeta bookMeta = (BookMeta) combine.other.getItemMeta();
		if (!bookMeta.hasPages()) return;
		
		ItemStack result = createResultSkull(bookMeta.getPages());
		if (result == null) return;
		
		result.setAmount(combine.skull.getAmount());
		event.setResult(result);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onClickResultSkull(InventoryClickEvent event) {
		if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
		if (event.getInventory().getType() != InventoryType.ANVIL) return;
		
		boolean shiftClick = event.isShiftClick();
		if (!shiftClick && (event.getCursor() != null && event.getCursor().getType() != Material.AIR)) return;
		
		AnvilInventory inv = (AnvilInventory) event.getInventory();
		SkullCombine combine = decideCombination(inv.getItem(0), inv.getItem(1));
		if (combine == null) return;
		
		if (!isBook(combine.other)) return;
		
		ItemStack resultSkull = inv.getItem(2);
		if (resultSkull == null || resultSkull.getType() != Material.SKULL_ITEM) return;
		if (shiftClick && !inventoryHasRoom(event.getWhoClicked().getInventory(), resultSkull)) return;
		
		event.setCancelled(true);
		
		HumanEntity human = event.getWhoClicked();
		InventoryView view = event.getView();
		getServer().getScheduler().runTask(this, () -> {
			inv.setItem(combine.skullSlot, new ItemStack(Material.AIR));
			inv.setItem(2, new ItemStack(Material.AIR));
			if (shiftClick) human.getInventory().addItem(resultSkull);
			else view.setCursor(resultSkull);
			inv.getLocation().getWorld().playSound(inv.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7F, 2F);
			inv.getLocation().getWorld().spawnParticle(Particle.CLOUD, inv.getLocation().add(0.5, 0.6, 0.5), 20, 0.2, 0.05, 0.2, 0.05);
		});
	}
	
	private static boolean isPlayerSkull(ItemStack skull) {
		if (skull == null || skull.getType() != Material.SKULL_ITEM) return false;
		//MAGIC: Player skull
		return skull.getDurability() == 3;
	}
	
	private static boolean isBook(ItemStack book) {
		if (book == null) return false;
		return book.getType() == Material.BOOK_AND_QUILL
				|| book.getType() == Material.WRITTEN_BOOK;
	}
	
	private static ItemStack createResultSkull(List<String> pages) {
		String bookText = pages.stream()
				.collect(Collectors.joining());
		
		//Try id and value first
		Matcher idMatcher = textureIdPattern.matcher(bookText);
		Matcher valueMatcher = textureValuePattern.matcher(bookText);
		if (idMatcher.find()) {
			if (!valueMatcher.find()) return null;
			//MAGIC: Player skull
			ItemStack result = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
			return SkullUtil.setTexture(result, idMatcher.group(1), valueMatcher.group(1));
		}
		
		//Try username
		Matcher usernameMatcher = usernamePattern.matcher(bookText);
		if (!usernameMatcher.matches()) return null;
		String username = usernameMatcher.group();
		
		//MAGIC: Player skull
		ItemStack result = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
		
		SkullMeta resultMeta = (SkullMeta) result.getItemMeta();
		resultMeta.setOwner(username);
		result.setItemMeta(resultMeta);
		
		return result;
	}
	
	/**
	 * Checks whether of not the given inventory has room for the given item stack.
	 * Note that this assumes the item stack fits in a single empty space.
	 * @param inv The inventory
	 * @param item The item stack
	 * @return Whether the inventory has room
	 */
	private static boolean inventoryHasRoom(Inventory inv, ItemStack item) {
		int numRemaining = item.getAmount();
		ItemStack[] contents = inv.getStorageContents();
		for (ItemStack currentItem : contents) {
			if (currentItem == null || currentItem.getType() == Material.AIR) return true;
			if (!item.isSimilar(currentItem)) continue;
			
			numRemaining -= currentItem.getMaxStackSize() - currentItem.getAmount();
			if (numRemaining <= 0) return true;
		}
		return false;
	}
	
}
