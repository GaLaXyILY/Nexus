package me.pugabyte.nexus.features.recipes.models;

import me.pugabyte.nexus.Nexus;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.List;

public abstract class FunctionalRecipe extends NexusRecipe implements Listener {

	public FunctionalRecipe() {
		Nexus.registerListener(this);
	}

	@Override
	public abstract ItemStack getResult();

	/**
	 * Return the recipe to be registered on the server
	 */
	@Override
	public abstract Recipe getRecipe();

	/**
	 * Return a list of static ingredients in the recipe, but not any that would be a material choice
	 */
	@Override
	public abstract List<ItemStack> getIngredients();

	/**
	 * Return a string array of length 3 with the pattern of the recipe. If shapeless, return null
	 * Use spaces for air
	 * Use # for Material Choice
	 */
	@Override
	public abstract String[] getPattern();

	/**
	 * If a material choice is used, return that material choice. Return null if no choice is used.
	 */
	@Override
	public abstract RecipeChoice.MaterialChoice getMaterialChoice();
}
