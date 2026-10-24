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
        // 1. Create the "Confirm" button.
        ActionButton yesButton = ActionButton.builder(Component.text("✔ Confirm", TextColor.color(0x00FFAA)))
                .tooltip(Component.text("Click to execute this action"))
                .action(DialogAction.customClick((response, audience) -> {
                    onConfirm.run();
                }, ClickCallback.Options.builder().uses(1).build()))
                .build();

        // 2. Create the "Cancel" button.
        ActionButton noButton = ActionButton.builder(Component.text("✘ Cancel", NamedTextColor.RED))
                .tooltip(Component.text("Click to cancel"))
                .action(DialogAction.customClick((response, audience) -> {
                    player.sendMessage(Component.text("§7[IconsLoader] Action cancelled."));
                }, ClickCallback.Options.builder().uses(1).build()))
                .build();

        // 3. Build the Dialog using the correct API.
        // The .empty() call is required to get the builder that supports .base() and .type().
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title))
                        .body(java.util.List.of(DialogBody.plainMessage(Component.text(message))))
                        .build())
                .type(DialogType.confirmation(yesButton, noButton))
        );

        // 4. Show it to the player.
        player.showDialog(dialog);
    }
}