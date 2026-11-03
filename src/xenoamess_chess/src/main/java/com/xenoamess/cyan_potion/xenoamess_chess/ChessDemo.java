package com.xenoamess.cyan_potion.xenoamess_chess;

import com.xenoamess.cyan_potion.base.GameManager;
import com.xenoamess.cyan_potion.base.GameWindow;
import com.xenoamess.cyan_potion.mcp.AutomationBridge;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Entry point for the native demo and explicitly marked headless integration mode. */
public final class ChessDemo {
    public static void main(String[] args) throws Exception {
        Path sessionFile=Path.of("src/xenoamess_chess/target/automation-session.json");
        for(int i=0;i<args.length;i++) if(args[i].equals("--session")&&i+1<args.length) sessionFile=Path.of(args[++i]);
        if(Arrays.asList(args).contains("--headless")) {
            ChessSession session=new ChessSession(Path.of("src/xenoamess_chess/target/saves"));
            AutomationBridge bridge=new AutomationBridge(session,sessionFile,0);
            var owner=Executors.newSingleThreadScheduledExecutor();owner.scheduleAtFixedRate(bridge::drain,0,10,TimeUnit.MILLISECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { bridge.close();owner.shutdownNow(); }));
            System.out.println("Chess headless automation ready: "+sessionFile.toAbsolutePath());new CountDownLatch(1).await();
        } else {
            System.setProperty("chess.automation.session",sessionFile.toString());
            try { new GameManager(Map.of("SettingFilePath","settings/ChessSettings.x8l")).startup(); }
            catch(Throwable failure) { failure.printStackTrace(System.err);System.exit(1); }
        }
    }
}
