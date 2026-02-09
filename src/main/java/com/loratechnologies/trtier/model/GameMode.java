package com.loratechnologies.trtier.model;

import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.util.Optional;

public record GameMode(String id, String title) {
    public static final GameMode NONE = new GameMode("annoying_long_id_that_no_one_will_ever_use_just_to_make_sure", "§cNone§r");

    public boolean isNone() {
        return this.id.equals(NONE.id);
    }

    private Pair<Character, TextColor> iconAndColor() {
        return switch (this.id.toLowerCase()) {
            case "axe" -> new Pair<>('\uE701', TextColor.fromFormatting(Formatting.GREEN));
            case "mace" -> new Pair<>('\uE702', TextColor.fromFormatting(Formatting.GRAY));
            case "nethpot", "nethop", "neth_pot" -> new Pair<>('\uE703', TextColor.fromRgb(0x7d4a40));
            case "pot" -> new Pair<>('\uE704', TextColor.fromRgb(0xff0000));
            case "smp" -> new Pair<>('\uE705', TextColor.fromRgb(0xeccb45));
            case "sword" -> new Pair<>('\uE706', TextColor.fromRgb(0xa4fdf0));
            case "uhc" -> new Pair<>('\uE707', TextColor.fromFormatting(Formatting.RED));
            case "vanilla" -> new Pair<>('\uE708', TextColor.fromFormatting(Formatting.LIGHT_PURPLE));
            case "gapple" -> new Pair<>('\uE709', TextColor.fromRgb(0xFFD700));
            case "crystal" -> new Pair<>('\uE70A', TextColor.fromFormatting(Formatting.LIGHT_PURPLE));
            case "diasmp", "dia_smp" -> new Pair<>('\uE70B', TextColor.fromRgb(0x8c668b));
            default -> new Pair<>('•', TextColor.fromFormatting(Formatting.WHITE));
        };
    }

    public Optional<Character> icon() {
        Pair<Character, TextColor> pair = this.iconAndColor();

        return pair.getRight().getRgb() == 0xFFFFFF ? Optional.empty() : Optional.of(pair.getLeft());
    }

    public Text asStyled(boolean withDefaultDot) {
        Pair<Character, TextColor> pair = this.iconAndColor();

        if (pair.getRight().getRgb() == 0xFFFFFF && !withDefaultDot) {
            return Text.of(this.title);
        } else {
            Text name = Text.literal(this.title).styled(s -> s.withColor(pair.getRight()));
            return Text.literal(pair.getLeft() + " ").append(name);
        }
    }
}
