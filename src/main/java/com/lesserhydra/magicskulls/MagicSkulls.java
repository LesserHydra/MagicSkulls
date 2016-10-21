package com.lesserhydra.magicskulls;

import com.lesserhydra.magicskulls.volitilecode.NMSSkullUtil;
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
		//Must be skull
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
		if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) return;
		
		AnvilInventory inv = (AnvilInventory) event.getInventory();
		ItemStack skull = inv.getItem(0);
		if (!isEmptySkull(skull)) return;
		
		ItemStack book = inv.getItem(1);
		if (book == null || !(book.getType() == Material.BOOK_AND_QUILL || book.getType() == Material.WRITTEN_BOOK)) return;
		
		ItemStack resultSkull = inv.getItem(2);
		if (resultSkull == null || resultSkull.getType() != Material.SKULL_ITEM) return;
		
		event.setCancelled(true);
		
		HumanEntity human = event.getWhoClicked();
		InventoryView view = event.getView();
		getServer().getScheduler().runTask(this, () -> {
			inv.setItem(0, new ItemStack(Material.AIR));
			inv.setItem(2, new ItemStack(Material.AIR));
			view.setCursor(resultSkull);
			inv.getLocation().getWorld().playSound(inv.getLocation(), Sound.ENTITY_FIREWORK_LARGE_BLAST, 1.0F, 2F);
			inv.getLocation().getWorld().spawnParticle(Particle.CLOUD, inv.getLocation().add(0.5, 0.6, 0.5), 50, 0, 0, 0, 0.4);
		});
	}
	
	private boolean isEmptySkull(ItemStack skull) {
		if (skull == null || skull.getType() != Material.SKULL_ITEM) return false;
		//Must be player skull
		if (skull.getDurability() != 3) return false;
		//Must be blank
		return NMSSkullUtil.skullIsEmpty(skull);
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
			return NMSSkullUtil.setTexture(result, idMatcher.group(1), valueMatcher.group(1));
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
	
}
