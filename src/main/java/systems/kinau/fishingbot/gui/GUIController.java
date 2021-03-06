package systems.kinau.fishingbot.gui;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.Getter;
import systems.kinau.fishingbot.FishingBot;
import systems.kinau.fishingbot.event.EventHandler;
import systems.kinau.fishingbot.event.Listener;
import systems.kinau.fishingbot.event.custom.FishCaughtEvent;
import systems.kinau.fishingbot.gui.config.ConfigGUI;
import systems.kinau.fishingbot.modules.command.CommandExecutor;
import systems.kinau.fishingbot.network.protocol.play.PacketOutChat;

import javax.annotation.Resources;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class GUIController implements Listener {

    @FXML private TableView<LootItem> lootTable;
    @FXML private TableView<Enchantment> booksTable;
    @FXML private TableView<Enchantment> bowsTable;
    @FXML private TableView<Enchantment> rodsTable;
    @FXML private TableColumn lootItemColumn;
    @FXML private TableColumn lootCountColumn;
    @FXML private TextField commandlineTextField;
    @FXML private Tab lootTab;
    @FXML private Button startStopButton;
    @FXML private Button configButton;
    @FXML private Button playPauseButton;
    @FXML private ImageView skinPreview;
    @FXML private Label usernamePreview;

    @Getter private LootHistory lootHistory;

    public GUIController() {
        this.lootHistory = new LootHistory();
    }

    @FXML
    protected void initialize(URL location, Resources resources) {
        lootItemColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        lootCountColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        setupEnchantmentTable(booksTable);
        setupEnchantmentTable(bowsTable);
        setupEnchantmentTable(rodsTable);
    }

    public void exit(Event e) {
        Platform.exit();
    }

    public void deleteAllData(Event e) {
        this.lootHistory = new LootHistory();
        lootTable.getItems().clear();
        booksTable.getItems().clear();
        bowsTable.getItems().clear();
        rodsTable.getItems().clear();
        this.lootTab.setText(FishingBot.getI18n().t("ui-tabs-loot", lootHistory.getItems().stream().mapToInt(LootItem::getCount).sum()));
    }

    public void github(Event e) {
        openWebpage("https://github.com/MrKinau/FishingBot");
    }

    public void issues(Event e) {
        openWebpage("https://github.com/MrKinau/FishingBot/issues");
    }

    public void discord(Event e) {
        openWebpage("https://discord.gg/xHpCDYf");
    }

    public void openConfig(Event e) {
        String file;
        if (FishingBot.getInstance().getCurrentBot() == null)
            file = new File("config.json").getPath();
        else
            file = FishingBot.getInstance().getCurrentBot().getConfig().getPath();
        openFile(file);
    }

    public void openLogsDir(Event e) {
        String file;
        if (FishingBot.getInstance().getCurrentBot() == null)
            file = new File("logs/").getPath();
        else
            file = FishingBot.getInstance().getCurrentBot().getLogsFolder().getPath();
        openFile(file);
    }

    public void openLog(Event e) {
        String file;
        if (FishingBot.getInstance().getCurrentBot() == null)
            file = new File("logs/log0.log").getPath();
        else
            file = FishingBot.getInstance().getCurrentBot().getLogsFolder().getPath() + "/log0.log";
        openFile(file);
    }

    public void commandlineSend(Event e) {
        runCommand(commandlineTextField.getText());
        commandlineTextField.setText("");
    }

    private void runCommand(String text) {
        if (FishingBot.getInstance().getCurrentBot() == null || FishingBot.getInstance().getCurrentBot().getNet() == null)
            return;
        if (text.startsWith("/")) {
            boolean executed = FishingBot.getInstance().getCurrentBot().getCommandRegistry().dispatchCommand(text, CommandExecutor.CONSOLE);
            if (executed)
                return;
        }
        FishingBot.getInstance().getCurrentBot().getNet().sendPacket(new PacketOutChat(text));
    }

    private void openFile(String fileUrl) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(new File(fileUrl));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void openWebpage(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void playPause(Event e) {
        if (FishingBot.getInstance().getCurrentBot() == null)
            return;
        boolean paused = FishingBot.getInstance().getCurrentBot().getFishingModule().isPaused();
        FishingBot.getInstance().getCurrentBot().getFishingModule().setPaused(!paused);
        updatePlayPaused();
    }

    public void updatePlayPaused() {
        boolean paused = false;
        if (FishingBot.getInstance().getCurrentBot() != null)
            paused = FishingBot.getInstance().getCurrentBot().getFishingModule().isPaused();
        boolean finalPaused = paused;
        Platform.runLater(() -> {
            if (finalPaused)
                playPauseButton.setText(FishingBot.getI18n().t("ui-button-play"));
            else
                playPauseButton.setText(FishingBot.getI18n().t("ui-button-pause"));
        });
    }

    public void startStop(Event e) {
        if (FishingBot.getInstance().getCurrentBot() == null) {
            startStopButton.setText(FishingBot.getI18n().t("ui-button-stop"));
            playPauseButton.setDisable(false);
            new Thread(() -> FishingBot.getInstance().startBot()).start();
        } else {
            startStopButton.setDisable(true);
            playPauseButton.setDisable(true);
            startStopButton.setText(FishingBot.getI18n().t("ui-button-start"));
            FishingBot.getInstance().stopBot(true);
        }
    }

    public void updateStartStop() {
        Platform.runLater(() -> {
            if (FishingBot.getInstance().getCurrentBot() == null) {
                startStopButton.setText(FishingBot.getI18n().t("ui-button-start"));
                playPauseButton.setDisable(true);
            } else {
                startStopButton.setDisable(true);
                playPauseButton.setDisable(false);
                startStopButton.setText(FishingBot.getI18n().t("ui-button-stop"));

            }
        });
    }

    public void enableStartStop() {
        Platform.runLater(() -> {
            startStopButton.setDisable(false);
        });
    }

    public void openConfigGUI(Event e) {
        Stage primaryStage = (Stage) configButton.getScene().getWindow();
        new ConfigGUI(primaryStage);
    }

    public void openAbout(Event e) {
        Dialogs.showAboutWindow((Stage) configButton.getScene().getWindow(), s -> openWebpage(s));
    }

    public void setImage(String uuid) {
        if (uuid == null || uuid.isEmpty())
            uuid = UUID.randomUUID().toString().replace("-","").toLowerCase();
        String finalUuid = uuid;
        Platform.runLater(() -> {
            skinPreview.setFitWidth(120);
            skinPreview.setFitHeight(170);
            skinPreview.setVisible(true);
            skinPreview.setImage(new Image(String.format("https://crafatar.com/renders/body/%s?overlay", finalUuid)));
        });
    }

    public void setAccountName(String name) {
        Platform.runLater(() -> usernamePreview.setText(name));
    }

    @EventHandler
    public void onFishCaught(FishCaughtEvent event) {
        Platform.runLater(() -> {
            lootItemColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            lootCountColumn.setCellValueFactory(new PropertyValueFactory<>("count"));

            LootItem lootItem = lootHistory.registerItem(event.getItem().getName(), event.getItem().getEnchantments());
            AtomicBoolean existing = new AtomicBoolean(false);

            if (lootTable == null)
                return;


            lootTable.getItems().forEach(item -> {
                if (item.getName().equalsIgnoreCase(lootItem.getName())) {
                    item.setCount(lootItem.getCount());
                    existing.set(true);

                    lootCountColumn.setVisible(false);
                    lootCountColumn.setVisible(true);
                }
            });


            if (!existing.get())
                lootTable.getItems().add(lootItem);

            this.lootTab.setText(FishingBot.getI18n().t("ui-tabs-loot", lootHistory.getItems().stream().mapToInt(LootItem::getCount).sum()));


            if (event.getItem().getEnchantments().isEmpty())
                return;

            setupEnchantmentTable(booksTable);
            setupEnchantmentTable(bowsTable);
            setupEnchantmentTable(rodsTable);

            switch (event.getItem().getName().toLowerCase()) {
                case "enchanted_book": {
                    updateEnchantments(booksTable, event.getItem().getEnchantments());
                    break;
                }
                case "bow": {
                    updateEnchantments(bowsTable, event.getItem().getEnchantments());
                    break;
                }
                case "fishing_rod": {
                    updateEnchantments(rodsTable, event.getItem().getEnchantments());
                    break;
                }
            }
        });
    }

    private void setupEnchantmentTable(TableView<Enchantment> table) {
        table.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("name"));
        table.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("level"));
        table.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("count"));
    }

    private void updateEnchantments(TableView<Enchantment> table, List<systems.kinau.fishingbot.bot.Enchantment> enchantments) {
        enchantments.forEach(enchantment -> {
            AtomicBoolean exists = new AtomicBoolean(false);
            table.getItems().forEach(item -> {
                if (item.getName().equalsIgnoreCase(enchantment.getEnchantmentType().getName()) && item.getLevel() == enchantment.getLevel()) {
                    item.setCount(item.getCount() + 1);
                    exists.set(true);
                    table.getColumns().get(2).setVisible(false);
                    table.getColumns().get(2).setVisible(true);
                }
            });
            if (!exists.get())
                table.getItems().add(new Enchantment(enchantment.getEnchantmentType().getName(), enchantment.getLevel(), 1));
        });
    }
}
