package net.iamaprogrammer.command.argument;

import com.google.gson.JsonObject;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ArgumentHelper;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;

public class ScaleArgumentSerializer implements ArgumentSerializer<ScaleArgumentType, ScaleArgumentSerializer.Properties> {
    public ScaleArgumentSerializer() {
    }

    public void writePacket(ScaleArgumentSerializer.Properties properties, PacketByteBuf packetByteBuf) {
        boolean bl = properties.min != Double.MIN_VALUE;
        boolean bl2 = properties.max != Double.MAX_VALUE;
        packetByteBuf.writeByte(ArgumentHelper.getMinMaxFlag(bl, bl2));
        if (bl) {
            packetByteBuf.writeDouble(properties.min);
        }

        if (bl2) {
            packetByteBuf.writeDouble(properties.max);
        }
    }

    public ScaleArgumentSerializer.Properties fromPacket(PacketByteBuf packetByteBuf) {
        byte b = packetByteBuf.readByte();
        double i = ArgumentHelper.hasMinFlag(b) ? packetByteBuf.readDouble() : Double.MIN_VALUE;
        double j = ArgumentHelper.hasMaxFlag(b) ? packetByteBuf.readDouble() : Double.MAX_VALUE;
        return new ScaleArgumentSerializer.Properties(i, j);
    }

    public void writeJson(ScaleArgumentSerializer.Properties properties, JsonObject jsonObject) {
        if (properties.min != Double.MIN_VALUE) {
            jsonObject.addProperty("min", properties.min);
        }

        if (properties.max != Double.MAX_VALUE) {
            jsonObject.addProperty("max", properties.max);
        }

    }

    public ScaleArgumentSerializer.Properties getArgumentTypeProperties(ScaleArgumentType doubleArgumentType) {
        return new ScaleArgumentSerializer.Properties(doubleArgumentType.getMinimum(), doubleArgumentType.getMaximum());
    }

    public final class Properties implements ArgumentSerializer.ArgumentTypeProperties<ScaleArgumentType> {
        final double min;
        final double max;

        Properties(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public ScaleArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
            return ScaleArgumentType.scale(this.min, this.max);
        }

        public ArgumentSerializer<ScaleArgumentType, ?> getSerializer() {
            return ScaleArgumentSerializer.this;
        }
    }
}
