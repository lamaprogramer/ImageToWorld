package net.iamaprogrammer.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.MapPostProcessingComponent;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class LockedMap {
    public static ItemStack createLockedMap(World world, byte scale) {
        ItemStack itemStack = new ItemStack(Items.FILLED_MAP);
        MapIdComponent mapIdComponent = allocateMapId(world, scale, world.getRegistryKey());
        itemStack.set(DataComponentTypes.MAP_ID, mapIdComponent);
        return itemStack;
    }

    private static MapIdComponent allocateMapId(World world, int scale, RegistryKey<World> dimension) {
        MapState mapState = MapState.of((byte)scale, true, dimension);
        MapIdComponent mapIdComponent = world.getNextMapId();
        world.putMapState(mapIdComponent, mapState);
        return mapIdComponent;
    }
}
