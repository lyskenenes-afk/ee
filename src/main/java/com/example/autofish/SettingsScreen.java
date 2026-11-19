package com.example.autofish;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class SettingsScreen extends Screen {

    private ButtonWidget fishButton;
    private ButtonWidget captchaButton;

    public SettingsScreen() {
        super(Text.literal("AutoFish Ayarlari"));
    }

    @Override
    protected void init() {
        int w = 200, h = 20;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 40;

        fishButton = ButtonWidget.builder(fishLabel(), btn -> {
            AutoFishClient.setFishEnabled(!AutoFishClient.fishEnabled);
            btn.setMessage(fishLabel());
        }).dimensions(x, y, w, h).build();

        captchaButton = ButtonWidget.builder(captchaLabel(), btn -> {
            AutoFishClient.captchaEnabled = !AutoFishClient.captchaEnabled;
            btn.setMessage(captchaLabel());
        }).dimensions(x, y + 28, w, h).build();

        ButtonWidget close = ButtonWidget.builder(Text.literal("Kapat"), btn -> this.close())
                .dimensions(x, y + 66, w, h).build();

        addDrawableChild(fishButton);
        addDrawableChild(captchaButton);
        addDrawableChild(close);
    }

    private Text fishLabel() {
        return Text.literal("Otomatik Balik: " + (AutoFishClient.fishEnabled ? "ACIK" : "KAPALI"));
    }

    private Text captchaLabel() {
        return Text.literal("Dogrulama Cevaplayici: " + (AutoFishClient.captchaEnabled ? "ACIK" : "KAPALI"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 65, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Tuslari Ayarlar > Kontroller > AutoFish bolumunden degistirebilirsin"),
                this.width / 2, this.height / 2 + 50, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
