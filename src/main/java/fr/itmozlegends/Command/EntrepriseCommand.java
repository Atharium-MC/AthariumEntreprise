package fr.itmozlegends.Command;

import fr.itmozlegends.Manager.EntrepriseManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EntrepriseCommand implements CommandExecutor {

    private final EntrepriseManager manager;
    private Economy economy;

    public EntrepriseCommand(EntrepriseManager manager) {
        this.manager = manager;
    }

    public EntrepriseCommand(EntrepriseManager manager, Economy economy) {
        this.manager = manager;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /entreprise create <nom>");
                    return true;
                }
                if (manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu es déjà dans une entreprise.");
                    return true;
                }
                if (manager.createEntreprise(args[1], player.getUniqueId())) {
                    player.sendMessage("§aEntreprise créée !");
                } else {
                    player.sendMessage("§cCe nom est déjà pris.");
                }
                break;

            case "info":
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                String entInfo = manager.getEntreprise(player.getUniqueId());
                player.sendMessage("§6Entreprise: §e" + entInfo);
                player.sendMessage("§6Description: §f" + manager.getDescription(entInfo));
                player.sendMessage("§6Membres:");
                for (UUID uuid : manager.getMembers(entInfo)) {
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    String grade = manager.getGrade(entInfo, uuid);
                    if (uuid.equals(manager.getOwner(entInfo))) {
                        player.sendMessage("  §a[PDG] " + name);
                    } else {
                        player.sendMessage("  §7" + name + (grade != null ? " §8- §7" + grade : ""));
                    }
                }
                break;

            case "balance":
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                String entBalance = manager.getEntreprise(player.getUniqueId());
                double solde = economy.getBalance(Bukkit.getOfflinePlayer(entBalance));
                player.sendMessage("§6Solde de l'entreprise §e" + entBalance + "§6: §a" + solde + "$");
                break;

            case "withdraw":
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage : /entreprise withdraw <montant>");
                    return true;
                }
                if (!manager.isOwner(manager.getEntreprise(player.getUniqueId()), player.getUniqueId())) {
                    player.sendMessage("§cSeul le PDG peut retirer de l'argent.");
                    return true;
                }
                double amountWithdraw;
                try {
                    amountWithdraw = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cMontant invalide.");
                    return true;
                }
                if (amountWithdraw <= 0) {
                    player.sendMessage("§cLe montant doit être positif.");
                    return true;
                }
                String entrepriseWithdraw = manager.getEntreprise(player.getUniqueId());
                if (!manager.removeSolde(entrepriseWithdraw, amountWithdraw)) {
                    player.sendMessage("§cL'entreprise n'a pas assez d'argent.");
                    return true;
                }
                economy.depositPlayer(player, amountWithdraw);
                player.sendMessage("§aTu as retiré " + amountWithdraw + "$ de l'entreprise.");
                break;

            case "deposit":
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage : /entreprise deposit <montant>");
                    return true;
                }
                double amountDeposit;
                try {
                    amountDeposit = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cMontant invalide.");
                    return true;
                }
                if (amountDeposit <= 0) {
                    player.sendMessage("§cLe montant doit être positif.");
                    return true;
                }
                if (economy.getBalance(player) < amountDeposit) {
                    player.sendMessage("§cTu n'as pas assez d'argent.");
                    return true;
                }
                String entrepriseDeposit = manager.getEntreprise(player.getUniqueId());
                economy.withdrawPlayer(player, amountDeposit);
                manager.addSolde(entrepriseDeposit, amountDeposit);
                player.sendMessage("§aTu as déposé " + amountDeposit + "$ dans l'entreprise.");
                break;

            case "invite":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /entreprise invite <joueur>");
                    return true;
                }
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                String entInvite = manager.getEntreprise(player.getUniqueId());
                if (!manager.isOwner(entInvite, player.getUniqueId())) {
                    player.sendMessage("§cSeul le PDG peut inviter des joueurs.");
                    return true;
                }
                Player targetInvite = Bukkit.getPlayer(args[1]);
                if (targetInvite == null) {
                    player.sendMessage("§cJoueur introuvable.");
                    return true;
                }
                if (manager.isInEntreprise(targetInvite.getUniqueId())) {
                    player.sendMessage("§cCe joueur est déjà dans une entreprise.");
                    return true;
                }
                manager.invitePlayer(entInvite, targetInvite.getUniqueId(), 300000); // 5 minutes
                player.sendMessage("§aInvitation envoyée à " + targetInvite.getName());
                targetInvite.sendMessage("§eTu as reçu une invitation pour rejoindre l'entreprise §6" + entInvite + "§e.");
                targetInvite.sendMessage("§eTape §a/entreprise accept §epour rejoindre.");
                break;

            case "accept":
                if (!manager.hasInvitation(player.getUniqueId())) {
                    player.sendMessage("§cTu n'as aucune invitation en attente.");
                    return true;
                }
                if (manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu es déjà dans une entreprise.");
                    return true;
                }
                manager.acceptInvitation(player.getUniqueId());
                String entAccepted = manager.getInvitationEntreprise(player.getUniqueId());
                player.sendMessage("§aTu as rejoint l'entreprise " + entAccepted + " !");
                break;

            case "leave":
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                String entLeave = manager.getEntreprise(player.getUniqueId());
                if (manager.isOwner(entLeave, player.getUniqueId())) {
                    player.sendMessage("§cLe PDG ne peut pas quitter l'entreprise, transfère d'abord la propriété ou disband.");
                    return true;
                }
                manager.leave(player.getUniqueId());
                player.sendMessage("§aTu as quitté l'entreprise " + entLeave + ".");
                break;

            case "disband":
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                String entDisband = manager.getEntreprise(player.getUniqueId());
                if (!manager.isOwner(entDisband, player.getUniqueId())) {
                    player.sendMessage("§cSeul le PDG peut dissoudre l'entreprise.");
                    return true;
                }
                manager.disband(entDisband);
                player.sendMessage("§aEntreprise dissoute.");
                break;

            case "description":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /entreprise description <texte>");
                    return true;
                }
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                String entDesc = manager.getEntreprise(player.getUniqueId());
                if (!manager.isOwner(entDesc, player.getUniqueId())) {
                    player.sendMessage("§cSeul le PDG peut modifier la description.");
                    return true;
                }
                String desc = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                manager.setDescription(entDesc, desc);
                player.sendMessage("§aDescription mise à jour !");
                break;

            case "grade":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /entreprise grade <joueur> <grade>");
                    return true;
                }
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                String entGrade = manager.getEntreprise(player.getUniqueId());
                if (!manager.isOwner(entGrade, player.getUniqueId())) {
                    player.sendMessage("§cSeul le PDG peut attribuer des grades.");
                    return true;
                }
                Player targetGrade = Bukkit.getPlayer(args[1]);
                if (targetGrade == null || !manager.isInEntreprise(targetGrade.getUniqueId()) || !manager.getEntreprise(targetGrade.getUniqueId()).equals(entGrade)) {
                    player.sendMessage("§cCe joueur n’est pas dans ton entreprise.");
                    return true;
                }
                String grade = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                manager.setGrade(entGrade, targetGrade.getUniqueId(), grade);
                player.sendMessage("§aGrade défini : " + targetGrade.getName() + " → " + grade);
                break;

            case "transfer":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /entreprise transfer <joueur>");
                    return true;
                }
                if (!manager.isInEntreprise(player.getUniqueId())) {
                    player.sendMessage("§cTu n'es dans aucune entreprise.");
                    return true;
                }
                String entTransfer = manager.getEntreprise(player.getUniqueId());
                if (!manager.isOwner(entTransfer, player.getUniqueId())) {
                    player.sendMessage("§cSeul le PDG peut transférer l'entreprise.");
                    return true;
                }
                Player newOwner = Bukkit.getPlayer(args[1]);
                if (newOwner == null || !manager.getEntreprise(newOwner.getUniqueId()).equals(entTransfer)) {
                    player.sendMessage("§cCe joueur n’est pas dans ton entreprise.");
                    return true;
                }
                manager.setOwner(entTransfer, newOwner.getUniqueId());
                player.sendMessage("§aTu as transféré la direction à " + newOwner.getName());
                newOwner.sendMessage("§aTu es maintenant le PDG de l’entreprise " + entTransfer);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage("§eCommandes disponibles:");
        p.sendMessage("§e/entreprise create <nom>");
        p.sendMessage("§e/entreprise info");
        p.sendMessage("§e/entreprise invite <joueur>");
        p.sendMessage("§e/entreprise accept");
        p.sendMessage("§e/entreprise leave");
        p.sendMessage("§e/entreprise disband");
        p.sendMessage("§e/entreprise description <texte>");
        p.sendMessage("§e/entreprise grade <joueur> <grade>");
        p.sendMessage("§e/entreprise transfer <joueur>");
        p.sendMessage("§e/entreprise balance");
    }
}
