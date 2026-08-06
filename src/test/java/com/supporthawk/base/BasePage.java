package com.supporthawk.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.supporthawk.config.AppConfig;
import com.supporthawk.data.QueryModel;
import com.supporthawk.utils.EdgeTTSUtil;
import com.supporthawk.utils.ScreenshotUtil;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Every UI test class extends this. Do not modify for individual test needs — page objects and
 * test classes should only ever use the {@code page} field this sets up. */
public class BasePage {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;
    private Path currentVoiceWavPath;

    @BeforeMethod(alwaysRun = true)
    public void setUpBrowser(Object[] params) {
        playwright = Playwright.create();
        boolean headless = AppConfig.isHeadless();
        double slowMo = headless ? 0 : 800;
        List<String> launchArgs = new ArrayList<>();
        launchArgs.add("--use-fake-ui-for-media-stream");

        String voiceQuery = extractVoiceQuery(params);
        if (voiceQuery != null && !voiceQuery.isBlank()) {
            currentVoiceWavPath = EdgeTTSUtil.generateWavFile(voiceQuery);
            launchArgs.add("--use-fake-device-for-media-stream");
            launchArgs.add("--use-file-for-fake-audio-capture=" + currentVoiceWavPath.toAbsolutePath());
            System.setProperty("current.voice.wav.path", currentVoiceWavPath.toAbsolutePath().toString());
        } else {
            currentVoiceWavPath = null;
            System.clearProperty("current.voice.wav.path");
        }

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(slowMo)
                        .setArgs(launchArgs));
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setRecordVideoDir(Paths.get("target/videos")));
        context.grantPermissions(
                List.of("microphone"),
                new BrowserContext.GrantPermissionsOptions().setOrigin(AppConfig.BASE_URL)
        );
        page = context.newPage();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        boolean failed = result.getStatus() == ITestResult.FAILURE;
        Path videoPath = null;

        if (page != null) {
            ScreenshotUtil.captureOnFailure(page, result);
            try {
                if (page.video() != null) {
                    videoPath = page.video().path();
                }
            } catch (Exception ignored) {
            }
            page.close();
        }

        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }

        if (videoPath != null) {
            try {
                if (failed) {
                    String name = result.getName() + "_" + System.currentTimeMillis() + ".webm";
                    Files.move(videoPath, videoPath.getParent().resolve(name),
                            StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.deleteIfExists(videoPath);
                }
            } catch (IOException ignored) {
            }
        }

        if (currentVoiceWavPath != null) {
            try {
                Files.deleteIfExists(currentVoiceWavPath);
            } catch (IOException ignored) {
            }
        }
        System.clearProperty("current.voice.wav.path");
    }

    private String extractVoiceQuery(Object[] params) {
        if (params == null) {
            return null;
        }

        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param instanceof QueryModel) {
                QueryModel queryModel = (QueryModel) param;
                return queryModel.getQuery();
            }
        }
        return null;
    }
}
