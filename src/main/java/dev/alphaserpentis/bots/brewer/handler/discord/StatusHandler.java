package dev.alphaserpentis.bots.brewer.handler.discord;

import dev.alphaserpentis.coffeecore.core.CoffeeCore;
import dev.alphaserpentis.coffeecore.helper.ContainerHelper;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.entities.Activity;

public class StatusHandler {
    public static void init(@NonNull CoffeeCore core) {
         final var helper = new ContainerHelper(core.getActiveContainer());

         helper.setActivity(Activity.playing("with your roles!"));
    }
}
