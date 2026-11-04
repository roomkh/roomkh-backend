package tech.techey.property.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tech.techey.property.common.exception.ApiException;

class OwnerContactNormalizationTest {

    @Test
    void blankValuesClearTheChannel() {
        assertThat(OwnerContactController.phone("  ")).isNull();
        assertThat(OwnerContactController.telegram("")).isNull();
        assertThat(OwnerContactController.facebook(null)).isNull();
    }

    @Test
    void telegramAcceptsUsernamesLinksAndPhones() {
        assertThat(OwnerContactController.telegram("@roomkh_owner")).isEqualTo("roomkh_owner");
        assertThat(OwnerContactController.telegram("https://t.me/roomkh_owner")).isEqualTo("roomkh_owner");
        assertThat(OwnerContactController.telegram("t.me/roomkh_owner")).isEqualTo("roomkh_owner");
        assertThat(OwnerContactController.telegram("012 345 678")).isEqualTo("+85512345678");
        assertThat(OwnerContactController.telegram("+855 12 345 678")).isEqualTo("+85512345678");
    }

    @Test
    void telegramRejectsAnythingElse() {
        for (String bad : new String[]{"ab", "https://evil.example/x", "javascript:alert(1)", "@bad name"})
            assertThatThrownBy(() -> OwnerContactController.telegram(bad)).isInstanceOf(ApiException.class);
    }

    @Test
    void facebookAcceptsPageNamesAndFacebookUrls() {
        assertThat(OwnerContactController.facebook("sophea.property")).isEqualTo("https://www.facebook.com/sophea.property");
        assertThat(OwnerContactController.facebook("facebook.com/sophea.property")).isEqualTo("https://facebook.com/sophea.property");
        assertThat(OwnerContactController.facebook("http://m.facebook.com/profile.php?id=123"))
            .isEqualTo("https://m.facebook.com/profile.php?id=123");
    }

    @Test
    void facebookRejectsOtherHostsAndEmptyPaths() {
        for (String bad : new String[]{"https://facebook.com.evil.example/x", "https://evil.example/facebook", "facebook.com",
                "javascript:alert(1)", "https://facebook.com/x#frag"})
            assertThatThrownBy(() -> OwnerContactController.facebook(bad)).isInstanceOf(ApiException.class);
    }

    @Test
    void phoneMustLookLikeAPhoneNumber() {
        assertThat(OwnerContactController.phone(" 012 345 678 ")).isEqualTo("012 345 678");
        assertThatThrownBy(() -> OwnerContactController.phone("call me")).isInstanceOf(ApiException.class);
    }
}
