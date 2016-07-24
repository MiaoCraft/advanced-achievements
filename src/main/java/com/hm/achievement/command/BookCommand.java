package com.hm.achievement.command;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.particle.ParticleEffect;
import com.hm.achievement.particle.ReflectionUtils.PackageType;

public class BookCommand {

	private AdvancedAchievements plugin;
	private int bookTime;
	private String bookSeparator;
	private boolean additionalEffects;
	private boolean sound;
	private int version;

	// Corresponds to times at which players have received their books.
	private HashMap<Player, Long> players;

	public BookCommand(AdvancedAchievements plugin) {

		this.plugin = plugin;
		players = new HashMap<Player, Long>();
		bookTime = plugin.getPluginConfig().getInt("TimeBook", 0) * 1000;
		bookSeparator = plugin.getPluginConfig().getString("BookSeparator", "");
		additionalEffects = plugin.getPluginConfig().getBoolean("AdditionalEffects", true);
		sound = plugin.getPluginConfig().getBoolean("Sound", true);
		// Simple and fast check to compare versions. Might need to
		// be updated in the future depending on how the Minecraft
		// versions change in the future.
		version = Integer.parseInt(PackageType.getServerVersion().split("_")[1]);
	}

	/**
	 * Check is player hasn't received a book too recently (with "too recently" being defined in configuration file).
	 */
	private boolean timeAuthorisedBook(Player player) {

		if (player.hasPermission("achievement.*"))
			return true;
		long currentTime = System.currentTimeMillis();
		long lastBookTime = 0;
		if (players.containsKey(player))
			lastBookTime = players.get(player);
		if (currentTime - lastBookTime < bookTime)
			return false;
		players.put(player, currentTime);
		return true;

	}

	/**
	 * Give an achievements book to the player, or several books depending on the number of achievements.
	 */
	public void giveBook(Player player) {

		if (timeAuthorisedBook(player)) {

			// Play special effect when receiving the book.
			if (additionalEffects)
				try {
					ParticleEffect.ENCHANTMENT_TABLE.display(0, 2, 0, 1, 1000, player.getLocation(), 100);
				} catch (Exception ex) {
					plugin.getLogger().severe("Error while displaying additional particle effects.");
				}

			// Play special sound when receiving the book.
			if (sound) {
				if (version < 9) // Old enum for versions prior to Minecraft
									// 1.9.
					player.getWorld().playSound(player.getLocation(), Sound.valueOf("LEVEL_UP"), 1, 0);
				else
					player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0);
			}

			ArrayList<String> achievements = plugin.getDb().getPlayerAchievementsList(player);

			fillBook(achievements, player);

			player.sendMessage(plugin.getChatHeader()
					+ plugin.getPluginLang().getString("book-received", "You received your achievements book!"));
		} else {
			// The player has already received a book recently.
			player.sendMessage(plugin.getChatHeader() + plugin.getPluginLang()
					.getString("book-delay", "You must wait TIME seconds between each book reception!")
					.replace("TIME", "" + bookTime / 1000));
		}
	}

	/**
	 * Construct the pages of a book.
	 */
	private void fillBook(ArrayList<String> achievements, Player player) {

		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		ArrayList<String> pages = new ArrayList<String>();
		BookMeta bm = (BookMeta) book.getItemMeta();

		try {
			for (int i = 0; i < achievements.size(); i += 3) {
				String currentAchievement = "&0" + achievements.get(i) + "\n" + bookSeparator + "\n"
						+ achievements.get(i + 1) + "\n" + bookSeparator + "\n&r" + achievements.get(i + 2);
				currentAchievement = ChatColor.translateAlternateColorCodes('&', currentAchievement);
				pages.add(currentAchievement);
			}
		} catch (

		Exception e) {
			plugin.getLogger().severe("Error while creating book pages of book.");
		}

		bm.setPages(pages);
		bm.setAuthor(player.getName());
		bm.setTitle(plugin.getPluginLang().getString("book-name", "Achievements Book"));
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		bm.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&r&o" +plugin.getPluginLang().getString("book-date", "Book created on DATE.").replace("DATE",
				format.format(new Date())))));

		book.setItemMeta(bm);

		if (player.getInventory().firstEmpty() != -1)
			player.getInventory().addItem(new ItemStack[] { book });
		else
			for (ItemStack item : new ItemStack[] { book })
				player.getWorld().dropItem(player.getLocation(), item);

	}

	public HashMap<Player, Long> getPlayers() {

		return players;
	}

}
