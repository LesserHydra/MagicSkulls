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
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPrepareAnvilSkull(PrepareAnvilEvent event) {
		AnvilInventory inv = event.getInventory();
		//Must be empty skull
		ItemStack skull = inv.getItem(0);
		if (!isEmptySkull(skull)) return;
		
		//Second item must be written book
		ItemStack book = inv.getItem(1);
		if (book == null || !(book.getType() == Material.BOOK_AND_QUILL || book.getType() == Material.WRITTEN_BOOK)) return;
		
		BookMeta bookMeta = (BookMeta) book.getItemMeta();
		if (!bookMeta.hasPages()) return;
		
		ItemStack result = createResultSkull(bookMeta.getPages());
		if (result == null) return;
		
		result.setAmount(skull.getAmount());
		event.setResult(result);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onClickResultSkull(InventoryClickEvent event) {
		if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
		if (event.getInventory().getType() != InventoryType.ANVIL) return;
		
		boolean shiftClick = event.isShiftClick();
		if (!shiftClick && (event.getCursor() != null && event.getCursor().getType() != Material.AIR)) return;
		
		AnvilInventory inv = (AnvilInventory) event.getInventory();
		ItemStack skull = inv.getItem(0);
		if (!isEmptySkull(skull)) return;
		
		ItemStack book = inv.getItem(1);
		if (book == null || !(book.getType() == Material.BOOK_AND_QUILL || book.getType() == Material.WRITTEN_BOOK)) return;
		
		ItemStack resultSkull = inv.getItem(2);
		if (resultSkull == null || resultSkull.getType() != Material.SKULL_ITEM) return;
		if (shiftClick && !inventoryHasRoom(event.getWhoClicked().getInventory(), resultSkull)) return;
		
		event.setCancelled(true);
		
		HumanEntity human = event.getWhoClicked();
		InventoryView view = event.getView();
		getServer().getScheduler().runTask(this, () -> {
			inv.setItem(0, new ItemStack(Material.AIR));
			inv.setItem(2, new ItemStack(Material.AIR));
			if (shiftClick) human.getInventory().addItem(resultSkull);
			else view.setCursor(resultSkull);
			inv.getLocation().getWorld().playSound(inv.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7F, 2F);
			inv.getLocation().getWorld().spawnParticle(Particle.CLOUD, inv.getLocation().add(0.5, 0.6, 0.5), 20, 0.2, 0.05, 0.2, 0.05);
		});
	}
	
	private boolean isEmptySkull(ItemStack skull) {
		if (skull == null || skull.getType() != Material.SKULL_ITEM) return false;
		//Must be player skull
		if (skull.getDurability() != 3) return false;
		//Must be blank
		return SkullUtil.skullIsEmpty(skull);
	}
	
	private ItemStack createResultSkull(List<String> pages) {
		String bookText = pages.stream()
				.collect(Collectors.joining());
		
		//Try id and value first
		Matcher idMatcher = textureIdPattern.matcher(bookText);
		Matcher valueMatcher = textureValuePattern.matcher(bookText);
		if (idMatcher.find()) {
			if (!valueMatcher.find()) return null;
			ItemStack result = new ItemStack(Material.SKULL_ITEM);
			result.setDurability((short) 3);
			return SkullUtil.setTexture(result, idMatcher.group(1), valueMatcher.group(1));
		}
		
		//Try username
		Matcher usernameMatcher = usernamePattern.matcher(bookText);
		if (!usernameMatcher.find()) return null;
		String username = usernameMatcher.group();
		
		ItemStack result = new ItemStack(Material.SKULL_ITEM);
		result.setDurability((short) 3);
		
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
	private boolean inventoryHasRoom(Inventory inv, ItemStack item) {
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
