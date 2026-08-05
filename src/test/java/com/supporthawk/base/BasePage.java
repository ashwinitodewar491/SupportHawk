package com.supporthawk.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.supporthawk.config.AppConfig;
import com.supporthawk.utils.ScreenshotUtil;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/** Every UI test class extends this. Do not modify for individual test needs — page objects and
 * test classes should only ever use the {@code page} field this sets up. */
public class BasePage {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeMethod(alwaysRun = true)
    public void setUpBrowser() {
        playwright = Playwright.create();
        boolean headless = AppConfig.isHeadless();
        double slowMo = headless ? 0 : 800;

        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setSlowMo(slowMo));
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setRecordVideoDir(Paths.get("target/videos")));
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
    }
}
