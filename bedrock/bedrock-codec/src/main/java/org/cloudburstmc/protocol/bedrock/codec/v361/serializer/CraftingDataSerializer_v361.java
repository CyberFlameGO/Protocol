package org.cloudburstmc.protocol.bedrock.codec.v361.serializer;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.codec.v354.serializer.CraftingDataSerializer_v354;
import org.cloudburstmc.protocol.bedrock.data.defintions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.CraftingData;
import org.cloudburstmc.protocol.bedrock.data.inventory.CraftingDataType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.common.util.VarInts;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CraftingDataSerializer_v361 extends CraftingDataSerializer_v354 {
    public static final CraftingDataSerializer_v361 INSTANCE = new CraftingDataSerializer_v361();

    @Override
    protected CraftingData readShapelessRecipe(ByteBuf buffer, BedrockCodecHelper helper, CraftingDataType type) {
        String recipeId = helper.readString(buffer);
        List<ItemData> inputs = new ObjectArrayList<>();
        helper.readArray(buffer, inputs, this::readIngredient);

        List<ItemData> outputs = new ObjectArrayList<>();
        helper.readArray(buffer, outputs, helper::readItem);

        UUID uuid = helper.readUuid(buffer);
        String craftingTag = helper.readString(buffer);
        int priority = VarInts.readInt(buffer);
        return new CraftingData(type, recipeId, -1, -1, -1, -1, inputs, outputs, uuid,
                craftingTag, priority);
    }

    @Override
    protected void writeShapelessRecipe(ByteBuf buffer, BedrockCodecHelper helper, CraftingData data) {
        helper.writeString(buffer, data.getRecipeId());
        helper.writeArray(buffer, data.getInputs(), this::writeIngredient);
        helper.writeArray(buffer, data.getOutputs(), helper::writeItem);
        helper.writeUuid(buffer, data.getUuid());
        helper.writeString(buffer, data.getCraftingTag());
        VarInts.writeInt(buffer, data.getPriority());
    }

    @Override
    protected CraftingData readShapedRecipe(ByteBuf buffer, BedrockCodecHelper helper, CraftingDataType type) {
        String recipeId = helper.readString(buffer);
        int width = VarInts.readInt(buffer);
        int height = VarInts.readInt(buffer);
        int inputCount = width * height;
        List<ItemData> inputs = new ObjectArrayList<>(inputCount);
        for (int i = 0; i < inputCount; i++) {
            inputs.add(this.readIngredient(buffer, helper));
        }
        List<ItemData> outputs = new ObjectArrayList<>();
        helper.readArray(buffer, outputs, helper::readItem);
        UUID uuid = helper.readUuid(buffer);
        String craftingTag = helper.readString(buffer);
        int priority = VarInts.readInt(buffer);
        return new CraftingData(type, recipeId, width, height, -1, -1, inputs, outputs, uuid,
                craftingTag, priority);
    }

    @Override
    protected void writeShapedRecipe(ByteBuf buffer, BedrockCodecHelper helper, CraftingData data) {
        helper.writeString(buffer, data.getRecipeId());
        VarInts.writeInt(buffer, data.getWidth());
        VarInts.writeInt(buffer, data.getHeight());
        int count = data.getWidth() * data.getHeight();
        List<ItemData> inputs = data.getInputs();
        for (int i = 0; i < count; i++) {
            this.writeIngredient(buffer, inputs.get(i));
        }
        helper.writeArray(buffer, data.getOutputs(), helper::writeItem);
        helper.writeUuid(buffer, data.getUuid());
        helper.writeString(buffer, data.getCraftingTag());
        VarInts.writeInt(buffer, data.getPriority());
    }

    protected ItemData readIngredient(ByteBuf buffer, BedrockCodecHelper helper) {
        int id = VarInts.readInt(buffer);
        ItemDefinition definition = helper.getItemDefinitions().getDefinition(id);
        int auxValue = 0;
        int stackSize = 0;

        if (id != 0) {
            auxValue = VarInts.readInt(buffer);
            if (auxValue == 0x7fff) auxValue = -1;
            stackSize = VarInts.readInt(buffer);
        }

        return ItemData.builder()
                .definition(definition)
                .damage(auxValue)
                .count(stackSize)
                .build();
    }

    protected void writeIngredient(ByteBuf buffer, ItemData itemData) {
        requireNonNull(itemData, "ItemData is null");

        int id = itemData.getDefinition().getRuntimeId();
        VarInts.writeInt(buffer, id);

        if (id != 0) {
            int damage = itemData.getDamage();
            if (damage == -1) damage = 0x7fff;
            VarInts.writeInt(buffer, damage);
            VarInts.writeInt(buffer, itemData.getCount());
        }
    }
}
