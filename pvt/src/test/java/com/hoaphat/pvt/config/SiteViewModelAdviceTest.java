package com.hoaphat.pvt.config;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SiteViewModelAdviceTest {

    @Test
    void addsConfiguredSiteNameToEveryViewModel() {
        SiteViewModelAdvice advice = new SiteViewModelAdvice("Phòng vật tư Ống thép Hòa Phát Bình Dương - Long An");
        ConcurrentModel model = new ConcurrentModel();

        advice.addSiteName(model);

        assertThat(model.getAttribute("siteName"))
                .isEqualTo("Phòng vật tư Ống thép Hòa Phát Bình Dương - Long An");
    }

    @Test
    void blankConfigurationIsRejected() {
        assertThatThrownBy(() -> new SiteViewModelAdvice("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.site-name");
    }

    @Test
    void deploymentPropertiesPreserveVietnameseSiteNames() throws IOException {
        assertSiteName("deployment-configs/da-nang/application.properties",
                "Phòng vật tư Ống thép Hòa Phát Đà Nẵng");
        assertSiteName("deployment-configs/othp/application.properties",
                "Phòng vật tư Ống thép Hòa Phát");
        assertSiteName("deployment-configs/bd-la/application.properties",
                "Phòng vật tư Ống thép Hòa Phát Bình Dương - Long An");
    }

    private void assertSiteName(String path, String expected) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get(path))) {
            properties.load(input);
        }
        assertThat(properties.getProperty("app.site-name")).isEqualTo(expected);
    }
}
