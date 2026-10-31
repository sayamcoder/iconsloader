package iconloader.iconLoader.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class DialogManager {

    public static void openConfirmDialog(Player player, String title, String message, Runnable onConfirm) {
        ActionButton yesButton = ActionButton.builder(Component.text("✔ Confirm", TextColor.color(0x00FFAA)))
            .tooltip(Component.text("Click to execute this action"))
            .action(DialogAction.customClick((response, audience) -> {
                onConfirm.run();
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        ActionButton noButton = ActionButton.builder(Component.text("✘ Cancel", NamedTextColor.RED))
            .tooltip(Component.text("Click to cancel"))
            .action(DialogAction.customClick((response, audience) -> {
                player.sendMessage(Component.text("§7[IconsLoader] Action cancelled."));
            }, ClickCallback.Options.builder().uses(1).build()))
            .build();

        // THE FIX: .empty() allows chaining .base() and .type()
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(title))
                .body(java.util.List.of(DialogBody.plainMessage(Component.text(message))))
                .build())
            .type(DialogType.confirmation(yesButton, noButton))
        );

        player.showDialog(dialog);
    }
}