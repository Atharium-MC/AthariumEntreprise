package fr.itmozlegends;

import fr.itmozlegends.Command.EntrepriseCommand;
import fr.itmozlegends.Manager.EntrepriseManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class AthariumEntreprise extends JavaPlugin {

    private EntrepriseManager entrepriseManager;
    public static Economy economy;

    @Override
    public void onEnable() {
        getLogger().info("AthariumEntreprise est allumé !");

        if (!setupEconomy()) {
            getLogger().severe("Vault est requis pour ce plugin !");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        entrepriseManager = new EntrepriseManager(getDataFolder());
        getCommand("entreprise").setExecutor(new EntrepriseCommand(entrepriseManager));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;

        economy = rsp.getProvider();
        return economy != null;
    }

    @Override
    public void onDisable() {
        getLogger().info("AthariumEntreprise est éteint !");
    }
}
