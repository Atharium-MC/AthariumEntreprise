package fr.itmozlegends.Manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EntrepriseManager {

    private final File file;
    private final FileConfiguration config;

    // Map pour gérer les invitations temporaires : joueur UUID -> invitation
    private final Map<UUID, Invitation> invitations = new HashMap<>();

    public EntrepriseManager(File dataFolder) {
        this.file = new File(dataFolder, "entreprises.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean createEntreprise(String nom, UUID owner) {
        if (config.contains(nom)) return false;

        config.set(nom + ".owner", owner.toString());
        config.set(nom + ".members", Collections.singletonList(owner.toString()));
        config.set(nom + ".solde", 0.0);
        save();
        return true;
    }

    public boolean isInEntreprise(UUID uuid) {
        for (String key : config.getKeys(false)) {
            List<String> members = config.getStringList(key + ".members");
            if (members.contains(uuid.toString())) return true;
        }
        return false;
    }

    public String getEntreprise(UUID uuid) {
        for (String key : config.getKeys(false)) {
            List<String> members = config.getStringList(key + ".members");
            if (members.contains(uuid.toString())) return key;
        }
        return null;
    }

    public boolean isOwner(String entreprise, UUID uuid) {
        return config.getString(entreprise + ".owner").equals(uuid.toString());
    }

    public UUID getOwner(String entreprise) {
        String uuidStr = config.getString(entreprise + ".owner");
        if (uuidStr == null) return null;
        return UUID.fromString(uuidStr);
    }

    public List<UUID> getMembers(String entreprise) {
        List<String> list = config.getStringList(entreprise + ".members");
        List<UUID> uuids = new ArrayList<>();
        for (String s : list) {
            uuids.add(UUID.fromString(s));
        }
        return uuids;
    }

    public void invite(String entreprise, UUID uuid) {
        List<String> members = config.getStringList(entreprise + ".members");
        members.add(uuid.toString());
        config.set(entreprise + ".members", members);
        save();
    }

    public void leave(UUID uuid) {
        String ent = getEntreprise(uuid);
        if (ent == null) return;
        List<String> members = config.getStringList(ent + ".members");
        members.remove(uuid.toString());
        config.set(ent + ".members", members);

        if (members.isEmpty()) {
            config.set(ent, null); // Supprime l'entreprise si vide
        }
        save();
    }

    public void disband(String entreprise) {
        config.set(entreprise, null);
        save();
    }

    public void setDescription(String entreprise, String description) {
        config.set(entreprise + ".description", description);
        save();
    }

    public String getDescription(String entreprise) {
        return config.getString(entreprise + ".description", "Aucune description");
    }

    public void setGrade(String entreprise, UUID member, String grade) {
        config.set(entreprise + ".grades." + member.toString(), grade);
        save();
    }

    public String getGrade(String entreprise, UUID member) {
        return config.getString(entreprise + ".grades." + member.toString());
    }

    public void setOwner(String entreprise, UUID newOwner) {
        config.set(entreprise + ".owner", newOwner.toString());
        save();
    }

    // ---- Gestion du solde bancaire ----

    public double getSolde(String entreprise) {
        return config.getDouble(entreprise + ".solde", 0.0);
    }

    public void setSolde(String entreprise, double solde) {
        config.set(entreprise + ".solde", solde);
        save();
    }

    public void addSolde(String entreprise, double montant) {
        double actuel = getSolde(entreprise);
        setSolde(entreprise, actuel + montant);
    }

    public boolean removeSolde(String entreprise, double montant) {
        double actuel = getSolde(entreprise);
        if (actuel < montant) return false;
        setSolde(entreprise, actuel - montant);
        return true;
    }

    // ---- Sauvegarde ----

    private void save() {
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    // ---- Gestion des invitations ----

    public static class Invitation {
        private final String entreprise;
        private final long expirationTime; // timestamp en ms

        public Invitation(String entreprise, long durationMillis) {
            this.entreprise = entreprise;
            this.expirationTime = System.currentTimeMillis() + durationMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

        public String getEntreprise() {
            return entreprise;
        }
    }

    public void invitePlayer(String entreprise, UUID invitedPlayer, long durationMillis) {
        invitations.put(invitedPlayer, new Invitation(entreprise, durationMillis));
    }

    public boolean hasInvitation(UUID player) {
        Invitation inv = invitations.get(player);
        if (inv == null) return false;
        if (inv.isExpired()) {
            invitations.remove(player);
            return false;
        }
        return true;
    }

    public String getInvitationEntreprise(UUID player) {
        Invitation inv = invitations.get(player);
        if (inv == null || inv.isExpired()) {
            invitations.remove(player);
            return null;
        }
        return inv.getEntreprise();
    }

    public void acceptInvitation(UUID player) {
        if (!hasInvitation(player)) return;
        String entreprise = getInvitationEntreprise(player);
        if (entreprise == null) return;

        invite(entreprise, player);
        invitations.remove(player);
    }

    public void cleanupInvitations() {
        invitations.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
