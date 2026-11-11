package com.viscriptquests.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Forge counterpart of the main branch's NeoForge configuration screen entry. */
public final class QuestConfigScreen extends Screen {
    private final Screen parent;
    private final Map<ForgeConfigSpec.ConfigValue<?>, Object> draft = new LinkedHashMap<>();
    private final List<ForgeConfigSpec.ConfigValue<?>> values = new ArrayList<>();
    private int page;
    private boolean invalid;

    public QuestConfigScreen(Screen parent) {
        super(Component.literal("ViScriptQuests"));
        this.parent = parent;
        for (var field : ClientConfig.class.getFields()) {
            try {
                if (field.get(null) instanceof ForgeConfigSpec.ConfigValue<?> value) {
                    values.add(value);
                    draft.put(value, value.get());
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    protected void init() {
        clearWidgets();
        int count = pageSize();
        page = Math.min(page, (values.size() - 1) / count);
        for (int i = page * count; i < Math.min(values.size(), (page + 1) * count); i++) {
            var value = values.get(i);
            Object current = draft.get(value);
            int y = 35 + (i % count) * 30;
            if (value.get() instanceof Double) {
                var input = new EditBox(font, width / 2 + 35, y, 110, 20, label(value));
                input.setValue(String.valueOf(current));
                input.setResponder(text -> draft.put(value, text));
                addRenderableWidget(input);
            } else {
                addRenderableWidget(Button.builder(Component.literal(String.valueOf(current)), button -> {
                    Object next;
                    if (current instanceof Boolean flag) next = !flag;
                    else {
                        var enumeration = (Enum<?>) current;
                        var constants = enumeration.getDeclaringClass().getEnumConstants();
                        next = constants[(enumeration.ordinal() + 1) % constants.length];
                    }
                    draft.put(value, next);
                    rebuildWidgets();
                }).bounds(width / 2 + 35, y, 110, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1); rebuildWidgets();
        }).bounds(width / 2 - 150, height - 28, 35, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page = Math.min((values.size() - 1) / count, page + 1); rebuildWidgets();
        }).bounds(width / 2 - 110, height - 28, 35, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(width / 2 - 65, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(width / 2 + 40, height - 28, 100, 20).build());
    }

    private int pageSize() { return Math.max(1, (height - 85) / 30); }

    private Component label(ForgeConfigSpec.ConfigValue<?> value) {
        ForgeConfigSpec.ValueSpec spec = ClientConfig.SPEC.getSpec().get(value.getPath());
        String key = spec.getTranslationKey();
        return key == null ? Component.literal(String.join(".", value.getPath())) : Component.translatable(key);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void save() {
        var resolved = new LinkedHashMap<ForgeConfigSpec.ConfigValue<?>, Object>();
        for (var value : values) {
            Object candidate = draft.get(value);
            try {
                if (value.get() instanceof Double) candidate = Double.parseDouble(candidate.toString());
                ForgeConfigSpec.ValueSpec spec = ClientConfig.SPEC.getSpec().get(value.getPath());
                if (!spec.test(candidate)) { invalid = true; return; }
            } catch (NumberFormatException e) { invalid = true; return; }
            resolved.put(value, candidate);
        }
        resolved.forEach((value, candidate) -> ((ForgeConfigSpec.ConfigValue) value).set(candidate));
        ClientConfig.SPEC.save();
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int count = pageSize();
        for (int i = page * count; i < Math.min(values.size(), (page + 1) * count); i++) {
            graphics.drawString(font, font.plainSubstrByWidth(label(values.get(i)).getString(), Math.max(50, width / 2 - 10)),
                    10, 41 + (i % count) * 30, 0xFFFFFF);
        }
        if (invalid) graphics.drawCenteredString(font, Component.translatable("viscript_quests.configuration.invalid"),
                width / 2, height - 43, 0xFF5555);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
