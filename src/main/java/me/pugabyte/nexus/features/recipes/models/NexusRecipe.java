package me.pugabyte.nexus.features.recipes.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.recipes.CustomRecipes;
import me.pugabyte.nexus.features.recipes.RecipeUtils;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class NexusRecipe {

	@NonNull
	public ItemStack result;
	public List<ItemStack> ingredients = new ArrayList<>();
	public RecipeChoice.MaterialChoice materialChoice;
	public String[] pattern;
	public Recipe recipe;
	public RecipeType type = RecipeType.MISC;

	public String getPermission() {
		return null;
	}

	public ItemStack getResult() {
		return recipe.getResult();
	}

	public NexusRecipe type(RecipeType type) {
		this.type = type;
		return this;
	}

	public static NexusRecipe shapeless(ItemStack result, Material material, RecipeChoice.MaterialChoice ingredients) {
		NexusRecipe recipe = new NexusRecipe(result);
		recipe.getIngredients().add(new ItemStack(material));

		NamespacedKey key = new NamespacedKey(Nexus.getInstance(), stripColor("custom_" + getItemName(result)));
		ShapelessRecipe bukkitRecipe = new ShapelessRecipe(key, result);
		bukkitRecipe.addIngredient(material);
		bukkitRecipe.addIngredient(ingredients);
		recipe.setRecipe(bukkitRecipe);
		recipe.setMaterialChoice(ingredients);

		CustomRecipes.recipes.add(recipe);
		return recipe;
	}

	public static NexusRecipe shapeless(ItemStack result, Material... ingredients) {
		NexusRecipe recipe = new NexusRecipe(result);
		Arrays.asList(ingredients).forEach(mat -> recipe.getIngredients().add(new ItemStack(mat)));

		NamespacedKey key = new NamespacedKey(Nexus.getInstance(), stripColor("custom_" + getItemName(result)));
		ShapelessRecipe bukkitRecipe = new ShapelessRecipe(key, result);
		for (Material material : ingredients)
			bukkitRecipe.addIngredient(material);
		recipe.setRecipe(bukkitRecipe);

		CustomRecipes.recipes.add(recipe);
		return recipe;
	}

	public static NexusRecipe shaped(ItemStack result, String[] pattern, Material... ingredients) {
		NexusRecipe recipe = new NexusRecipe(result);
		recipe.setPattern(pattern);
		Arrays.asList(ingredients).forEach(mat -> recipe.getIngredients().add(new ItemStack(mat)));

		NamespacedKey key = new NamespacedKey(Nexus.getInstance(), "custom_" + getItemName(result));
		recipe.setRecipe(shapedRecipe(key, result, pattern, ingredients));

		CustomRecipes.recipes.add(recipe);
		return recipe;
	}

	public static ShapedRecipe shapedRecipe(NamespacedKey key, ItemStack result, String[] pattern, Material... ingredients) {
		ShapedRecipe bukkitRecipe = new ShapedRecipe(key, result);
		bukkitRecipe.shape(pattern[0], pattern[1], pattern[2]);
		for (int i = 1; i <= ingredients.length; i++) {
			bukkitRecipe.setIngredient((char) i, ingredients[i - 1]);
		}
		return bukkitRecipe;
	}

	public static ShapedRecipe surroundRecipe(NamespacedKey key, ItemStack result, Material center, Material surround) {
		return surroundRecipe(key, result, new ItemStack(center), surround);
	}

	public static ShapedRecipe surroundRecipe(NamespacedKey key, ItemStack result, ItemStack center, Material surround) {
		return surroundRecipe(key, result, center, new RecipeChoice.MaterialChoice(surround));
	}

	public static ShapedRecipe surroundRecipe(NamespacedKey key, ItemStack result, ItemStack center, RecipeChoice.MaterialChoice surround) {
		ShapedRecipe bukkitRecipe = new ShapedRecipe(key, result);
		bukkitRecipe.shape("111", "121", "111");
		bukkitRecipe.setIngredient('1', surround);
		bukkitRecipe.setIngredient('2', center);
		return bukkitRecipe;
	}

	public static NexusRecipe surround(ItemStack result, ItemStack center, RecipeChoice.MaterialChoice surround) {
		NexusRecipe recipe = new NexusRecipe(result);
		recipe.setPattern(new String[] {"###", "#2#", "###"});
		recipe.setMaterialChoice(surround);
		recipe.getIngredients().add(center);

		NamespacedKey key = new NamespacedKey(Nexus.getInstance(), "custom_" + getItemName(result));
		recipe.setRecipe(surroundRecipe(key, result, center, surround));

		CustomRecipes.recipes.add(recipe);
		return recipe;
	}

	public static NexusRecipe surround(ItemStack result, Material center, RecipeChoice.MaterialChoice surround) {
		return surround(result, new ItemStack(center), surround);
	}

	public static NexusRecipe surround(ItemStack result, Material center, Material surround) {
		return surround(result, center, new RecipeChoice.MaterialChoice(surround));
	}

	public void register() {
		try {
			if (getRecipe() == null) return;
			for (Recipe recipe1 : Bukkit.getServer().getRecipesFor(getRecipe().getResult()))
				if (RecipeUtils.areEqual(getRecipe(), recipe1)) return;
			Tasks.sync(() -> {
				try {
					Bukkit.addRecipe(getRecipe());
				} catch (IllegalStateException duplicate) {
					Nexus.log(duplicate.getMessage());
				} catch (Exception ex) {
					Nexus.log("Error while adding custom recipe " + ((Keyed) getRecipe()).getKey() + " to Bukkit");
					ex.printStackTrace();
				}
			});
		} catch (Exception ex) {
			Nexus.log("Error while adding custom recipe " + ((Keyed) getRecipe()).getKey());
			ex.printStackTrace();
		}
	}

	private static String getItemName(ItemStack result) {
		return StringUtils.stripColor(ItemUtils.getName(result).replaceAll(" ", "_").trim().toLowerCase());
	}
}
