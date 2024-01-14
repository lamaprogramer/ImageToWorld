package net.iamaprogrammer.util;

import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class LockedMap {
    public static ItemStack createLockedMap(World world, byte scale) {
        ItemStack itemStack = new ItemStack(Items.FILLED_MAP);
        createLockedMapState(itemStack, world, scale, world.getRegistryKey());
        return itemStack;
    }

    private static void setMapId(ItemStack stack, int id) {
        stack.getOrCreateNbt().putInt("map", id);
    }

    private static void createLockedMapState(ItemStack stack, World world, int scale, RegistryKey<World> dimension) {
        int i = allocateMapId(world, scale, dimension);
        setMapId(stack, i);
    }

    private static int allocateMapId(World world, int scale, RegistryKey<World> dimension) {
        MapState mapState = MapState.of((byte)scale, true, dimension);
        int i = world.getNextMapId();
        world.putMapState(FilledMapItem.getMapName(i), mapState);
        return i;
    }
}
