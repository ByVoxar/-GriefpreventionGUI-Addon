# GriefPreventionGUI

**GriefPreventionGUI** is an advanced management plugin that transforms the text-based, complex world of the classic **GriefPrevention** plugin into a modern, visual, and user-friendly interface (GUI). Your players no longer need to memorize tedious commands; they can manage all claim operations with a single click.

---

##  Key Features

*    **Seamless Integration:** Hooks directly into the original GriefPrevention data store, ensuring zero lag, excellent performance, and complete data safety.
*    **Advanced Claim List (`/claimlerim`):** Displays all claims owned by the player. 
    *   **Left-click:** Opens the specific claim control panel.
    *   **Right-click:** Teleports the player directly to the claim's center.
*    **Claim Market (`/claimpazarı`):** An automated real estate system. Players can list their claims for in-game currency, and buyers can instantly transfer deeds securely with a single click.
*    **Dynamic Flag Management:** Toggle over 10+ crucial settings like PvP, monster/animal spawning, container opening, and block interactions instantly via visual icons.
*    **Visual Member Control:** Lists trusted players using their real Minecraft skin heads. Add members via an anvil prompt or revoke trust instantly by clicking their head (`/untrust`).
*    **Economy-Based Expiration:** Link claim lifetimes to the server economy to keep the world clean, optimized, and active.

---

##  Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/claim` | Opens the main menu for the claim you are standing on. | `griefpreventiongui.use` |
| `/claimlerim` | Opens the advanced claim list GUI. | `griefpreventiongui.list` |
| `/claimpazarı` | Opens the automated claim market GUI. | `griefpreventiongui.market` |

---

##  Requirements

*   **Java 21** or higher.
*   **Paper** / **Purpur** (1.21.x recommended).
*   **GriefPrevention** (Latest version).
*   An economy provider (e.g., **Vault** + EssentialsX).

---

##  Installation

1. Download the latest release `.jar` file.
2. Drop the file into your server's `plugins/` directory.
3. Ensure **GriefPrevention** and your economy plugin are running.
4. Restart your server or use a plugin manager to load it.
5. Configure the messages and pricing scales in the generated `config.yml`.
