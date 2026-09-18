package com.ia7md.patternconverter.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public record SlotStatus(Kind kind, Optional<Component> message) {
    public static final SlotStatus EMPTY = new SlotStatus(Kind.EMPTY, Optional.empty());

    public static final Codec<SlotStatus> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Kind.CODEC.fieldOf("kind").forGetter(SlotStatus::kind),
            ComponentSerialization.CODEC.optionalFieldOf("message").forGetter(SlotStatus::message)
    ).apply(instance, SlotStatus::new));

    public static SlotStatus waiting(Component message) {
        return new SlotStatus(Kind.WAITING, Optional.of(message));
    }

    public static SlotStatus queued(Component message) {
        return new SlotStatus(Kind.QUEUED, Optional.of(message));
    }

    public static SlotStatus error(Component message) {
        return new SlotStatus(Kind.ERROR, Optional.of(message));
    }

    public enum Kind implements StringRepresentable {
        EMPTY("empty"),
        QUEUED("queued"),
        WAITING("waiting"),
        ERROR("error");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
